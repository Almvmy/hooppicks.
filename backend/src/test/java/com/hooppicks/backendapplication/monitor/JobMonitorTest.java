package com.hooppicks.backendapplication.monitor;

import com.hooppicks.backendapplication.entity.JobRun;
import com.hooppicks.backendapplication.repository.JobRunRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobMonitorTest {

    private static final Instant NOW = Instant.parse("2026-10-24T12:00:00Z");
    private static final JobMonitor.Job EVERY_5_MIN = new JobMonitor.Job("x", "X", Duration.ofMinutes(5));
    private static final JobMonitor.Job DAILY = new JobMonitor.Job("d", "D", Duration.ofDays(1));

    private static JobRun run(Instant success, Instant failure) {
        JobRun r = new JobRun();
        r.setLastSuccessAt(success);
        r.setLastFailureAt(failure);
        return r;
    }

    @Test
    void statut_selon_le_dernier_succes_et_le_dernier_echec() {
        assertThat(JobMonitor.status(null, EVERY_5_MIN, NOW)).isEqualTo(JobMonitor.Status.NEVER);
        assertThat(JobMonitor.status(run(NOW.minusSeconds(60), null), EVERY_5_MIN, NOW)).isEqualTo(JobMonitor.Status.OK);
        assertThat(JobMonitor.status(run(NOW.minus(Duration.ofMinutes(20)), null), EVERY_5_MIN, NOW)).isEqualTo(JobMonitor.Status.LATE);
        assertThat(JobMonitor.status(run(NOW.minusSeconds(600), NOW.minusSeconds(60)), EVERY_5_MIN, NOW)).isEqualTo(JobMonitor.Status.FAILING);
        assertThat(JobMonitor.status(run(NOW.minusSeconds(60), NOW.minusSeconds(600)), EVERY_5_MIN, NOW)).isEqualTo(JobMonitor.Status.OK);
        // Tâche quotidienne passée il y a 2 jours : encore dans la marge.
        assertThat(JobMonitor.status(run(NOW.minus(Duration.ofDays(2)), null), DAILY, NOW)).isEqualTo(JobMonitor.Status.OK);
    }

    @Test
    void enregistre_succes_et_echec_sans_jamais_faire_echouer_la_tache() {
        JobRunRepository repo = mock(JobRunRepository.class);
        when(repo.findById("moyennes-saison")).thenReturn(Optional.empty());
        JobMonitor monitor = new JobMonitor(repo, Clock.fixed(NOW, ZoneOffset.UTC));

        monitor.ok("moyennes-saison", "0 joueur(s) mis à jour");
        ArgumentCaptor<JobRun> saved = ArgumentCaptor.forClass(JobRun.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getLastSuccessAt()).isEqualTo(NOW);
        assertThat(saved.getValue().getLastDetail()).isEqualTo("0 joueur(s) mis à jour");

        when(repo.save(any())).thenThrow(new RuntimeException("base injoignable"));
        monitor.failed("moyennes-saison", new IllegalStateException("ESPN muet"));
    }

    @Test
    void sante_liste_toutes_les_taches_connues() {
        JobRunRepository repo = mock(JobRunRepository.class);
        JobRun stats = run(NOW.minusSeconds(60), null);
        stats.setName("stats-joueurs-espn");
        stats.setLastDetail("0 mis à jour sur 8");
        when(repo.findAll()).thenReturn(List.of(stats));

        List<JobMonitor.Health> health = new JobMonitor(repo, Clock.fixed(NOW, ZoneOffset.UTC)).health();

        assertThat(health).hasSize(JobMonitor.JOBS.size());
        assertThat(health).filteredOn(h -> h.key().equals("stats-joueurs-espn")).first()
                .satisfies(h -> {
                    assertThat(h.status()).isEqualTo(JobMonitor.Status.OK);
                    assertThat(h.lastDetail()).isEqualTo("0 mis à jour sur 8");
                });
        assertThat(health).filteredOn(h -> h.key().equals("resumes-video")).first()
                .extracting(JobMonitor.Health::status).isEqualTo(JobMonitor.Status.NEVER);
    }
}
