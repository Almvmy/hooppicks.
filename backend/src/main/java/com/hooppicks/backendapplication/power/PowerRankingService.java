package com.hooppicks.backendapplication.power;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.entity.TeamEloSnapshot;
import com.hooppicks.backendapplication.repository.TeamEloSnapshotRepository;
import com.hooppicks.backendapplication.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Power rankings HoopPicks : les 30 équipes classées par Elo (celui qui fixe
 * les cotes, mis à jour après chaque match), avec l'évolution sur une semaine
 * d'après une photo quotidienne prise à midi GMT (comme le classement des joueurs).
 */
@Service
public class PowerRankingService {

    private final TeamRepository teamRepository;
    private final TeamEloSnapshotRepository snapshotRepository;
    private final Clock clock;

    @Autowired
    public PowerRankingService(TeamRepository teamRepository, TeamEloSnapshotRepository snapshotRepository) {
        this(teamRepository, snapshotRepository, Clock.system(BankrollService.ZONE));
    }

    PowerRankingService(TeamRepository teamRepository, TeamEloSnapshotRepository snapshotRepository, Clock clock) {
        this.teamRepository = teamRepository;
        this.snapshotRepository = snapshotRepository;
        this.clock = clock;
    }

    public boolean hasSnapshot(LocalDate date) {
        return snapshotRepository.existsByDate(date);
    }

    @Transactional
    public void takeSnapshot(LocalDate date) {
        List<TeamEloSnapshot> rows = teamRepository.findAll().stream().map(t -> {
            TeamEloSnapshot s = new TeamEloSnapshot();
            s.setId(date + ":" + t.getId());
            s.setDate(date);
            s.setTeamId(t.getId());
            s.setElo(t.getEloRating());
            return s;
        }).toList();
        snapshotRepository.saveAll(rows);
    }

    /** previousRank / eloChange : null tant qu'il n'existe pas de photo d'au moins 7 jours. */
    public record Entry(String teamId, String abbreviation, int rank, Integer previousRank, int elo, Integer eloChange) {}

    public List<Entry> rankings() {
        List<Team> teams = teamRepository.findAll().stream()
                .sorted(Comparator.comparingDouble(Team::getEloRating).reversed().thenComparing(Team::getAbbreviation))
                .toList();

        LocalDate weekAgo = LocalDate.now(clock).minusDays(7);
        Map<String, Double> before = snapshotRepository.findLatestDateOnOrBefore(weekAgo)
                .map(d -> snapshotRepository.findByDate(d).stream()
                        .collect(Collectors.toMap(TeamEloSnapshot::getTeamId, TeamEloSnapshot::getElo, (a, b) -> a)))
                .orElse(Map.of());
        List<String> beforeOrder = before.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey).toList();

        List<Entry> out = new ArrayList<>();
        for (int i = 0; i < teams.size(); i++) {
            Team t = teams.get(i);
            Double old = before.get(t.getId());
            int previous = beforeOrder.indexOf(t.getId());
            out.add(new Entry(t.getId(), t.getAbbreviation(), i + 1, previous < 0 ? null : previous + 1,
                    (int) Math.round(t.getEloRating()), old == null ? null : (int) Math.round(t.getEloRating() - old)));
        }
        return out;
    }
}
