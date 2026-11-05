package com.hooppicks.backendapplication.engagement;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.nba.AltLines;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.DuelRepository;
import com.hooppicks.backendapplication.repository.MissionCompletionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

/**
 * Missions de la semaine : trois objectifs, les mêmes pour tout le monde (tirés
 * d'après la semaine), une action à faire et une à gagner au moins. Récompense
 * cosmétique uniquement (icônes de maillot, cf. AvatarService) : jamais de
 * points, chacun garde les mêmes 1000 pts à risquer. Elles donnent aussi une
 * raison d'essayer les marchés qu'on ne jouerait pas spontanément.
 *
 * Progression recalculée à l'affichage d'après les tickets de la semaine, rien
 * à synchroniser. Une réussite est notée en base à ce moment-là, pour la
 * semaine en cours et la précédente (un ticket réglé lundi matin compte encore).
 */
@Service
public class MissionService {

    /** Icône de maillot `rocket` débloquée à ce nombre de missions réussies. */
    public static final int ROCKET_AT = 5;
    /** Icône de maillot `star`. */
    public static final int STAR_AT = 15;

    static final int STAKE_TARGET = 500;
    // « 30+ pts » : seul le libellé distingue un palier d'une ligne de pari joueur.
    private static final Pattern MILESTONE_LABEL = Pattern.compile("\\d\\+ ");

    /** Ce que la progression d'une mission peut lire. */
    record WeekData(List<Bet> bets, List<Duel> duels) {
        List<Bet> won() {
            return bets.stream().filter(b -> b.getStatus() == BetStatus.WON).toList();
        }
    }

    record Mission(String key, String label, int target, ToIntFunction<WeekData> progress) {}

    private static final List<Mission> TO_PLAY = List.of(
            new Mission("nights", "Joue sur 3 soirées différentes", 3, MissionService::nights),
            new Mission("duel", "Lance ou accepte un duel", 1, d -> d.duels().isEmpty() ? 0 : 1),
            new Mission("alt", "Pose un ticket sur une ligne alternative ou un palier", 1,
                    d -> (int) d.bets().stream().filter(b -> b.getSelections().stream().anyMatch(MissionService::isAltOrMilestone)).count()),
            new Mission("stake", "Mise au moins " + STAKE_TARGET + " pts dans la semaine", STAKE_TARGET,
                    d -> d.bets().stream().filter(b -> b.getStatus() != BetStatus.VOID).mapToInt(Bet::getStake).sum()));

    private static final List<Mission> TO_WIN = List.of(
            new Mission("win_prop", "Gagne un ticket avec un pari joueur", 1, d -> wonWith(d, s -> s.getPlayerName() != null)),
            new Mission("win_underdog", "Gagne un ticket avec un outsider (vainqueur à 2,00 ou plus)", 1,
                    d -> wonWith(d, s -> "moneyline".equals(s.getMarket()) && s.getOdds() >= 2)),
            new Mission("win_parlay", "Gagne un combiné de 3 sélections ou plus", 1,
                    d -> (int) d.won().stream().filter(b -> b.getSelections().size() >= 3).count()),
            new Mission("win_total", "Gagne un ticket sur un total de points", 1,
                    d -> wonWith(d, s -> s.getMarket() != null && s.getMarket().endsWith("total"))));

    private static boolean isAltOrMilestone(BetSelection s) {
        return AltLines.ALT_SPREAD.equals(s.getMarket()) || AltLines.ALT_TOTAL.equals(s.getMarket())
                || (s.getLabel() != null && MILESTONE_LABEL.matcher(s.getLabel()).find());
    }

    private static int wonWith(WeekData d, java.util.function.Predicate<BetSelection> test) {
        return (int) d.won().stream().filter(b -> b.getSelections().stream().anyMatch(test)).count();
    }

    // Soirée NBA : de 12h à 12h GMT, comme la semaine de jeu.
    private static int nights(WeekData d) {
        return (int) d.bets().stream()
                .map(b -> b.getPlacedAt().minus(Duration.ofHours(12)).atZone(BankrollService.ZONE).toLocalDate())
                .distinct().count();
    }

    public record MissionView(String key, String label, int progress, int target, boolean done) {}

