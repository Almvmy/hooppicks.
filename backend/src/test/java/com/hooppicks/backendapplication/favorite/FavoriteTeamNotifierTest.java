package com.hooppicks.backendapplication.favorite;

import com.hooppicks.backendapplication.entity.AppNotification;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.entity.NotificationType;
import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.push.PushService;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.NotificationRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FavoriteTeamNotifierTest {

    @Mock private MatchRepository matchRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private PushService pushService;

    private FavoriteTeamNotifier notifier;

    private static final Instant NOW = Instant.parse("2026-10-07T22:30:00Z");
    private final Team knicks = team("NYK", "Knicks");
    private final Team heat = team("MIA", "Heat");

    @BeforeEach
    void setUp() {
        notifier = new FavoriteTeamNotifier(matchRepository, userRepository, notificationRepository, pushService);
        when(userRepository.findByFavoriteTeamEndingWith(any())).thenReturn(List.of());
    }

    private static Team team(String abbr, String name) {
        Team t = new Team();
        t.setAbbreviation(abbr);
        t.setName(name);
        return t;
    }

    private static User fan(String id, boolean notify) {
        User u = new User();
        u.setId(id);
        u.setNotifyFavoriteTeam(notify);
        return u;
    }

    private Match match(MatchStatus status, Integer homeScore, Integer awayScore) {
        Match m = new Match();
        m.setId("m1");
        m.setHomeTeam(knicks);
        m.setAwayTeam(heat);
        m.setStatus(status);
        m.setHomeScore(homeScore);
        m.setAwayScore(awayScore);
        m.setDate(NOW.plusSeconds(1800));
        return m;
    }

    private List<String> sentMessages() {
        ArgumentCaptor<AppNotification> captor = ArgumentCaptor.forClass(AppNotification.class);
        verify(notificationRepository, atLeast(0)).save(captor.capture());
        return captor.getAllValues().stream().map(AppNotification::getMessage).toList();
    }

    @Test
    void coup_d_envoi_une_seule_fois_et_seulement_aux_fans_qui_le_veulent() {
        Match m = match(MatchStatus.SCHEDULED, null, null);
        when(matchRepository.findByStatusAndDateBetween(eq(MatchStatus.SCHEDULED), any(), any())).thenReturn(List.of(m));
        when(userRepository.findByFavoriteTeamEndingWith(" Knicks")).thenReturn(List.of(fan("u1", true), fan("u2", false)));
        when(userRepository.findByFavoriteTeamEndingWith(" Heat")).thenReturn(List.of(fan("u3", true)));

        int sent = notifier.notifyUpcomingKickoffs(NOW);

        assertThat(sent).isEqualTo(2);
        // Accord au singulier pour « le Heat ».
        assertThat(sentMessages()).containsExactlyInAnyOrder(
                "Les Knicks jouent dans moins d'une heure contre le Heat.",
                "Le Heat joue dans moins d'une heure contre les Knicks.");
        verify(pushService).sendToUser(eq("u1"), any());
        verify(pushService, never()).sendToUser(eq("u2"), any());
        assertThat(m.getFavoriteKickoffNotified()).isTrue();

        // Deuxième passage : déjà notifié, rien de plus.
        clearInvocations(notificationRepository, pushService);
        assertThat(notifier.notifyUpcomingKickoffs(NOW)).isZero();
        verifyNoInteractions(pushService);
    }

    @Test
    void resultat_victoire_et_defaite_avec_le_score_vu_de_chaque_equipe() {
        Match m = match(MatchStatus.FINISHED, 112, 104);
        when(matchRepository.findByStatusAndDateBetween(eq(MatchStatus.FINISHED), any(), any())).thenReturn(List.of(m));
        when(userRepository.findByFavoriteTeamEndingWith(" Knicks")).thenReturn(List.of(fan("u1", true)));
        when(userRepository.findByFavoriteTeamEndingWith(" Heat")).thenReturn(List.of(fan("u3", true)));

        notifier.notifyResults(NOW);

        assertThat(sentMessages()).containsExactlyInAnyOrder(
                "Victoire des Knicks contre le Heat (112-104).",
                "Défaite du Heat contre les Knicks (104-112).");
        ArgumentCaptor<AppNotification> captor = ArgumentCaptor.forClass(AppNotification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).allMatch(n -> n.getType() == NotificationType.FAVORITE_TEAM);
        assertThat(m.getFavoriteResultNotified()).isTrue();
    }

    @Test
    void resultat_sans_score_ignore_et_pas_marque() {
        Match m = match(MatchStatus.FINISHED, null, null);
        when(matchRepository.findByStatusAndDateBetween(eq(MatchStatus.FINISHED), any(), any())).thenReturn(List.of(m));

        assertThat(notifier.notifyResults(NOW)).isZero();
        assertThat(m.getFavoriteResultNotified()).isNull();
    }
}
