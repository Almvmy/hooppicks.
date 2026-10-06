package com.hooppicks.backendapplication.bankroll;

import com.hooppicks.backendapplication.entity.AppNotification;
import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.entity.TransactionType;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.entity.WalletTransaction;
import com.hooppicks.backendapplication.repository.BetRepository;
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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BankrollServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private BetRepository betRepository;
    @Mock private WalletTransactionRepository transactionRepository;
    @Mock private NotificationRepository notificationRepository;

    // Mercredi 7 octobre 2026, 8h00 GMT : semaine de jeu du lundi 5.
    private static final Instant NOW = Instant.parse("2026-10-07T08:00:00Z");
    private static final LocalDate WEEK = LocalDate.of(2026, 10, 5);

    private BankrollService service;

    @BeforeEach
    void setUp() {
        service = new BankrollService(userRepository, betRepository, transactionRepository, notificationRepository,
                Clock.fixed(NOW, BankrollService.ZONE));
    }

    private static User user(int balance, LocalDate week) {
        User u = new User();
        u.setId("u1");
        u.setWalletBalance(balance);
        u.setBankrollWeek(week);
        return u;
    }

    @Test
    void la_semaine_de_jeu_commence_le_lundi_a_midi_heure_de_paris() {
        // Lundi 5 octobre, 11h59 GMT : encore la semaine du lundi 28 septembre.
        assertThat(BankrollService.weekOf(Instant.parse("2026-10-05T11:59:00Z"))).isEqualTo(LocalDate.of(2026, 9, 28));
        // Midi pile : nouvelle semaine.
        assertThat(BankrollService.weekOf(Instant.parse("2026-10-05T12:00:00Z"))).isEqualTo(WEEK);
        // Dimanche soir suivant : toujours la même.
        assertThat(BankrollService.weekOf(Instant.parse("2026-10-11T21:00:00Z"))).isEqualTo(WEEK);
        // GMT toute l'année : pas de décalage quand l'Europe change d'heure.
        assertThat(BankrollService.weekStart(Instant.parse("2026-11-04T12:00:00Z")).toInstant())
                .isEqualTo(Instant.parse("2026-11-02T12:00:00Z"));
    }

    @Test
    void un_joueur_deja_dans_la_semaine_n_est_pas_touche() {
        User u = user(240, WEEK);

        assertThat(service.ensureCurrent(u)).isFalse();

        assertThat(u.getWalletBalance()).isEqualTo(240);
        verifyNoInteractions(transactionRepository, notificationRepository);
    }

    @Test
    void nouvelle_semaine_remet_le_solde_a_1000_et_trace_la_difference() {
        User u = user(0, WEEK.minusWeeks(1));
        when(betRepository.sumPendingStakesSince(eq("u1"), any())).thenReturn(0L);
        when(betRepository.getNetResultBetween(eq("u1"), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{4L, -350L}));

        assertThat(service.ensureCurrent(u)).isTrue();

        assertThat(u.getWalletBalance()).isEqualTo(1000);
        assertThat(u.getBankrollWeek()).isEqualTo(WEEK);
        ArgumentCaptor<WalletTransaction> tx = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(transactionRepository).save(tx.capture());
        assertThat(tx.getValue().getType()).isEqualTo(TransactionType.WEEKLY_BANKROLL);
        assertThat(tx.getValue().getAmount()).isEqualTo(1000);

        ArgumentCaptor<AppNotification> n = ArgumentCaptor.forClass(AppNotification.class);
        verify(notificationRepository).save(n.capture());
        assertThat(n.getValue().getMessage()).contains("-350 pts au classement").contains("4 ticket(s)");
        // Récap calculé sur la semaine écoulée du joueur, pas sur la semaine en cours.
        verify(betRepository).getNetResultBetween("u1", Instant.parse("2026-09-28T12:00:00Z"), Instant.parse("2026-10-05T12:00:00Z"));
    }

    @Test
    void un_solde_trop_haut_redescend_aussi_a_1000() {
        User u = user(4200, WEEK.minusWeeks(1));
        when(betRepository.getNetResultBetween(any(), any(), any())).thenReturn(List.<Object[]>of(new Object[]{0L, null}));

        service.ensureCurrent(u);

        assertThat(u.getWalletBalance()).isEqualTo(1000);
        // Pas de récap pour une semaine sans ticket résolu.
        verifyNoInteractions(notificationRepository);
    }

    @Test
    void une_mise_deja_engagee_dans_la_nouvelle_semaine_n_est_pas_rendue() {
        // Pari posé lundi 12h01, avant le passage du scheduler : la mise a été
        // débitée de l'ancien solde, elle reste engagée sur le nouveau.
        User u = user(600, WEEK.minusWeeks(1));
        when(betRepository.sumPendingStakesSince("u1", Instant.parse("2026-10-05T12:00:00Z"))).thenReturn(150L);
        when(betRepository.getNetResultBetween(any(), any(), any())).thenReturn(List.of());

        service.ensureCurrent(u);

        assertThat(u.getWalletBalance()).isEqualTo(850);
    }

    @Test
    void premier_passage_d_un_compte_existant_explique_les_nouvelles_regles() {
        User u = user(3500, null);

        service.ensureCurrent(u);

        assertThat(u.getWalletBalance()).isEqualTo(1000);
        ArgumentCaptor<AppNotification> n = ArgumentCaptor.forClass(AppNotification.class);
        verify(notificationRepository).save(n.capture());
        assertThat(n.getValue().getMessage()).startsWith("Nouveau : chaque lundi à 12h");
    }

    @Test
    void seul_un_pari_de_la_semaine_du_solde_le_credite() {
        User u = user(1000, WEEK);
        Bet thisWeek = new Bet();
        thisWeek.setPlacedAt(Instant.parse("2026-10-06T18:00:00Z"));
        Bet lastWeek = new Bet();
        lastWeek.setPlacedAt(Instant.parse("2026-10-04T18:00:00Z"));

        assertThat(service.paysIntoCurrentBalance(u, thisWeek)).isTrue();
        assertThat(service.paysIntoCurrentBalance(u, lastWeek)).isFalse();
    }

    @Test
    void rollOverAll_verrouille_chaque_joueur_en_retard() {
        User late = user(0, WEEK.minusWeeks(2));
        when(userRepository.findIdsWithBankrollWeekNot(WEEK)).thenReturn(List.of("u1"));
        when(userRepository.findByIdForUpdate("u1")).thenReturn(Optional.of(late));
        when(betRepository.getNetResultBetween(any(), any(), any())).thenReturn(List.of());

        assertThat(service.rollOverAll()).isEqualTo(1);
        assertThat(late.getWalletBalance()).isEqualTo(1000);
    }
}
