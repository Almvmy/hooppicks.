package com.hooppicks.backendapplication.live;

import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.push.PushService;
import com.hooppicks.backendapplication.repository.MatchFollowRepository;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.NotificationRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MatchFollowServiceTest {

    private final MatchFollowRepository followRepository = mock(MatchFollowRepository.class);
    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final PushService pushService = mock(PushService.class);
    private final LiveMatchService liveMatchService = mock(LiveMatchService.class);
    private final MatchFollowService service = new MatchFollowService(followRepository, matchRepository,
            userRepository, notificationRepository, pushService, liveMatchService);

    private static final Instant TIP_OFF = Instant.parse("2026-10-23T23:00:00Z");
    private final Match match = new Match();
    private final MatchFollow follow = new MatchFollow();

    private static Team team(String abbr, String name) {
        Team t = new Team();
        t.setAbbreviation(abbr);
        t.setName(name);
        return t;
    }

    @BeforeEach
    void setUp() {
        match.setId("m1");
        match.setHomeTeam(team("BOS", "Celtics"));
        match.setAwayTeam(team("NYK", "Knicks"));
        match.setDate(TIP_OFF);
        match.setStatus(MatchStatus.LIVE);
        follow.setUserId("u1");
        follow.setMatchId("m1");
        User user = new User();
        user.setId("u1");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(followRepository.findActive()).thenReturn(List.of(follow));
        when(matchRepository.findAllById(any())).thenReturn(List.of(match));
    }

    private void espn(String state, int period, String clock, String detail, int home, int away) {
        when(liveMatchService.board()).thenReturn(List.of(
                new LiveMatchService.LiveStatusDto("m1", state, period, clock, detail, home, away)));
    }

    private List<String> titles() {
        ArgumentCaptor<PushService.PushMessage> captor = ArgumentCaptor.forClass(PushService.PushMessage.class);
        verify(pushService, atLeast(0)).sendToUser(eq("u1"), captor.capture());
        return captor.getAllValues().stream().map(PushService.PushMessage::title).toList();
    }

    private static String eq(String s) {
        return org.mockito.ArgumentMatchers.eq(s);
    }

    @Test
    void coup_d_envoi_puis_fin_de_quart_temps_une_seule_fois_chacun() {
        espn("in", 1, "0.0", "End of 1st Quarter", 19, 27);
        service.notifyFollowers(TIP_OFF.plus(Duration.ofMinutes(25)));
        service.notifyFollowers(TIP_OFF.plus(Duration.ofMinutes(26)));

        assertThat(titles()).containsExactly("C'est parti !", "Fin du 1er quart-temps");
        assertThat(follow.getLastPeriodNotified()).isEqualTo(1);
    }

    @Test
    void mi_temps() {
        follow.setKickoffNotified(true);
        follow.setLastPeriodNotified(1);
        espn("in", 2, "0.0", "Halftime", 45, 65);
        service.notifyFollowers(TIP_OFF.plus(Duration.ofHours(1)));
        assertThat(titles()).containsExactly("Mi-temps");
    }

    @Test
    void fin_de_match_serree() {
        follow.setKickoffNotified(true);
        follow.setLastPeriodNotified(3);
        espn("in", 4, "1:45", "1:45 - 4th Quarter", 101, 99);
        service.notifyFollowers(TIP_OFF.plus(Duration.ofHours(2)));
        assertThat(titles()).containsExactly("Fin de match serrée");
    }

    @Test
    void pas_de_fin_serree_avec_un_gros_ecart() {
        follow.setKickoffNotified(true);
        follow.setLastPeriodNotified(3);
        espn("in", 4, "1:45", "", 120, 99);
        service.notifyFollowers(TIP_OFF.plus(Duration.ofHours(2)));
        assertThat(titles()).isEmpty();
    }

    @Test
    void resultat_final_meme_si_espn_ne_repond_plus() {
        follow.setKickoffNotified(true);
        match.setStatus(MatchStatus.FINISHED);
        match.setHomeScore(113);
        match.setAwayScore(124);
        when(liveMatchService.board()).thenReturn(List.of());

        service.notifyFollowers(TIP_OFF.plus(Duration.ofHours(3)));

        assertThat(titles()).containsExactly("Terminé");
        assertThat(follow.getFinalNotified()).isTrue();
        ArgumentCaptor<AppNotification> notif = ArgumentCaptor.forClass(AppNotification.class);
        verify(notificationRepository).save(notif.capture());
        assertThat(notif.getValue().getMessage()).isEqualTo("Terminé · NYK 124 - 113 BOS");
    }

    @Test
    void match_reporte_abandonne_sans_notification() {
        match.setStatus(MatchStatus.SCHEDULED);
        when(liveMatchService.board()).thenReturn(List.of());

        service.notifyFollowers(TIP_OFF.plus(Duration.ofHours(13)));

        assertThat(titles()).isEmpty();
        assertThat(follow.getFinalNotified()).isTrue();
    }

    @Test
    void suivre_un_match_deja_commence_ne_dit_pas_c_est_parti() {
        when(matchRepository.findById("m1")).thenReturn(Optional.of(match));
        when(followRepository.findByUserIdAndMatchId("u1", "m1")).thenReturn(Optional.empty());

        service.follow("u1", "m1");

        ArgumentCaptor<MatchFollow> saved = ArgumentCaptor.forClass(MatchFollow.class);
        verify(followRepository).save(saved.capture());
        assertThat(saved.getValue().getKickoffNotified()).isTrue();
    }

    @Test
    void on_ne_suit_pas_un_match_termine() {
        match.setStatus(MatchStatus.FINISHED);
        when(matchRepository.findById("m1")).thenReturn(Optional.of(match));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.follow("u1", "m1"))
                .isInstanceOf(MatchFollowService.FollowException.class);
    }
}
