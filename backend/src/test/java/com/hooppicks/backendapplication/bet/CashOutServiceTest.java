package com.hooppicks.backendapplication.bet;

import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.entity.BetSelection;
import com.hooppicks.backendapplication.entity.BetStatus;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.live.LiveMatchService;
import com.hooppicks.backendapplication.nba.NbaSyncService;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CashOutServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-10T20:00:00Z");

    private final BetRepository bets = mock(BetRepository.class);
    private final MatchRepository matches = mock(MatchRepository.class);
    private final LiveMatchService live = mock(LiveMatchService.class);
    private final PlayerPropsService props = mock(PlayerPropsService.class);
    private final NbaSyncService sync = mock(NbaSyncService.class);
    private final CashOutService service = new CashOutService(bets, matches, live, props, sync,
            TransactionOperations.withoutTransaction(), Clock.fixed(NOW, ZoneOffset.UTC));

    private Bet bet;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId("me");
        Match m = new Match();
        m.setId("m");
        m.setStatus(MatchStatus.SCHEDULED);
        m.setDate(NOW.plusSeconds(3600 * 5));
        m.setMoneylineHome(2.0);
        m.setMoneylineAway(1.9);
        BetSelection s = new BetSelection();
        s.setMatchId("m");
        s.setMarket("moneyline");
        s.setOutcome("home");
        s.setOdds(2.0);
        bet = new Bet();
        bet.setId("b");
        bet.setUser(user);
        bet.setStake(100);
        bet.setPotentialPayout(200);
        bet.setSelections(new java.util.ArrayList<>(List.of(s)));
        s.setBet(bet);
        when(bets.findById("b")).thenReturn(Optional.of(bet));
        when(bets.findByIdForUpdate("b")).thenReturn(Optional.of(bet));
        when(bets.findByUserIdOrderByPlacedAtDesc("me")).thenReturn(List.of(bet));
        when(matches.findAllById(any())).thenReturn(List.of(m));
        when(sync.tryRunExclusive(any())).thenAnswer(inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return true;
        });
    }

    @Test
    void la_valeur_du_moment_est_proposee_pour_les_tickets_en_attente() {
        assertThat(service.quotes("me")).singleElement().satisfies(q -> {
            assertThat(q.betId()).isEqualTo("b");
            assertThat(q.value()).isEqualTo(93);
            assertThat(q.available()).isTrue();
        });
        verify(live, never()).pricing(any()); // match dans 5 h : pas d'appel au direct
    }

    @Test
    void encaisser_regle_le_ticket_sans_toucher_au_solde() {
        bet.getUser().setWalletBalance(400);

        Bet done = service.cashOut("me", "b", 93);

        assertThat(done.getStatus()).isEqualTo(BetStatus.CASHED_OUT);
        assertThat(done.getPotentialPayout()).isEqualTo(93);
        assertThat(done.getCashedOutFrom()).isEqualTo(200);
        assertThat(done.getResolvedAt()).isEqualTo(NOW);
        assertThat(bet.getUser().getWalletBalance()).isEqualTo(400);
    }

    @Test
    void une_valeur_plus_basse_que_celle_affichee_est_refusee_avec_la_nouvelle() {
        assertThatThrownBy(() -> service.cashOut("me", "b", 120))
                .isInstanceOfSatisfying(CashOutService.CashOutException.class, e -> {
                    assertThat(e.status()).isEqualTo(409);
                    assertThat(e.value()).isEqualTo(93);
                });
        assertThat(bet.getStatus()).isEqualTo(BetStatus.PENDING);
    }

    @Test
    void jamais_pendant_la_resolution_des_paris() {
        doReturn(false).when(sync).tryRunExclusive(any());

        assertThatThrownBy(() -> service.cashOut("me", "b", 93))
                .isInstanceOfSatisfying(CashOutService.CashOutException.class, e -> assertThat(e.status()).isEqualTo(409));
        assertThat(bet.getStatus()).isEqualTo(BetStatus.PENDING);
    }

    @Test
    void un_ticket_deja_regle_ou_d_un_autre_ne_s_encaisse_pas() {
        assertThatThrownBy(() -> service.cashOut("autre", "b", 1))
                .isInstanceOfSatisfying(CashOutService.CashOutException.class, e -> assertThat(e.status()).isEqualTo(404));

        bet.setStatus(BetStatus.WON);
        assertThatThrownBy(() -> service.cashOut("me", "b", 1))
                .isInstanceOfSatisfying(CashOutService.CashOutException.class, e -> assertThat(e.status()).isEqualTo(409));
    }
}
