package com.hooppicks.backendapplication.engagement;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.push.PushService;
import com.hooppicks.backendapplication.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class WeeklyReminderServiceTest {

    private static final LocalDate WEEK = LocalDate.of(2026, 11, 2);
    private static final Instant SUNDAY_EVENING = Instant.parse("2026-11-08T19:00:00Z");
    private static final Instant MONDAY_AFTERNOON = Instant.parse("2026-11-02T13:00:00Z");

    private final WeeklyReminderRepository reminderRepository = mock(WeeklyReminderRepository.class);
    private final BetRepository betRepository = mock(BetRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final LeagueRepository leagueRepository = mock(LeagueRepository.class);
    private final LeagueMembershipRepository membershipRepository = mock(LeagueMembershipRepository.class);
    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final PushService pushService = mock(PushService.class);

    private WeeklyReminderService at(Instant now) {
        return new WeeklyReminderService(reminderRepository, betRepository, userRepository, matchRepository, leagueRepository,
                membershipRepository, notificationRepository, pushService, Clock.fixed(now, BankrollService.ZONE));
    }

    private static User user(String id, int balance) {
        User u = new User();
        u.setId(id);
        u.setWalletBalance(balance);
        u.setBankrollWeek(WEEK);
        return u;
    }

    private static LeagueMembership member(User u) {
        LeagueMembership m = new LeagueMembership();
        m.setUser(u);
        return m;
    }

    private void active(User... users) {
        when(betRepository.findUserIdsWithBetSince(any())).thenReturn(List.of("x"));
        when(userRepository.findAllById(any())).thenReturn(List.of(users));
    }

    @BeforeEach
    void setUp() {
        when(matchRepository.findByStatusAndDateBetween(any(), any(), any())).thenReturn(List.of(new Match()));
        when(leagueRepository.findAll()).thenReturn(List.of());
        when(betRepository.getAllWeekScores(any())).thenReturn(List.of());
    }

    @Test
    void dimanche_soir_le_budget_restant_avec_l_ecart_au_podium_de_la_ligue() {
        User me = user("me", 640);
        User a = user("a", 0), b = user("b", 0), c = user("c", 0);
        active(me);
        League league = new League();
        league.setId("l");
        league.setName("Les potes");
        when(leagueRepository.findAll()).thenReturn(List.of(league));
        when(membershipRepository.findByLeagueId("l")).thenReturn(List.of(member(me), member(a), member(b), member(c)));
        when(betRepository.getAllWeekScores(WEEK.atStartOfDay())).thenReturn(List.of(
                new Object[]{"a", 300L}, new Object[]{"b", 200L}, new Object[]{"c", 100L}, new Object[]{"me", 60L}));

        assertThat(at(SUNDAY_EVENING).run()).isEqualTo(1);

        ArgumentCaptor<AppNotification> saved = ArgumentCaptor.forClass(AppNotification.class);
        verify(notificationRepository).save(saved.capture());
        assertThat(saved.getValue().getMessage())
                .isEqualTo("Il te reste 640 pts à miser avant lundi 12h GMT. Tu es à 40 pts du podium de « Les potes ».");
        assertThat(saved.getValue().getType()).isEqualTo(NotificationType.WEEKLY_REMINDER);
        verify(pushService).sendToUser(anyString(), any());
    }

    @Test
    void pas_de_rappel_sans_budget_ni_sans_match_ni_si_le_joueur_l_a_coupe() {
        User broke = user("broke", 50);
        User off = user("off", 900);
        off.setNotifyWeeklyReminders(false);
        active(broke, off);
        assertThat(at(SUNDAY_EVENING).run()).isZero();

        User rich = user("rich", 900);
        active(rich);
        when(matchRepository.findByStatusAndDateBetween(any(), any(), any())).thenReturn(List.of());
        assertThat(at(SUNDAY_EVENING).run()).isZero();
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void solde_d_une_semaine_passee_compte_pour_le_budget_entier() {
        User idle = user("idle", 10);
        idle.setBankrollWeek(WEEK.minusWeeks(1));
        active(idle);

        assertThat(at(SUNDAY_EVENING).run()).isEqualTo(1);

        ArgumentCaptor<AppNotification> saved = ArgumentCaptor.forClass(AppNotification.class);
        verify(notificationRepository).save(saved.capture());
        assertThat(saved.getValue().getMessage()).startsWith("Il te reste 1000 pts");
    }

    @Test
    void lundi_la_nouvelle_semaine_en_push_seulement() {
        active(user("me", 1000));

        assertThat(at(MONDAY_AFTERNOON).run()).isEqualTo(1);

        verify(pushService).sendToUser(anyString(), any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void une_seule_fois_et_jamais_hors_de_sa_fenetre() {
        active(user("me", 1000));
        when(reminderRepository.existsById(WEEK + ":MONDAY")).thenReturn(true);
        assertThat(at(MONDAY_AFTERNOON).run()).isZero();

        // Mercredi : ni l'un ni l'autre, et rien n'est noté comme envoyé.
        assertThat(at(Instant.parse("2026-11-04T15:00:00Z")).run()).isZero();
        verify(pushService, never()).sendToUser(anyString(), any());
        verify(reminderRepository, never()).save(any());
    }
}
