package com.hooppicks.backendapplication.leaderboard;

import com.hooppicks.backendapplication.dto.LeaderboardEntryDto;
import com.hooppicks.backendapplication.entity.RankSnapshot;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.RankSnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LeaderboardService {

    // Les joueurs sont en France : "cette semaine" commence le lundi à minuit
    // heure de Paris, pas en UTC.
    public static final ZoneId ZONE = ZoneId.of("Europe/Paris");
    private static final int FORM_LENGTH = 5;
    // Historique gardé : de quoi comparer à la veille, avec de la marge.
    private static final int SNAPSHOT_RETENTION_DAYS = 30;

    private final BetRepository betRepository;
    private final RankSnapshotRepository rankSnapshotRepository;
    private final Clock clock;

    @Autowired
    public LeaderboardService(BetRepository betRepository, RankSnapshotRepository rankSnapshotRepository) {
        this(betRepository, rankSnapshotRepository, Clock.system(ZONE));
    }

    LeaderboardService(BetRepository betRepository, RankSnapshotRepository rankSnapshotRepository, Clock clock) {
        this.betRepository = betRepository;
        this.rankSnapshotRepository = rankSnapshotRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDto> leaderboard(LeaderboardPeriod period) {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(ZONE);
        List<Object[]> rows = period == LeaderboardPeriod.SEASON
                ? betRepository.getLeaderboardRaw()
                : betRepository.getLeaderboardRawSince(period.start(now));

        Map<String, List<String>> results = recentResults();
        // L'historique photographie le classement saison : l'évolution n'a
        // de sens que pour lui.
        PreviousRanks previous = period == LeaderboardPeriod.SEASON
                ? previousRanks(now.toLocalDate())
                : PreviousRanks.NONE;

        Map<String, Integer> currentRanks = ranksByUserId(rows);

        return LeaderboardEntryDto.fromRows(rows, userId -> {
            List<String> recent = results.getOrDefault(userId, List.of());
            Integer change = null;
            boolean newcomer = false;
            if (previous.available()) {
                Integer before = previous.ranks().get(userId);
                if (before == null) newcomer = true;
                else change = before - currentRanks.get(userId);
            }
            return new LeaderboardEntryDto.Extras(change, newcomer,
                    recent.subList(0, Math.min(FORM_LENGTH, recent.size())), streak(recent));
        });
    }

    /**
     * Photographie le classement saison du jour (remplace celle déjà prise
     * ce jour-là s'il y en a une) et purge l'historique trop ancien.
     */
    @Transactional
    public void takeSnapshot(LocalDate date) {
        rankSnapshotRepository.deleteBySnapshotDate(date);
        List<RankSnapshot> snapshots = new ArrayList<>();
        ranksByUserId(betRepository.getLeaderboardRaw()).forEach((userId, rank) -> {
            RankSnapshot s = new RankSnapshot();
            s.setUserId(userId);
            s.setRank(rank);
            s.setSnapshotDate(date);
            snapshots.add(s);
        });
        rankSnapshotRepository.saveAll(snapshots);
        rankSnapshotRepository.deleteOlderThan(date.minusDays(SNAPSHOT_RETENTION_DAYS));
    }

    @Transactional(readOnly = true)
    public boolean hasSnapshot(LocalDate date) {
        return rankSnapshotRepository.existsBySnapshotDate(date);
    }

    private record PreviousRanks(boolean available, Map<String, Integer> ranks) {
        static final PreviousRanks NONE = new PreviousRanks(false, Map.of());
    }

    // Rangs calculés par LeaderboardEntryDto.fromRows (égalités comprises),
    // qui ne garde pas l'id : on le reprend dans les lignes, même ordre.
    private static Map<String, Integer> ranksByUserId(List<Object[]> rows) {
        List<LeaderboardEntryDto> entries = LeaderboardEntryDto.fromRows(rows);
        Map<String, Integer> ranks = new HashMap<>();
        for (int i = 0; i < rows.size(); i++) ranks.put((String) rows.get(i)[0], entries.get(i).rank());
        return ranks;
    }

    // Dernière photo d'un jour précédent (normalement la veille ; plus
    // ancienne si le serveur était éteint) : celle du jour même n'est
    // jamais utilisée, sinon l'évolution retomberait à zéro dès la photo prise.
    private PreviousRanks previousRanks(LocalDate today) {
        LocalDate date = rankSnapshotRepository.findLatestDateBefore(today);
        if (date == null) return PreviousRanks.NONE;
        return new PreviousRanks(true, rankSnapshotRepository.findBySnapshotDate(date).stream()
                .collect(Collectors.toMap(RankSnapshot::getUserId, RankSnapshot::getRank, (a, b) -> a)));
    }

    private Map<String, List<String>> recentResults() {
        Map<String, List<String>> byUser = new HashMap<>();
        for (Object[] row : betRepository.getRecentResultsPerUser()) {
            String status = String.valueOf(row[1]);
            byUser.computeIfAbsent((String) row[0], k -> new ArrayList<>())
                    .add("WON".equals(status) ? "W" : "L");
        }
        return byUser;
    }

    /** +n : n tickets gagnés d'affilée (le plus récent en tête), -n : n perdus. */
    static int streak(List<String> recent) {
        if (recent.isEmpty()) return 0;
        String first = recent.get(0);
        int count = 0;
        for (String r : recent) {
            if (!r.equals(first)) break;
            count++;
        }
        return "W".equals(first) ? count : -count;
    }
}
