package com.hooppicks.backendapplication.engagement;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.push.PushService;
import com.hooppicks.backendapplication.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Deux rappels par semaine, aux joueurs actifs (un ticket sur les 4 dernières
 * semaines : on ne relance pas indéfiniment celui qui est parti), selon la
 * préférence notifyWeeklyReminders :
 * - dimanche soir : le budget qu'il reste à miser avant lundi 12h GMT, et
 *   l'écart au podium de sa ligue quand il est à sa portée ;
 * - lundi après 12h GMT : la nouvelle semaine. Push seulement : le récap de
 *   la semaine est déjà dans la cloche (BankrollService).
 * Chacun une seule fois (`WeeklyReminder`), et jamais hors de sa fenêtre : un
 * serveur redémarré mardi n'envoie pas « nouvelle semaine » en retard.
 */
@Service
public class WeeklyReminderService {

    private static final Logger log = LoggerFactory.getLogger(WeeklyReminderService.class);
    static final Duration ACTIVE_WINDOW = Duration.ofDays(28);
    // En dessous, plus grand-chose à miser : le rappel ne servirait à rien.
    static final int MIN_BALANCE = 100;
    // Dimanche 18h GMT → lundi 6h GMT (la semaine commence le lundi à 12h).
    private static final Duration SUNDAY_FROM = Duration.ofDays(6).plusHours(6);
    private static final Duration SUNDAY_TO = Duration.ofDays(6).plusHours(18);
    private static final Duration MONDAY_TO = Duration.ofHours(24);

    private final WeeklyReminderRepository reminderRepository;
    private final BetRepository betRepository;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final LeagueRepository leagueRepository;
    private final LeagueMembershipRepository membershipRepository;
    private final NotificationRepository notificationRepository;
    private final PushService pushService;
    private final Clock clock;

    @Autowired
    public WeeklyReminderService(WeeklyReminderRepository reminderRepository, BetRepository betRepository,
                                 UserRepository userRepository, MatchRepository matchRepository,
                                 LeagueRepository leagueRepository, LeagueMembershipRepository membershipRepository,
                                 NotificationRepository notificationRepository, PushService pushService) {
        this(reminderRepository, betRepository, userRepository, matchRepository, leagueRepository, membershipRepository,
                notificationRepository, pushService, Clock.system(BankrollService.ZONE));
    }

    WeeklyReminderService(WeeklyReminderRepository reminderRepository, BetRepository betRepository,
                          UserRepository userRepository, MatchRepository matchRepository,
                          LeagueRepository leagueRepository, LeagueMembershipRepository membershipRepository,
                          NotificationRepository notificationRepository, PushService pushService, Clock clock) {
        this.reminderRepository = reminderRepository;
        this.betRepository = betRepository;
        this.userRepository = userRepository;
        this.matchRepository = matchRepository;
        this.leagueRepository = leagueRepository;
        this.membershipRepository = membershipRepository;
        this.notificationRepository = notificationRepository;
        this.pushService = pushService;
        this.clock = clock;
    }

    /** Envoie le rappel dont c'est le moment, s'il n'est pas déjà parti. Renvoie le nombre de joueurs prévenus. */
    @Transactional
    public int run() {
        Instant now = clock.instant();
        LocalDate week = BankrollService.weekOf(now);
        Duration sinceStart = Duration.between(BankrollService.startOf(week), now);

        if (sinceStart.compareTo(MONDAY_TO) < 0) return once(week + ":MONDAY", () -> sendMonday(now));
        if (sinceStart.compareTo(SUNDAY_FROM) >= 0 && sinceStart.compareTo(SUNDAY_TO) < 0) {
            return once(week + ":SUNDAY", () -> sendSunday(now, week));
        }
        return 0;
    }

    private int once(String id, java.util.function.IntSupplier send) {
        if (reminderRepository.existsById(id)) return 0;
        WeeklyReminder marker = new WeeklyReminder();
        marker.setId(id);
        marker.setSentAt(clock.instant());
        reminderRepository.save(marker);
        int sent = send.getAsInt();
        log.info("Rappel {} : {} joueur(s)", id, sent);
        return sent;
    }

    private List<User> activePlayers(Instant now) {
        return userRepository.findAllById(betRepository.findUserIdsWithBetSince(now.minus(ACTIVE_WINDOW))).stream()
                .filter(User::isNotifyWeeklyReminders)
                .toList();
    }

    private int sendMonday(Instant now) {
        int sent = 0;
        for (User user : activePlayers(now)) {
            pushService.sendToUser(user.getId(), new PushService.PushMessage("Nouvelle semaine",
                    BankrollService.WEEKLY_BANKROLL + " pts t'attendent. Tout le monde repart à égalité.", "/matches"));
            sent++;
        }
        return sent;
    }

    private int sendSunday(Instant now, LocalDate week) {
        Instant weekEnd = BankrollService.startOf(week).plus(Duration.ofDays(7));
        // Plus aucun match avant lundi 12h : « il te reste à miser » n'aurait pas de sens.
        if (matchRepository.findByStatusAndDateBetween(MatchStatus.SCHEDULED, now, weekEnd).isEmpty()) return 0;

        Map<String, Long> scores = new HashMap<>();
        for (Object[] row : betRepository.getAllWeekScores(week.atStartOfDay())) {
            scores.put((String) row[0], ((Number) row[1]).longValue());
        }
        Map<String, PodiumGap> gaps = podiumGaps(scores);

        int sent = 0;
        for (User user : activePlayers(now)) {
            // Solde pas encore passé à cette semaine : le joueur n'a rien misé depuis lundi.
            int balance = week.equals(user.getBankrollWeek()) ? user.getWalletBalance() : BankrollService.WEEKLY_BANKROLL;
            if (balance < MIN_BALANCE) continue;

            StringBuilder message = new StringBuilder("Il te reste ").append(balance).append(" pts à miser avant lundi 12h GMT.");
            PodiumGap gap = gaps.get(user.getId());
            if (gap != null) {
                message.append(" Tu es à ").append(gap.points()).append(" pts du podium de « ").append(gap.league()).append(" ».");
            }
            AppNotification notification = new AppNotification();
            notification.setUser(user);
            notification.setType(NotificationType.WEEKLY_REMINDER);
            notification.setMessage(message.toString());
            notificationRepository.save(notification);
            pushService.sendToUser(user.getId(), new PushService.PushMessage("Dernière ligne droite", message.toString(), "/matches"));
            sent++;
        }
        return sent;
    }

    record PodiumGap(String league, long points) {}

    // Pour chaque joueur hors podium : sa ligue où le podium de la semaine est le plus proche.
    private Map<String, PodiumGap> podiumGaps(Map<String, Long> scores) {
        Map<String, PodiumGap> best = new HashMap<>();
        for (League league : leagueRepository.findAll()) {
            List<String> members = membershipRepository.findByLeagueId(league.getId()).stream()
                    .map(m -> m.getUser().getId()).toList();
            // Avec 3 membres ou moins, tout le monde est sur le podium.
            if (members.size() <= 3) continue;
            long third = members.stream().map(id -> scores.getOrDefault(id, 0L))
                    .sorted(Comparator.reverseOrder()).skip(2).findFirst().orElse(0L);
            for (String id : members) {
                long gap = third - scores.getOrDefault(id, 0L);
                if (gap <= 0) continue;
                PodiumGap current = best.get(id);
                if (current == null || gap < current.points()) best.put(id, new PodiumGap(league.getName(), gap));
            }
        }
        return best;
    }
}
