package com.hooppicks.backendapplication.admin;

import com.hooppicks.backendapplication.entity.AppNotification;
import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.entity.BetStatus;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.NotificationType;
import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.entity.TransactionType;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.entity.WalletTransaction;
import com.hooppicks.backendapplication.nba.NbaSyncService;
import com.hooppicks.backendapplication.push.PushService;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminActionsServiceTest {

    @Mock private BetRepository betRepository;
    @Mock private MatchRepository matchRepository;
    @Mock private UserRepository userRepository;
    @Mock private WalletTransactionRepository transactionRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private PushService pushService;
    @Mock private NbaSyncService nbaSyncService;
    @Mock private AdminAuditService auditService;
    @Mock private PlatformTransactionManager transactionManager;

    private AdminActionsService service;
    private final User admin = user("admin", 0);

    @BeforeEach
    void setUp() {
        service = new AdminActionsService(betRepository, matchRepository, userRepository, transactionRepository,
                notificationRepository, pushService, nbaSyncService, auditService, transactionManager);
        // Verrou libre par défaut : le travail s'exécute.
        when(nbaSyncService.tryRunExclusive(any())).thenAnswer(inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return true;
        });
    }

    private static User user(String id, int balance) {
        User u = new User();
        u.setId(id);
        u.setUsername("pseudo-" + id);
        u.setWalletBalance(balance);
        return u;
    }

    private Bet pendingBet(String id, User owner, int stake) {
        Bet b = new Bet();
        b.setId(id);
        b.setUser(owner);
        b.setStake(stake);
        b.setStatus(BetStatus.PENDING);
        when(betRepository.findById(id)).thenReturn(Optional.of(b));
        when(userRepository.findByIdForUpdate(owner.getId())).thenReturn(Optional.of(owner));
        return b;
    }

    @Test
    void annuler_un_pari_rembourse_la_mise_trace_et_previent_le_joueur() {
        User player = user("u1", 300);
        Bet bet = pendingBet("b1", player, 200);

        service.voidBet("b1", "Match reporté", admin);

        assertThat(bet.getStatus()).isEqualTo(BetStatus.VOID);
        assertThat(bet.getResolvedAt()).isNotNull();
        assertThat(player.getWalletBalance()).isEqualTo(500);
        ArgumentCaptor<WalletTransaction> tx = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(transactionRepository).save(tx.capture());
        assertThat(tx.getValue().getAmount()).isEqualTo(200);
        assertThat(tx.getValue().getType()).isEqualTo(TransactionType.BONUS);
        ArgumentCaptor<AppNotification> notif = ArgumentCaptor.forClass(AppNotification.class);
        verify(notificationRepository).save(notif.capture());
        assertThat(notif.getValue().getType()).isEqualTo(NotificationType.SYSTEM);
        assertThat(notif.getValue().getMessage()).contains("200 pts").contains("Match reporté");
        verify(auditService).log(eq(admin), eq("VOID_BET"), eq("pseudo-u1"), anyString());
    }

    @Test
    void un_pari_deja_regle_ne_peut_pas_etre_annule() {
        User player = user("u1", 300);
        Bet bet = pendingBet("b1", player, 200);
        bet.setStatus(BetStatus.WON);

        assertThatThrownBy(() -> service.voidBet("b1", "Erreur", admin))
                .isInstanceOf(AdminActionException.class)
                .extracting(e -> ((AdminActionException) e).getStatus()).isEqualTo(400);
        assertThat(player.getWalletBalance()).isEqualTo(300);
        verifyNoInteractions(auditService);
    }

    @Test
    void synchro_en_cours_action_refusee_en_409_sans_rien_toucher() {
        doReturn(false).when(nbaSyncService).tryRunExclusive(any());
        User player = user("u1", 300);
        pendingBet("b1", player, 200);

        assertThatThrownBy(() -> service.voidBet("b1", "Match reporté", admin))
                .isInstanceOf(AdminActionException.class)
                .extracting(e -> ((AdminActionException) e).getStatus()).isEqualTo(409);
        assertThat(player.getWalletBalance()).isEqualTo(300);
    }

    @Test
    void motif_obligatoire() {
        assertThatThrownBy(() -> service.voidBet("b1", "  ", admin)).isInstanceOf(AdminActionException.class);
        assertThatThrownBy(() -> service.adjustWallet("u1", 100, null, admin)).isInstanceOf(AdminActionException.class);
        verifyNoInteractions(betRepository, userRepository);
    }

    @Test
    void annuler_les_paris_d_un_match_les_rembourse_tous() {
        Team home = new Team();
        home.setAbbreviation("NYK");
        Team away = new Team();
        away.setAbbreviation("MIA");
        Match match = new Match();
        match.setId("m1");
        match.setHomeTeam(home);
        match.setAwayTeam(away);
        when(matchRepository.findById("m1")).thenReturn(Optional.of(match));
        User a = user("u1", 0);
        User b = user("u2", 50);
        Bet b1 = pendingBet("b1", a, 100);
        Bet b2 = pendingBet("b2", b, 40);
        when(betRepository.findPendingBetsForMatch("m1")).thenReturn(List.of(b1, b2));

        int voided = service.voidPendingBetsForMatch("m1", "Match annulé", admin);

        assertThat(voided).isEqualTo(2);
        assertThat(a.getWalletBalance()).isEqualTo(100);
        assertThat(b.getWalletBalance()).isEqualTo(90);
        verify(auditService).log(eq(admin), eq("VOID_MATCH_BETS"), eq("MIA @ NYK"), anyString());
    }

    @Test
    void ajuster_un_solde_credit_et_debit_jamais_negatif() {
        User player = user("u1", 100);
        when(userRepository.findByIdForUpdate("u1")).thenReturn(Optional.of(player));

        assertThat(service.adjustWallet("u1", 250, "Compensation bug", admin)).isEqualTo(350);
        assertThat(service.adjustWallet("u1", -50, "Correction", admin)).isEqualTo(300);
        assertThatThrownBy(() -> service.adjustWallet("u1", -400, "Trop", admin))
                .isInstanceOf(AdminActionException.class);
        assertThat(player.getWalletBalance()).isEqualTo(300);
        assertThatThrownBy(() -> service.adjustWallet("u1", 0, "Rien", admin)).isInstanceOf(AdminActionException.class);
        verify(auditService, times(2)).log(eq(admin), eq("ADJUST_WALLET"), eq("pseudo-u1"), anyString());
    }

    @Test
    void annonce_a_tous_avec_push_optionnel() {
        when(userRepository.findAll()).thenReturn(List.of(user("u1", 0), user("u2", 0)));

        int sent = service.announce("  Nouvelle saison !  ", true, admin);

        assertThat(sent).isEqualTo(2);
        verify(notificationRepository, times(2)).save(any());
        verify(pushService, times(2)).sendToUser(anyString(), any());
        assertThatThrownBy(() -> service.announce("", false, admin)).isInstanceOf(AdminActionException.class);
        assertThatThrownBy(() -> service.announce("x".repeat(281), false, admin)).isInstanceOf(AdminActionException.class);
    }
}
