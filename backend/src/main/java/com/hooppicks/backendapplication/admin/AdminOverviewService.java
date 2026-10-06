package com.hooppicks.backendapplication.admin;

import com.hooppicks.backendapplication.entity.BetStatus;
import com.hooppicks.backendapplication.entity.MatchType;
import com.hooppicks.backendapplication.nba.AdminSyncStatus;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.RosterPlayerRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Chiffres de la vue d'ensemble de la console admin. */
@Service
public class AdminOverviewService {

    // Jours comptés dans le fuseau du jeu, comme le reste de l'app côté joueurs.
    static final ZoneId ZONE = com.hooppicks.backendapplication.bankroll.BankrollService.ZONE;
    static final int SERIES_DAYS = 14;

    public record DailyPoint(String date, int signups, int bets) {}

    public record Overview(
            long totalUsers, long newUsers7d, long activeBettors7d,
            long betsPlaced24h, long betsPlaced7d, long pointsStaked7d,
            long pointsInCirculation, long pendingBets,
            long totalMatches, long lockedMatches, long unlinkedMatches, Map<String, Long> matchesByType,
            long rosterPlayers, long playersWithoutStats,
            Instant lastSyncAt, int lastGamesSynced, int lastBetsResolved, String syncMode,
            List<DailyPoint> series
    ) {}

    private final UserRepository userRepository;
    private final BetRepository betRepository;
    private final MatchRepository matchRepository;
    private final RosterPlayerRepository rosterPlayerRepository;
    private final AdminSyncStatus adminSyncStatus;
    private final Clock clock;

    @Autowired
    public AdminOverviewService(UserRepository userRepository, BetRepository betRepository,
                                MatchRepository matchRepository, RosterPlayerRepository rosterPlayerRepository,
                                AdminSyncStatus adminSyncStatus) {
        this(userRepository, betRepository, matchRepository, rosterPlayerRepository, adminSyncStatus, Clock.systemUTC());
    }

    AdminOverviewService(UserRepository userRepository, BetRepository betRepository,
                         MatchRepository matchRepository, RosterPlayerRepository rosterPlayerRepository,
                         AdminSyncStatus adminSyncStatus, Clock clock) {
        this.userRepository = userRepository;
        this.betRepository = betRepository;
        this.matchRepository = matchRepository;
        this.rosterPlayerRepository = rosterPlayerRepository;
        this.adminSyncStatus = adminSyncStatus;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Overview overview() {
        Instant now = clock.instant();
        Instant weekAgo = now.minus(Duration.ofDays(7));

        Map<String, Long> byType = new LinkedHashMap<>();
        for (Object[] row : matchRepository.countByType()) {
            // Pas encore relié à ESPN = phase inconnue, compté à part.
            String key = row[0] == null ? "unknown" : ((MatchType) row[0]).name().toLowerCase();
            byType.put(key, (Long) row[1]);
        }

        return new Overview(
                userRepository.count(),
                userRepository.countByCreatedAtAfter(weekAgo),
                betRepository.countDistinctBettorsSince(weekAgo),
                betRepository.countByPlacedAtAfter(now.minus(Duration.ofHours(24))),
                betRepository.countByPlacedAtAfter(weekAgo),
                betRepository.sumStakesSince(weekAgo),
                userRepository.sumWalletBalances(),
                betRepository.countByStatus(BetStatus.PENDING),
                matchRepository.count(),
                matchRepository.countByAdminLockedTrue(),
                matchRepository.countByEspnEventIdIsNull(),
                byType,
                rosterPlayerRepository.count(),
                rosterPlayerRepository.countByStatsUpdatedAtIsNull(),
                adminSyncStatus.getLastSyncAt(),
                adminSyncStatus.getLastGamesSynced(),
                adminSyncStatus.getLastBetsResolved(),
                adminSyncStatus.getMode(),
                series(now)
        );
    }

    /** Inscriptions et paris par jour sur les 14 derniers jours, jours vides compris. */
    List<DailyPoint> series(Instant now) {
        LocalDate today = now.atZone(ZONE).toLocalDate();
        LocalDate first = today.minusDays(SERIES_DAYS - 1);
        Instant since = first.atStartOfDay(ZONE).toInstant();

        Map<LocalDate, int[]> days = new LinkedHashMap<>();
        for (int i = 0; i < SERIES_DAYS; i++) days.put(first.plusDays(i), new int[2]);
        for (Instant t : userRepository.findCreatedAtSince(since)) {
            int[] counts = days.get(t.atZone(ZONE).toLocalDate());
            if (counts != null) counts[0]++;
        }
        for (Instant t : betRepository.findPlacedAtSince(since)) {
            int[] counts = days.get(t.atZone(ZONE).toLocalDate());
            if (counts != null) counts[1]++;
        }

        List<DailyPoint> points = new ArrayList<>();
        days.forEach((date, counts) -> points.add(new DailyPoint(date.toString(), counts[0], counts[1])));
        return points;
    }
}
