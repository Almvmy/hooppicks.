package com.hooppicks.backendapplication.league;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.League;
import com.hooppicks.backendapplication.entity.LeagueMembership;
import com.hooppicks.backendapplication.leaderboard.LeaderboardPeriod;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.LeagueMembershipRepository;
import com.hooppicks.backendapplication.repository.LeagueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Ligue contre ligue : score d'une ligue = moyenne de ses 5 meilleurs joueurs
 * sur la période (mêmes points que le classement). Une moyenne, pour qu'une
 * grosse ligue n'écrase pas une petite par le nombre ; les 5 meilleurs, pour
 * qu'un membre inactif ne la plombe pas. 3 membres au moins : à deux, ce
 * serait un duel. Seuls le nom de la ligue et son nombre de membres sortent,
 * jamais le code d'invitation ni la liste des joueurs.
 */
@Service
public class LeagueRankingService {

    static final int TOP_PLAYERS = 5;
    static final int MIN_MEMBERS = 3;
    static final int SHOWN = 20;

    /** `mine` : le joueur en est membre. `rank` : place, ex æquo compris. */
    public record LeagueRow(int rank, String id, String name, int members, long score, boolean mine) {}

    private final BetRepository betRepository;
    private final LeagueRepository leagueRepository;
    private final LeagueMembershipRepository membershipRepository;
    private final Clock clock;

    @Autowired
    public LeagueRankingService(BetRepository betRepository, LeagueRepository leagueRepository, LeagueMembershipRepository membershipRepository) {
        this(betRepository, leagueRepository, membershipRepository, Clock.system(BankrollService.ZONE));
    }

    LeagueRankingService(BetRepository betRepository, LeagueRepository leagueRepository, LeagueMembershipRepository membershipRepository, Clock clock) {
        this.betRepository = betRepository;
        this.leagueRepository = leagueRepository;
        this.membershipRepository = membershipRepository;
        this.clock = clock;
    }

    /** Les 20 premières, plus celles du joueur si elles sont plus bas. */
    @Transactional(readOnly = true)
    public List<LeagueRow> ranking(LeaderboardPeriod period, String userId) {
        Map<String, Long> points = new HashMap<>();
        for (Object[] row : betRepository.getLeaderboardRawSince(period.start(ZonedDateTime.now(clock)), period.phase())) {
            points.put((String) row[0], ((Number) row[2]).longValue());
        }

        record Scored(League league, int members, long score, boolean mine) {}
        List<Scored> scored = new ArrayList<>();
        for (League league : leagueRepository.findAll()) {
            List<LeagueMembership> memberships = membershipRepository.findByLeagueId(league.getId());
            if (memberships.size() < MIN_MEMBERS) continue;
            List<Long> best = memberships.stream()
                    .map(m -> points.getOrDefault(m.getUser().getId(), 0L))
                    .sorted(Comparator.reverseOrder()).limit(TOP_PLAYERS).toList();
            long score = Math.round(best.stream().mapToLong(Long::longValue).average().orElse(0));
            boolean mine = memberships.stream().anyMatch(m -> m.getUser().getId().equals(userId));
            scored.add(new Scored(league, memberships.size(), score, mine));
        }
        scored.sort(Comparator.comparingLong(Scored::score).reversed().thenComparing(s -> s.league().getName(), String.CASE_INSENSITIVE_ORDER));

        List<LeagueRow> rows = new ArrayList<>();
        int rank = 0;
        long previous = Long.MIN_VALUE;
        for (int i = 0; i < scored.size(); i++) {
            Scored s = scored.get(i);
            if (s.score() != previous) rank = i + 1;
            previous = s.score();
            if (i < SHOWN || s.mine()) {
                rows.add(new LeagueRow(rank, s.league().getId(), s.league().getName(), s.members(), s.score(), s.mine()));
            }
        }
        return rows.stream().collect(Collectors.toList());
    }
}
