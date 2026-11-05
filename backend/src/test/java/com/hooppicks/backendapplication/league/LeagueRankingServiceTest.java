package com.hooppicks.backendapplication.league;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.League;
import com.hooppicks.backendapplication.entity.LeagueMembership;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.leaderboard.LeaderboardPeriod;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.LeagueMembershipRepository;
import com.hooppicks.backendapplication.repository.LeagueRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LeagueRankingServiceTest {

    private final BetRepository betRepository = mock(BetRepository.class);
    private final LeagueRepository leagueRepository = mock(LeagueRepository.class);
    private final LeagueMembershipRepository membershipRepository = mock(LeagueMembershipRepository.class);
    private final LeagueRankingService service = new LeagueRankingService(betRepository, leagueRepository, membershipRepository,
            Clock.fixed(Instant.parse("2026-11-05T15:00:00Z"), BankrollService.ZONE));

    private final List<League> leagues = new ArrayList<>();
    private final List<Object[]> points = new ArrayList<>();

    private void league(String id, String... userIds) {
        League l = new League();
        l.setId(id);
        l.setName("Ligue " + id);
        leagues.add(l);
        when(membershipRepository.findByLeagueId(id)).thenReturn(List.of(userIds).stream().map(u -> {
            User user = new User();
            user.setId(u);
            LeagueMembership m = new LeagueMembership();
            m.setUser(user);
            return m;
        }).toList());
    }

    private void points(String user, long value) {
        points.add(new Object[]{user, user, value});
    }

    private List<LeagueRankingService.LeagueRow> ranking(String userId) {
        when(leagueRepository.findAll()).thenReturn(leagues);
        when(betRepository.getLeaderboardRawSince(any(), any())).thenReturn(points);
        return service.ranking(LeaderboardPeriod.WEEK, userId);
    }

    @Test
    void moyenne_des_5_meilleurs_une_grosse_ligue_n_ecrase_pas_une_petite() {
        // 8 membres : seuls les 5 meilleurs comptent, les 3 inactifs ne la plombent pas.
        league("grosse", "a", "b", "c", "d", "e", "f", "g", "h");
        for (String u : List.of("a", "b", "c", "d", "e")) points(u, 100);
        // 3 membres meilleurs en moyenne.
        league("petite", "x", "y", "z");
        for (String u : List.of("x", "y", "z")) points(u, 200);

        List<LeagueRankingService.LeagueRow> rows = ranking("a");

        assertThat(rows).extracting(LeagueRankingService.LeagueRow::id).containsExactly("petite", "grosse");
        assertThat(rows.get(0).score()).isEqualTo(200);
        assertThat(rows.get(1).score()).isEqualTo(100);
        assertThat(rows.get(1).mine()).isTrue();
        assertThat(rows.get(0).mine()).isFalse();
    }

    @Test
    void moins_de_3_membres_pas_classee() {
        league("duo", "a", "b");
        points("a", 500);

        assertThat(ranking("a")).isEmpty();
    }

    @Test
    void ex_aequo_meme_place() {
        league("un", "a", "b", "c");
        league("deux", "x", "y", "z");
        league("trois", "m", "n", "o");
        for (String u : List.of("a", "b", "c", "x", "y", "z")) points(u, 90);

        assertThat(ranking("a")).extracting(LeagueRankingService.LeagueRow::rank).containsExactly(1, 1, 3);
    }

    @Test
    void ma_ligue_reste_visible_au_dela_des_20_premieres() {
        IntStream.range(0, 25).forEach(i -> {
            String a = "a" + i, b = "b" + i, c = "c" + i;
            league("L" + i, a, b, c);
            points(a, 1000 - i);
        });
        league("fin", "me", "p", "q");

        List<LeagueRankingService.LeagueRow> rows = ranking("me");

        assertThat(rows).hasSize(21);
        assertThat(rows.get(20).id()).isEqualTo("fin");
        assertThat(rows.get(20).rank()).isEqualTo(26);
    }
}
