package com.hooppicks.backendapplication.monitor;

import com.hooppicks.backendapplication.entity.JobRun;
import com.hooppicks.backendapplication.repository.JobRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Santé des tâches de fond, pour l'onglet « Santé » de la console admin.
 * Plusieurs tâches avaient échoué en production sans que personne ne le voie
 * (résumés vidéo, moyennes des joueurs restées à la saison passée) : chaque
 * tâche note ici son dernier succès (avec un court résultat) et son dernier
 * échec. Ne fait jamais échouer la tâche qui l'appelle.
 */
@Component
public class JobMonitor {

    private static final Logger log = LoggerFactory.getLogger(JobMonitor.class);

    /** Tâche suivie : libellé de la console et fréquence attendue (en retard au-delà de 3 fois). */
    public record Job(String key, String label, Duration every) {}

    public static final Map<String, Job> JOBS = new LinkedHashMap<>();

    private static void job(String key, String label, Duration every) {
        JOBS.put(key, new Job(key, label, every));
    }

    static {
        job("sync-matchs", "Matchs et règlement des paris", Duration.ofMinutes(5));
        job("espn-phases", "Phases et présaison (ESPN)", Duration.ofMinutes(5));
        job("espn-liaison", "Liaison des matchs à ESPN", Duration.ofMinutes(5));
        job("feuilles-de-match", "Feuilles de match (ESPN)", Duration.ofMinutes(5));
        job("moyennes-saison", "Moyennes de la saison", Duration.ofMinutes(5));
        job("stats-joueurs-espn", "Stats des joueurs (ESPN)", Duration.ofMinutes(5));
        job("resumes-video", "Résumés vidéo", Duration.ofMinutes(10));
        job("semaines", "Semaines, duels et palmarès", Duration.ofMinutes(1));
        job("matchs-suivis", "Notifications des matchs suivis", Duration.ofMinutes(1));
        job("equipe-favorite", "Notifications équipe favorite", Duration.ofMinutes(5));
        job("effectifs", "Effectifs (ESPN)", Duration.ofDays(1));
        job("classement-officiel", "Classement officiel (ESPN)", Duration.ofDays(1));
        job("photo-classement", "Photo du classement", Duration.ofDays(1));
    }

    public enum Status { OK, LATE, FAILING, NEVER }

    public record Health(String key, String label, Status status, Instant lastSuccessAt, Instant lastFailureAt,
                         String lastError, String lastDetail, long everyMinutes) {}

    private final JobRunRepository repository;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public JobMonitor(JobRunRepository repository) {
        this(repository, Clock.systemUTC());
    }

    JobMonitor(JobRunRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void ok(String key, String detail) {
        update(key, run -> {
            run.setLastSuccessAt(clock.instant());
            run.setLastDetail(cut(detail, 255));
        });
    }

    public void failed(String key, Exception e) {
        update(key, run -> {
            run.setLastFailureAt(clock.instant());
            run.setLastError(cut(e.getClass().getSimpleName() + (e.getMessage() == null ? "" : " : " + e.getMessage()), 500));
        });
    }

    private void update(String key, java.util.function.Consumer<JobRun> change) {
        try {
            JobRun run = repository.findById(key).orElseGet(() -> {
                JobRun r = new JobRun();
                r.setName(key);
                return r;
            });
            change.accept(run);
            repository.save(run);
        } catch (Exception e) {
            log.warn("Suivi de la tâche {} impossible à enregistrer", key, e);
        }
    }

    public List<Health> health() {
        Instant now = clock.instant();
        Map<String, JobRun> runs = new LinkedHashMap<>();
        repository.findAll().forEach(r -> runs.put(r.getName(), r));
        List<Health> out = new ArrayList<>();
        for (Job job : JOBS.values()) {
            JobRun r = runs.get(job.key());
            out.add(new Health(job.key(), job.label(), status(r, job, now),
                    r == null ? null : r.getLastSuccessAt(), r == null ? null : r.getLastFailureAt(),
                    r == null ? null : r.getLastError(), r == null ? null : r.getLastDetail(), job.every().toMinutes()));
        }
        return out;
    }

    static Status status(JobRun r, Job job, Instant now) {
        if (r == null || (r.getLastSuccessAt() == null && r.getLastFailureAt() == null)) return Status.NEVER;
        if (r.getLastFailureAt() != null && (r.getLastSuccessAt() == null || r.getLastFailureAt().isAfter(r.getLastSuccessAt()))) {
            return Status.FAILING;
        }
        // Une tâche quotidienne peut avoir tourné hier matin : marge d'un jour de plus.
        Duration tolerance = job.every().multipliedBy(3).plus(job.every().compareTo(Duration.ofHours(1)) >= 0 ? Duration.ofDays(1) : Duration.ZERO);
        return r.getLastSuccessAt().isBefore(now.minus(tolerance)) ? Status.LATE : Status.OK;
    }

    private static String cut(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
