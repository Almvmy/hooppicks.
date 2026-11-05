package com.hooppicks.backendapplication.engagement;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WeeklyRecapServiceTest {

    // Mardi 10 novembre : la semaine qui vient de finir est celle du lundi 2.
    private static final Instant NOW = Instant.parse("2026-11-10T10:00:00Z");
    private static final LocalDate WEEK = LocalDate.of(2026, 11, 2);
    private static final Instant IN_WEEK = Instant.parse("2026-11-04T20:00:00Z");

    private final BetRepository betRepository = mock(BetRepository.class);
    private final LeagueMembershipRepository membershipRepository = mock(LeagueMembershipRepository.class);
    private final DuelRepository duelRepository = mock(DuelRepository.class);
    private final WeeklyTitleRepository titleRepository = mock(WeeklyTitleRepository.class);
    private final WeeklyRecapService service = new WeeklyRecapService(betRepository, membershipRepository, duelRepository,
            titleRepository, Clock.fixed(NOW, BankrollService.ZONE));

    private static Bet bet(String id, BetStatus status, int stake, int payout, Instant placedAt, String... labels) {
        Bet b = new Bet();
        b.setId(id);
        b.setStatus(status);
        b.setStake(stake);
        b.setPotentialPayout(payout);
        b.setTotalOdds(payout / (double) stake);
        b.setPlacedAt(placedAt);
        for (String label : labels) {
            BetSelection s = new BetSelection();
            s.setLabel(label);
            b.getSelections().add(s);
        }
        return b;
    }

    private static User user(String id, String name) {
        User u = new User();
        u.setId(id);
        u.setUsername(name);
        return u;
    }

    @BeforeEach
    void setUp() {
        when(betRepository.getAllWeekScores(any())).thenReturn(List.of());
        when(membershipRepository.findByUserId("me")).thenReturn(List.of());
        when(duelRepository.findAllForUser("me")).thenReturn(List.of());
        when(titleRepository.findByUserIdOrderByWeekDesc("me")).thenReturn(List.of());
    }

    @Test
    void rien_sans_ticket_regle_la_semaine_passee() {
        when(betRepository.findByUserIdOrderByPlacedAtDesc("me")).thenReturn(List.of(
                bet("cette-semaine", BetStatus.WON, 10, 20, Instant.parse("2026-11-09T20:00:00Z"), "LAL (V)"),
                bet("en-attente", BetStatus.PENDING, 10, 20, IN_WEEK, "BOS (V)")));

        assertThat(service.lastWeek("me")).isEmpty();
    }

    @Test
    void bilan_meilleur_ticket_et_place() {
        when(betRepository.findByUserIdOrderByPlacedAtDesc("me")).thenReturn(List.of(
                bet("a", BetStatus.WON, 100, 300, IN_WEEK, "LAL (V)", "BOS -3,5"),
                bet("b", BetStatus.WON, 50, 80, IN_WEEK, "NYK (V)"),
                bet("c", BetStatus.LOST, 40, 90, IN_WEEK, "OKC (V)")));
        when(betRepository.getAllWeekScores(WEEK.atStartOfDay())).thenReturn(List.of(
                new Object[]{"x", 500L}, new Object[]{"me", 190L}, new Object[]{"y", 100L}));

        WeeklyRecapService.Recap recap = service.lastWeek("me").orElseThrow();

        assertThat(recap.net()).isEqualTo(190);
        assertThat(recap.points()).isEqualTo(190);
        assertThat(recap.tickets()).isEqualTo(3);
        assertThat(recap.won()).isEqualTo(2);
        assertThat(recap.rank()).isEqualTo(2);
        assertThat(recap.players()).isEqualTo(3);
        assertThat(recap.best().betId()).isEqualTo("a");
        assertThat(recap.best().labels()).isEqualTo("LAL (V) + BOS -3,5");
    }

    @Test
    void semaine_negative_vaut_zero_point() {
        when(betRepository.findByUserIdOrderByPlacedAtDesc("me")).thenReturn(List.of(bet("c", BetStatus.LOST, 40, 90, IN_WEEK, "OKC (V)")));

        WeeklyRecapService.Recap recap = service.lastWeek("me").orElseThrow();

        assertThat(recap.net()).isEqualTo(-40);
        assertThat(recap.points()).isZero();
        assertThat(recap.best()).isNull();
    }

    @Test
    void ligues_titres_et_duels_de_la_semaine() {
        when(betRepository.findByUserIdOrderByPlacedAtDesc("me")).thenReturn(List.of(bet("a", BetStatus.WON, 100, 300, IN_WEEK, "LAL (V)")));
        when(betRepository.getAllWeekScores(WEEK.atStartOfDay())).thenReturn(List.of(new Object[]{"me", 200L}, new Object[]{"x", 50L}));

        User me = user("me", "moi");
        User x = user("x", "Kader");
        League league = new League();
        league.setId("l");
        league.setName("Les potes");
        LeagueMembership mine = new LeagueMembership();
        mine.setLeague(league);
        mine.setUser(me);
        LeagueMembership other = new LeagueMembership();
        other.setLeague(league);
        other.setUser(x);
        when(membershipRepository.findByUserId("me")).thenReturn(List.of(mine));
        when(membershipRepository.findByLeagueId("l")).thenReturn(List.of(mine, other));

        WeeklyTitle title = new WeeklyTitle();
        title.setWeek(WEEK);
        title.setLeagueId("l");
        WeeklyTitle old = new WeeklyTitle();
        old.setWeek(WEEK.minusWeeks(2));
        when(titleRepository.findByUserIdOrderByWeekDesc("me")).thenReturn(List.of(title, old));

        Duel duel = new Duel();
        duel.setChallenger(x);
        duel.setOpponent(me);
        duel.setWeek(WEEK);
        duel.setStatus(DuelStatus.FINISHED);
        duel.setChallengerPoints(50L);
        duel.setOpponentPoints(200L);
        duel.setWinnerId("me");
        when(duelRepository.findAllForUser("me")).thenReturn(List.of(duel));

        WeeklyRecapService.Recap recap = service.lastWeek("me").orElseThrow();

        assertThat(recap.champion()).isFalse();
        assertThat(recap.leagues()).containsExactly(new WeeklyRecapService.LeagueLine("l", "Les potes", 1, 2, true));
        assertThat(recap.duels()).containsExactly(new WeeklyRecapService.DuelLine("Kader", 200, 50, "won"));
    }
}
