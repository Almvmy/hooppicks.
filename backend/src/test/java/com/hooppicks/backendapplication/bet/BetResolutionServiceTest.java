package com.hooppicks.backendapplication.bet;

import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.NotificationRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import com.hooppicks.backendapplication.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BetResolutionServiceTest {

    @Mock
    private BetRepository betRepository;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WalletTransactionRepository transactionRepository;
    @Mock
    private NotificationRepository notificationRepository;

    private BetResolutionService service;

    @BeforeEach
    void setUp() {
        service = new BetResolutionService(betRepository, matchRepository, userRepository,
                transactionRepository, notificationRepository);
    }

    private Match finishedMatch(String id, int homeScore, int awayScore, double spreadValue, double totalValue) {
        Match match = new Match();
        match.setId(id);
        match.setStatus(MatchStatus.FINISHED);
        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        match.setSpreadValue(spreadValue);
        match.setTotalValue(totalValue);
        return match;
    }

    private BetSelection selection(String matchId, String market, String outcome) {
        BetSelection s = new BetSelection();
        s.setMatchId(matchId);
        s.setMarket(market);
        s.setOutcome(outcome);
        s.setOdds(1.9);
        return s;
    }

    private Bet pendingBet(User user, int stake, int potentialPayout, BetSelection... selections) {
        Bet bet = new Bet();
        bet.setUser(user);
        bet.setStake(stake);
        bet.setPotentialPayout(potentialPayout);
        bet.setStatus(BetStatus.PENDING);
        for (BetSelection s : selections) bet.getSelections().add(s);
        return bet;
    }

    private User user(int startingBalance) {
        User u = new User();
        u.setId("user-1");
        u.setWalletBalance(startingBalance);
        return u;
    }

    @Test
    void moneyline_gagnant_credite_le_solde_et_notifie() {
        Match match = finishedMatch("m1", 100, 90, -2.5, 220.5);
        User user = user(1000);
        Bet bet = pendingBet(user, 10, 18, selection("m1", "moneyline", "home"));

        when(betRepository.findByStatus(BetStatus.PENDING)).thenReturn(List.of(bet));
        when(matchRepository.findAllById(any())).thenReturn(List.of(match));

        int resolved = service.resolvePendingBets();

        assertThat(resolved).isEqualTo(1);
        assertThat(bet.getStatus()).isEqualTo(BetStatus.WON);
        assertThat(bet.getResolvedAt()).isNotNull();
        assertThat(user.getWalletBalance()).isEqualTo(1018);

        ArgumentCaptor<AppNotification> notifCaptor = ArgumentCaptor.forClass(AppNotification.class);
        verify(notificationRepository).save(notifCaptor.capture());
        assertThat(notifCaptor.getValue().getType()).isEqualTo(NotificationType.BET_WON);
    }

    @Test
    void moneyline_perdant_ne_credite_rien_et_notifie_la_defaite() {
        Match match = finishedMatch("m1", 90, 100, -2.5, 220.5); // l'extérieur gagne
        User user = user(1000);
        Bet bet = pendingBet(user, 10, 18, selection("m1", "moneyline", "home"));

        when(betRepository.findByStatus(BetStatus.PENDING)).thenReturn(List.of(bet));
        when(matchRepository.findAllById(any())).thenReturn(List.of(match));

        service.resolvePendingBets();

        assertThat(bet.getStatus()).isEqualTo(BetStatus.LOST);
        assertThat(user.getWalletBalance()).isEqualTo(1000); // inchangé, la mise a déjà été débitée à la pose

        ArgumentCaptor<AppNotification> notifCaptor = ArgumentCaptor.forClass(AppNotification.class);
        verify(notificationRepository).save(notifCaptor.capture());
        assertThat(notifCaptor.getValue().getType()).isEqualTo(NotificationType.BET_LOST);
    }

    @Test
    void spread_couvert_est_gagnant() {
        // Domicile favori à -2.5, gagne de 10 -> couvre largement
        Match match = finishedMatch("m1", 100, 90, -2.5, 220.5);
        User user = user(1000);
        Bet bet = pendingBet(user, 10, 19, selection("m1", "spread", "home"));

        when(betRepository.findByStatus(BetStatus.PENDING)).thenReturn(List.of(bet));
        when(matchRepository.findAllById(any())).thenReturn(List.of(match));

        service.resolvePendingBets();

        assertThat(bet.getStatus()).isEqualTo(BetStatus.WON);
    }

    @Test
    void spread_non_couvert_est_perdant() {
        // Domicile favori à -2.5, gagne seulement de 1 -> ne couvre pas
        Match match = finishedMatch("m1", 91, 90, -2.5, 220.5);
        User user = user(1000);
        Bet bet = pendingBet(user, 10, 19, selection("m1", "spread", "home"));

        when(betRepository.findByStatus(BetStatus.PENDING)).thenReturn(List.of(bet));
        when(matchRepository.findAllById(any())).thenReturn(List.of(match));

        service.resolvePendingBets();

        assertThat(bet.getStatus()).isEqualTo(BetStatus.LOST);
    }

    @Test
    void total_over_gagnant_quand_le_score_depasse_la_ligne() {
        Match match = finishedMatch("m1", 120, 110, -2.5, 220.5); // total 230 > 220.5
        User user = user(1000);
        Bet bet = pendingBet(user, 10, 19, selection("m1", "total", "over"));

        when(betRepository.findByStatus(BetStatus.PENDING)).thenReturn(List.of(bet));
        when(matchRepository.findAllById(any())).thenReturn(List.of(match));

        service.resolvePendingBets();

        assertThat(bet.getStatus()).isEqualTo(BetStatus.WON);
    }

    @Test
    void egalite_pile_sur_le_seuil_rembourse_la_mise_sans_gain() {
        // Spread entier (-3) + score qui tombe pile dessus -> push
        Match match = finishedMatch("m1", 100, 97, -3, 220.5);
        User user = user(1000);
        Bet bet = pendingBet(user, 10, 19, selection("m1", "spread", "home"));

        when(betRepository.findByStatus(BetStatus.PENDING)).thenReturn(List.of(bet));
        when(matchRepository.findAllById(any())).thenReturn(List.of(match));

        service.resolvePendingBets();

        assertThat(bet.getStatus()).isEqualTo(BetStatus.VOID);
        assertThat(user.getWalletBalance()).isEqualTo(1010); // mise remboursée, pas le gain potentiel

        ArgumentCaptor<AppNotification> notifCaptor = ArgumentCaptor.forClass(AppNotification.class);
        verify(notificationRepository).save(notifCaptor.capture());
        assertThat(notifCaptor.getValue().getType()).isEqualTo(NotificationType.SYSTEM);
    }

    @Test
    void une_seule_selection_perdante_fait_perdre_tout_le_ticket_meme_si_les_autres_gagnent() {
        Match matchWin = finishedMatch("m1", 100, 90, -2.5, 220.5);
        Match matchLose = finishedMatch("m2", 90, 100, -2.5, 220.5);
        User user = user(1000);
        Bet bet = pendingBet(user, 10, 36,
                selection("m1", "moneyline", "home"), // gagnant
                selection("m2", "moneyline", "home")  // perdant
        );

        when(betRepository.findByStatus(BetStatus.PENDING)).thenReturn(List.of(bet));
        when(matchRepository.findAllById(any())).thenReturn(List.of(matchWin, matchLose));

        service.resolvePendingBets();

        assertThat(bet.getStatus()).isEqualTo(BetStatus.LOST);
        assertThat(user.getWalletBalance()).isEqualTo(1000);
    }

    @Test
    void une_selection_push_n_empeche_pas_les_autres_de_faire_gagner_le_ticket() {
        Match matchWin = finishedMatch("m1", 100, 90, -2.5, 220.5);
        Match matchPush = finishedMatch("m2", 100, 97, -3, 220.5); // push sur le spread
        User user = user(1000);
        Bet bet = pendingBet(user, 10, 36,
                selection("m1", "moneyline", "home"), // gagnant
                selection("m2", "spread", "home")      // push
        );

        when(betRepository.findByStatus(BetStatus.PENDING)).thenReturn(List.of(bet));
        when(matchRepository.findAllById(any())).thenReturn(List.of(matchWin, matchPush));

        service.resolvePendingBets();

        assertThat(bet.getStatus()).isEqualTo(BetStatus.WON);
        assertThat(user.getWalletBalance()).isEqualTo(1036);
    }

    @Test
    void un_ticket_dont_un_match_n_est_pas_encore_termine_reste_en_attente() {
        Match finished = finishedMatch("m1", 100, 90, -2.5, 220.5);
        Match stillScheduled = new Match();
        stillScheduled.setId("m2");
        stillScheduled.setStatus(MatchStatus.SCHEDULED);

        User user = user(1000);
        Bet bet = pendingBet(user, 10, 36,
                selection("m1", "moneyline", "home"),
                selection("m2", "moneyline", "home")
        );

        when(betRepository.findByStatus(BetStatus.PENDING)).thenReturn(List.of(bet));
        when(matchRepository.findAllById(any())).thenReturn(List.of(finished, stillScheduled));

        int resolved = service.resolvePendingBets();

        assertThat(resolved).isZero();
        assertThat(bet.getStatus()).isEqualTo(BetStatus.PENDING);
        assertThat(bet.getResolvedAt()).isNull();
        assertThat(user.getWalletBalance()).isEqualTo(1000);
        verifyNoInteractions(notificationRepository, transactionRepository);
        verify(betRepository, never()).save(any());
    }
}
