package com.hooppicks.backendapplication.palmares;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.push.PushService;
import com.hooppicks.backendapplication.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PalmaresServiceTest {

    // Mercredi 7 octobre 2026 : semaine en cours du 5, dernière terminée du 28 septembre.
    private static final Instant NOW = Instant.parse("2026-10-07T15:00:00Z");
    private static final LocalDate LAST = LocalDate.of(2026, 9, 28);

    private final BetRepository betRepository = mock(BetRepository.class);
    private final WeeklyTitleRepository titleRepository = mock(WeeklyTitleRepository.class);
    private final PalmaresWeekRepository weekRepository = mock(PalmaresWeekRepository.class);
    private final LeagueRepository leagueRepository = mock(LeagueRepository.class);
    private final LeagueMembershipRepository membershipRepository = mock(LeagueMembershipRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private PalmaresService service;

    @BeforeEach
    void setUp() {
        service = new PalmaresService(betRepository, titleRepository, weekRepository, leagueRepository,
                membershipRepository, userRepository, notificationRepository, mock(DuelRepository.class),
                mock(PushService.class), Clock.fixed(NOW, BankrollService.ZONE));
        when(userRepository.findById(any())).thenAnswer(inv -> {
            User u = new User();
            u.setId(inv.getArgument(0));
            return Optional.of(u);
        });
    }

    private LeagueMembership member(League league, String userId) {
        User u = new User();
        u.setId(userId);
        LeagueMembership m = new LeagueMembership();
        m.setLeague(league);
        m.setUser(u);
        return m;
    }

    @Test
    void champion_general_et_de_ligue_pour_la_derniere_semaine_avec_notification() {
        when(betRepository.findSettledWeeks()).thenReturn(List.of(LAST.atStartOfDay(), LocalDate.of(2026, 10, 5).atStartOfDay()));
        when(betRepository.countAllPendingPlacedBetween(any(), any())).thenReturn(0L);
        when(betRepository.getAllWeekScores(LAST.atStartOfDay())).thenReturn(List.<Object[]>of(
                new Object[]{"a", 500L}, new Object[]{"b", 200L}, new Object[]{"c", 120L}));
        League league = new League();
        league.setId("l1");
        league.setName("Les potes");
        when(leagueRepository.findAll()).thenReturn(List.of(league));
        when(membershipRepository.findByLeagueId("l1")).thenReturn(List.of(member(league, "b"), member(league, "c")));

        assertThat(service.recordEndedWeeks()).isEqualTo(1); // la semaine en cours n'est pas attribuée

        ArgumentCaptor<WeeklyTitle> titles = ArgumentCaptor.forClass(WeeklyTitle.class);
        verify(titleRepository, times(2)).save(titles.capture());
        assertThat(titles.getAllValues()).extracting(WeeklyTitle::getUserId, WeeklyTitle::getLeagueName)
                .containsExactlyInAnyOrder(org.assertj.core.groups.Tuple.tuple("a", null), org.assertj.core.groups.Tuple.tuple("b", "Les potes"));
        verify(notificationRepository, times(2)).save(any());
        verify(weekRepository).save(any());
    }

    @Test
    void une_semaine_deja_attribuee_ou_sans_points_ne_donne_rien() {
        when(betRepository.findSettledWeeks()).thenReturn(List.of(LAST.atStartOfDay(), LAST.minusWeeks(1).atStartOfDay()));
        when(weekRepository.existsById(LAST)).thenReturn(true);
        when(betRepository.getAllWeekScores(LAST.minusWeeks(1).atStartOfDay())).thenReturn(List.<Object[]>of(new Object[]{"a", 0L}));
        when(leagueRepository.findAll()).thenReturn(List.of());

        service.recordEndedWeeks();

        verify(titleRepository, never()).save(any());
        // Semaine plus ancienne rattrapée : marquée, sans notification.
        verify(weekRepository).save(any());
        verify(notificationRepository, never()).save(any());
    }
}