    /** `nextRewardAt` null : toutes les récompenses sont débloquées. */
    public record Missions(LocalDate week, List<MissionView> missions, long completed, Integer nextRewardAt, String nextRewardLabel) {}

    private final BetRepository betRepository;
    private final DuelRepository duelRepository;
    private final MissionCompletionRepository completionRepository;
    private final Clock clock;

    @Autowired
    public MissionService(BetRepository betRepository, DuelRepository duelRepository, MissionCompletionRepository completionRepository) {
        this(betRepository, duelRepository, completionRepository, Clock.system(BankrollService.ZONE));
    }

    MissionService(BetRepository betRepository, DuelRepository duelRepository, MissionCompletionRepository completionRepository, Clock clock) {
        this.betRepository = betRepository;
        this.duelRepository = duelRepository;
        this.completionRepository = completionRepository;
        this.clock = clock;
    }

    /** Une à faire, une à gagner, et une de plus piochée dans ce qui reste : stable sur toute la semaine. */
    static List<Mission> forWeek(LocalDate week) {
        Random random = new Random(week.toEpochDay());
        List<Mission> toPlay = new ArrayList<>(TO_PLAY);
        List<Mission> toWin = new ArrayList<>(TO_WIN);
        Collections.shuffle(toPlay, random);
        Collections.shuffle(toWin, random);
        List<Mission> rest = new ArrayList<>(toPlay.subList(1, toPlay.size()));
        rest.addAll(toWin.subList(1, toWin.size()));
        return List.of(toPlay.get(0), toWin.get(0), rest.get(random.nextInt(rest.size())));
    }

    @Transactional
    public Missions forUser(String userId) {
        LocalDate week = BankrollService.weekOf(clock.instant());
        List<Bet> allBets = betRepository.findByUserIdOrderByPlacedAtDesc(userId);
        List<Duel> allDuels = duelRepository.findAllForUser(userId);

        recordDone(userId, week.minusWeeks(1), weekData(userId, week.minusWeeks(1), allBets, allDuels));
        WeekData current = weekData(userId, week, allBets, allDuels);
        recordDone(userId, week, current);

        List<MissionView> views = forWeek(week).stream().map(m -> {
            int progress = m.progress().applyAsInt(current);
            return new MissionView(m.key(), m.label(), Math.min(progress, m.target()), m.target(), progress >= m.target());
        }).toList();

        long completed = completionRepository.countByUserId(userId);
        Integer nextAt = completed < ROCKET_AT ? Integer.valueOf(ROCKET_AT) : completed < STAR_AT ? Integer.valueOf(STAR_AT) : null;
        String nextLabel = nextAt == null ? null : nextAt == ROCKET_AT ? "Fusée" : "Étoile";
        return new Missions(week, views, completed, nextAt, nextLabel);
    }

    private WeekData weekData(String userId, LocalDate week, List<Bet> allBets, List<Duel> allDuels) {
        Instant start = BankrollService.startOf(week);
        Instant end = start.plus(Duration.ofDays(7));
        List<Bet> bets = allBets.stream().filter(b -> !b.getPlacedAt().isBefore(start) && b.getPlacedAt().isBefore(end)).toList();
        List<Duel> duels = allDuels.stream().filter(d -> week.equals(d.getWeek()))
                // Un défi reçu sans réponse ne compte pas : le joueur n'a encore rien fait.
                .filter(d -> d.getStatus() == DuelStatus.ACCEPTED || d.getStatus() == DuelStatus.FINISHED
                        || (d.getStatus() == DuelStatus.PENDING && userId.equals(d.getChallenger().getId())))
                .toList();
        return new WeekData(bets, duels);
    }

    private void recordDone(String userId, LocalDate week, WeekData data) {
        for (Mission m : forWeek(week)) {
            if (m.progress().applyAsInt(data) < m.target()) continue;
            String id = userId + ":" + week + ":" + m.key();
            if (completionRepository.existsById(id)) continue;
            MissionCompletion completion = new MissionCompletion();
            completion.setId(id);
            completion.setUserId(userId);
            completion.setCompletedAt(clock.instant());
            completionRepository.save(completion);
        }
    }
}
