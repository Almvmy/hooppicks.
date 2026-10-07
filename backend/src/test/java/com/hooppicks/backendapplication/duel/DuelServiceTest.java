package com.hooppicks.backendapplication.duel;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.push.PushService;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.DuelRepository;
import com.hooppicks.backendapplication.repository.NotificationRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DuelServiceTest {

    // Mercredi 7 octobre 2026, 15h GMT : semaine de jeu du lundi 5 octobre.
    private static final Instant NOW = Instant.parse("2026-10-07T15:00:00Z");
    private static final LocalDate WEEK = LocalDate.of(2026, 10, 5);

    private final DuelRepository duelRepository = mock(DuelRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final BetRepository betRepository = mock(BetRepository.class);
    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final PushService pushService = mock(PushService.class);
    private DuelService service;

    private final User alice = user("a", "Alice");
    private final User bob = user("b", "Bob");

    private static User user(String id, String name) {
        User u = new User();
        u.setId(id);
        u.setUsername(name);
        return u;
    }

    @BeforeEach
    void setUp() {
        service = new DuelService(duelRepository, userRepository, betRepository, notificationRepository, pushService,
                Clock.fixed(NOW, BankrollService.ZONE));
        when(userRepository.findById("a")).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("Bob")).thenReturn(Optional.of(bob));
        when(userRepository.findByUsername("Alice")).thenReturn(Optional.of(alice));
    }

    private Duel duel(DuelStatus status, LocalDate week) {
        Duel d = new Duel();
        d.setId("d1");
        d.setChallenger(alice);
        d.setOpponent(bob);
        d.setWeek(week);
        d.setStatus(status);
        return d;
    }

    @Test
    void un_defi_porte_sur_la_semaine_en_cours_et_previent_l_adversaire() {
        Duel d = service.challenge("a", "Bob");

        assertThat(d.getWeek()).isEqualTo(WEEK);
        assertThat(d.getStatus()).isEqualTo(DuelStatus.PENDING);
        verify(duelRepository).save(d);
        verify(notificationRepository).save(argThat(n -> n.getUser() == bob && n.getType() == NotificationType.DUEL));
    }

    @Test
    void on_ne_se_defie_pas_soi_meme_ni_deux_fois_la_meme_semaine() {
        assertThatThrownBy(() -> service.challenge("a", "Alice")).isInstanceOf(DuelService.DuelException.class);

        when(duelRepository.existsBetween(eq("a"), eq("b"), eq(WEEK), any())).thenReturn(true);
        assertThatThrownBy(() -> service.challenge("a", "Bob")).hasMessageContaining("déjà");
        verify(duelRepository, never()).save(any());
    }

    @Test
    void seul_l_adversaire_peut_accepter() {
        when(duelRepository.findById("d1")).thenReturn(Optional.of(duel(DuelStatus.PENDING, WEEK)));

        assertThatThrownBy(() -> service.respond("a", "d1", true)).isInstanceOf(DuelService.DuelException.class);
        assertThat(service.respond("b", "d1", true).getStatus()).isEqualTo(DuelStatus.ACCEPTED);
    }

    @Test
    void cloture_vainqueur_et_points_figes_une_fois_les_tickets_regles() {
        Duel d = duel(DuelStatus.ACCEPTED, WEEK.minusWeeks(1));
        when(duelRepository.findByStatusAndWeekBefore(DuelStatus.PENDING, WEEK)).thenReturn(List.of());
        when(duelRepository.findByStatusAndWeekBefore(DuelStatus.ACCEPTED, WEEK)).thenReturn(List.of(d));
        when(betRepository.countPendingPlacedBetween(any(), any(), any())).thenReturn(0L);
        when(betRepository.getWeekScores(eq(WEEK.minusWeeks(1).atStartOfDay()), any()))
                .thenReturn(List.<Object[]>of(new Object[]{"a", 120L}, new Object[]{"b", 300L}));

        assertThat(service.settleEndedWeeks()).isEqualTo(1);

        assertThat(d.getStatus()).isEqualTo(DuelStatus.FINISHED);
        assertThat(d.getWinnerId()).isEqualTo("b");
        assertThat(d.getChallengerPoints()).isEqualTo(120);
        verify(notificationRepository, times(2)).save(any());
    }

    @Test
    void un_ticket_encore_en_attente_retarde_la_cloture() {
        Duel d = duel(DuelStatus.ACCEPTED, WEEK.minusWeeks(1));
        when(duelRepository.findByStatusAndWeekBefore(DuelStatus.PENDING, WEEK)).thenReturn(List.of());
        when(duelRepository.findByStatusAndWeekBefore(DuelStatus.ACCEPTED, WEEK)).thenReturn(List.of(d));
        when(betRepository.countPendingPlacedBetween(any(), any(), any())).thenReturn(1L);

        // Semaine finie lundi 5 oct. 12h, on est le 7 : délai de 24 h dépassé, on clôt quand même.
        assertThat(service.settleEndedWeeks()).isEqualTo(1);

        // Mais un lundi à 13h, une heure après la fin, on attend.
        DuelService monday = new DuelService(duelRepository, userRepository, betRepository, notificationRepository,
                pushService, Clock.fixed(Instant.parse("2026-10-05T13:00:00Z"), BankrollService.ZONE));
        Duel d2 = duel(DuelStatus.ACCEPTED, WEEK.minusWeeks(1));
        when(duelRepository.findByStatusAndWeekBefore(DuelStatus.ACCEPTED, WEEK)).thenReturn(List.of(d2));
        assertThat(monday.settleEndedWeeks()).isZero();
        assertThat(d2.getStatus()).isEqualTo(DuelStatus.ACCEPTED);
    }

    @Test
    void un_defi_sans_reponse_expire_avec_la_semaine() {
        Duel d = duel(DuelStatus.PENDING, WEEK.minusWeeks(1));
        when(duelRepository.findByStatusAndWeekBefore(DuelStatus.PENDING, WEEK)).thenReturn(List.of(d));
        when(duelRepository.findByStatusAndWeekBefore(DuelStatus.ACCEPTED, WEEK)).thenReturn(List.of());

        service.settleEndedWeeks();

        assertThat(d.getStatus()).isEqualTo(DuelStatus.EXPIRED);
    }
}
