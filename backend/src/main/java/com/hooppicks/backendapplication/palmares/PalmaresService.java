package com.hooppicks.backendapplication.palmares;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.push.PushService;
import com.hooppicks.backendapplication.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

/**
 * Palmarès : à la fin de chaque semaine de jeu, titre de champion pour le
 * meilleur score de tout HoopPicks et pour le meilleur de chaque ligue
 * (égalité : tous les ex æquo). Même score que le classement de la semaine.
 * Attendu que tous les tickets de la semaine soient réglés (24 h au plus),
 * comme les duels. Les semaines passées sans titre sont rattrapées.
 */
@Service
public class PalmaresService {

    private static final Logger log = LoggerFactory.getLogger(PalmaresService.class);
    private static final Duration GRACE = Duration.ofHours(24);

    private final BetRepository betRepository;
    private final WeeklyTitleRepository titleRepository;
    private final PalmaresWeekRepository weekRepository;
    private final LeagueRepository leagueRepository;
    private final LeagueMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final DuelRepository duelRepository;
    private final PushService pushService;
    private final Clock clock;

    @Autowired
    public PalmaresService(BetRepository betRepository, WeeklyTitleRepository titleRepository,
                           PalmaresWeekRepository weekRepository, LeagueRepository leagueRepository,
                           LeagueMembershipRepository membershipRepository, UserRepository userRepository,
                           NotificationRepository notificationRepository, DuelRepository duelRepository,
                           PushService pushService) {
        this(betRepository, titleRepository, weekRepository, leagueRepository, membershipRepository, userRepository,
                notificationRepository, duelRepository, pushService, Clock.system(BankrollService.ZONE));
    }

    PalmaresService(BetRepository betRepository, WeeklyTitleRepository titleRepository,
                    PalmaresWeekRepository weekRepository, LeagueRepository leagueRepository,
                    LeagueMembershipRepository membershipRepository, UserRepository userRepository,
                    NotificationRepository notificationRepository, DuelRepository duelRepository,
                    PushService pushService, Clock clock) {
        this.betRepository = betRepository;
        this.titleRepository = titleRepository;
        this.weekRepository = weekRepository;
        this.leagueRepository = leagueRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.duelRepository = duelRepository;
        this.pushService = pushService;
        this.clock = clock;
    }

    /** Attribue les titres des semaines terminées qui n'en ont pas encore. Renvoie le nombre de semaines traitées. */
    @Transactional
    public int recordEndedWeeks() {
        LocalDate current = BankrollService.weekOf(clock.instant());
        Instant now = clock.instant();
        // La plus récente semaine terminée est la seule à mériter une notification :
        // les semaines rattrapées (historique) sont attribuées en silence.
        LocalDate lastEnded = current.minusWeeks(1);
        int done = 0;
        for (LocalDate week : settledWeeks()) {
            if (!week.isBefore(current) || weekRepository.existsById(week)) continue;
            Instant start = BankrollService.startOf(week);
            Instant end = start.plus(Duration.ofDays(7));
            if (betRepository.countAllPendingPlacedBetween(start, end) > 0 && now.isBefore(end.plus(GRACE))) continue;
            award(week, week.equals(lastEnded));
            done++;
        }
        return done;
    }

    private List<LocalDate> settledWeeks() {
        List<LocalDate> weeks = new ArrayList<>();
        for (Object o : betRepository.findSettledWeeks()) {
            if (o instanceof LocalDateTime t) weeks.add(t.toLocalDate());
            else if (o instanceof java.sql.Timestamp t) weeks.add(t.toLocalDateTime().toLocalDate());
            else if (o instanceof Instant t) weeks.add(t.atOffset(ZoneOffset.UTC).toLocalDate());
            else if (o instanceof OffsetDateTime t) weeks.add(t.toLocalDate());
        }
        weeks.sort(null);
        return weeks;
    }

    private void award(LocalDate week, boolean notify) {
        Map<String, Long> scores = new HashMap<>();
        for (Object[] row : betRepository.getAllWeekScores(week.atStartOfDay())) {
            scores.put((String) row[0], ((Number) row[1]).longValue());
        }
        int titles = grant(week, scores, null, null, notify);
        for (League league : leagueRepository.findAll()) {
            Map<String, Long> members = new HashMap<>();
            for (LeagueMembership m : membershipRepository.findByLeagueId(league.getId())) {
                Long s = scores.get(m.getUser().getId());
                if (s != null) members.put(m.getUser().getId(), s);
            }
            // Un titre de ligue n'a de sens qu'avec un peu de concurrence.
            if (members.size() >= 2) titles += grant(week, members, league.getId(), league.getName(), notify);
        }
        PalmaresWeek marker = new PalmaresWeek();
        marker.setWeek(week);
        weekRepository.save(marker);
        log.info("Palmarès : semaine du {} attribuée ({} titre(s))", week, titles);
    }

    private int grant(LocalDate week, Map<String, Long> scores, String leagueId, String leagueName, boolean notify) {
        long best = scores.values().stream().mapToLong(Long::longValue).max().orElse(0);
        if (best <= 0) return 0; // une semaine où personne n'a gagné de points n'a pas de champion
        int count = 0;
        for (Map.Entry<String, Long> e : scores.entrySet()) {
            if (e.getValue() != best) continue;
            WeeklyTitle t = new WeeklyTitle();
            t.setUserId(e.getKey());
            t.setWeek(week);
            t.setLeagueId(leagueId);
            t.setLeagueName(leagueName);
            t.setPoints(best);
            titleRepository.save(t);
            count++;
            if (notify) notifyChampion(e.getKey(), leagueName, best);
        }
        return count;
    }

    private void notifyChampion(String userId, String leagueName, long points) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;
        String message = leagueName == null
                ? "Champion de la semaine sur tout HoopPicks avec " + points + " pts ! Le titre rejoint ton palmarès."
                : "Champion de la semaine dans ta ligue « " + leagueName + " » avec " + points + " pts !";
        AppNotification n = new AppNotification();
        n.setUser(user);
        n.setType(NotificationType.SYSTEM);
        n.setMessage(message);
        notificationRepository.save(n);
        if (user.isNotifyLeagueActivity()) {
            pushService.sendToUser(userId, new PushService.PushMessage("Champion de la semaine", message, "/profile"));
        }
    }

    public record LeagueTitles(String leagueName, long count) {}

    public record Palmares(long weeklyTitles, List<LeagueTitles> leagueTitles, long duelWins, Long bestWeek) {}

    @Transactional(readOnly = true)
    public Palmares of(String userId) {
        List<WeeklyTitle> titles = titleRepository.findByUserIdOrderByWeekDesc(userId);
        long global = titles.stream().filter(t -> t.getLeagueId() == null).count();
        Map<String, Long> byLeague = new TreeMap<>();
        for (WeeklyTitle t : titles) {
            if (t.getLeagueId() != null) byLeague.merge(t.getLeagueName(), 1L, Long::sum);
        }
        List<LeagueTitles> leagues = byLeague.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new LeagueTitles(e.getKey(), e.getValue())).toList();
        Long bestWeek = titles.stream().map(WeeklyTitle::getPoints).max(Long::compare).orElse(null);
        return new Palmares(global, leagues, duelRepository.countWins(userId), bestWeek);
    }
}
