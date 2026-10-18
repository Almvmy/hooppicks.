package com.hooppicks.backendapplication.season;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SeasonPickServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-07T15:00:00Z");

    private final SeasonPickRepository pickRepository = mock(SeasonPickRepository.class);
    private final SeasonResultRepository resultRepository = mock(SeasonResultRepository.class);
    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SeasonMilestoneRepository milestoneRepository = mock(SeasonMilestoneRepository.class);
    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final com.hooppicks.backendapplication.push.PushService pushService =
            mock(com.hooppicks.backendapplication.push.PushService.class);
    private SeasonPickService service;

    @BeforeEach
    void setUp() {
        service = at(NOW);
        Team bos = new Team();
        bos.setAbbreviation("BOS");
        when(teamRepository.findAll()).thenReturn(List.of(bos));
        when(pickRepository.findBySeasonAndUserIdAndQuestion(any(), any(), any())).thenReturn(Optional.empty());
    }

    private SeasonPickService at(Instant now) {
        return new SeasonPickService(pickRepository, resultRepository, matchRepository, teamRepository, userRepository,
                milestoneRepository, notificationRepository, pushService, Clock.fixed(now, BankrollService.ZONE));
    }

    private static SeasonPick pick(String user, String question, String team) {
        SeasonPick p = new SeasonPick();
        p.setUserId(user);
        p.setSeason("2026-27");
        p.setQuestion(question);
        p.setTeamAbbreviation(team);
        return p;
    }

    private static User user(String id) {
        User u = new User();
        u.setId(id);
        u.setUsername(id);
        return u;
    }

    private void seasonStartsAt(Instant opener) {
        Match m = new Match();
        m.setDate(opener);
        when(matchRepository.findFirstByTypeAndDateAfterOrderByDateAsc(eq(MatchType.REGULAR), any())).thenReturn(Optional.of(m));
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }

    @Test
    void la_saison_courante_bascule_en_septembre() {
        assertThat(service.currentSeason()).isEqualTo("2026-27");
    }

    @Test
    void on_peut_pronostiquer_avant_le_premier_match_de_saison_reguliere() {
        seasonStartsAt(Instant.parse("2026-10-21T23:30:00Z"));
        service.pick("u1", "champion", "BOS");
        verify(pickRepository).save(argThat(p -> p.getSeason().equals("2026-27") && p.getTeamAbbreviation().equals("BOS")));
    }

    @Test
    void encore_ouvert_pendant_la_premiere_semaine_de_saison() {
        // Premier match le 1er octobre, on est le 7 : encore un jour.
        seasonStartsAt(Instant.parse("2026-10-01T23:30:00Z"));
        service.pick("u1", "champion", "BOS");
        verify(pickRepository).save(any());
        assertThat(service.deadline("2026-27")).isEqualTo(Instant.parse("2026-10-08T23:30:00Z"));
    }

    @Test
    void verrouille_une_fois_la_saison_commencee_et_refuse_l_inconnu() {
        // Saison commencée le 28 septembre : la semaine de rab est finie le 5 octobre.
        seasonStartsAt(Instant.parse("2026-09-28T23:30:00Z"));
        assertThatThrownBy(() -> service.pick("u1", "champion", "BOS")).hasMessageContaining("clos");

        seasonStartsAt(Instant.parse("2026-10-21T23:30:00Z"));
        assertThatThrownBy(() -> service.pick("u1", "mvp", "BOS")).hasMessageContaining("Question");
        assertThatThrownBy(() -> service.pick("u1", "champion", "XYZ")).hasMessageContaining("Équipe");
        verify(pickRepository, never()).save(any());
    }

    @Test
    void classement_des_bonnes_reponses_et_repartition_cachee_avant_le_verrou() {
        seasonStartsAt(Instant.parse("2026-10-21T23:30:00Z"));
        SeasonPick p = new SeasonPick();
        p.setUserId("u1");
        p.setSeason("2026-27");
        p.setQuestion("champion");
        p.setTeamAbbreviation("BOS");
        when(pickRepository.findBySeason("2026-27")).thenReturn(List.of(p));
        SeasonResult r = new SeasonResult();
        r.setQuestion("champion");
        r.setTeamAbbreviation("BOS");
        when(resultRepository.findBySeason("2026-27")).thenReturn(List.of(r));
        User u = new User();
        u.setId("u1");
        u.setUsername("Alice");
        when(userRepository.findAllById(any())).thenReturn(List.of(u));

        SeasonPickService.Overview o = service.overview("u1");

        assertThat(o.locked()).isFalse();
        assertThat(o.community()).isEmpty();
        assertThat(o.myPicks()).containsEntry("champion", "BOS");
        assertThat(o.leaderboard()).singleElement().satisfies(s -> {
            assertThat(s.username()).isEqualTo("Alice");
            assertThat(s.points()).isEqualTo(300);
        });
    }

    @Test
    void rappel_la_veille_du_verrou_seulement_a_qui_n_a_pas_fini() {
        // Verrou le 8 octobre à 9h : on est le 7 à 15h, dans les 24 h.
        seasonStartsAt(Instant.parse("2026-10-01T09:00:00Z"));
        List<SeasonPick> picks = new java.util.ArrayList<>();
        for (SeasonPickService.Question q : SeasonPickService.QUESTIONS) picks.add(pick("fini", q.key(), "BOS"));
        picks.add(pick("moitie", "champion", "BOS"));
        when(pickRepository.findBySeason("2026-27")).thenReturn(picks);
        when(userRepository.findAll()).thenReturn(List.of(user("fini"), user("moitie"), user("rien")));
        when(milestoneRepository.findById("2026-27")).thenReturn(Optional.empty());

        service.runMilestones();

        verify(notificationRepository, times(2)).save(any());
        verify(pushService, never()).sendToUser(eq("fini"), any());
        verify(pushService).sendToUser(eq("moitie"), argThat(m -> m.body().contains("il t'en manque 3")));
        verify(milestoneRepository).save(argThat(m -> m.getReminderSentAt() != null && m.getRevealedAt() == null));
    }

    @Test
    void rien_avant_la_veille_du_verrou() {
        seasonStartsAt(Instant.parse("2026-10-21T23:30:00Z"));
        service.runMilestones();
        verifyNoInteractions(notificationRepository, milestoneRepository);
    }

    @Test
    void revelation_au_verrou_une_seule_fois_et_sans_rappel_tardif() {
        seasonStartsAt(Instant.parse("2026-09-28T23:30:00Z")); // verrou le 5 octobre
        Team bos = new Team();
        bos.setAbbreviation("BOS");
        bos.setName("Celtics");
        when(teamRepository.findAll()).thenReturn(List.of(bos));
        when(pickRepository.findBySeason("2026-27")).thenReturn(List.of(
                pick("a", "champion", "BOS"), pick("b", "champion", "BOS"), pick("c", "champion", "OKC")));
        when(userRepository.findAllById(any())).thenReturn(List.of(user("a"), user("b"), user("c")));
        when(milestoneRepository.findById("2026-27")).thenReturn(Optional.empty());

        service.runMilestones();

        verify(userRepository, never()).findAll();
        verify(pushService).sendToUser(eq("a"), argThat(m -> m.body().contains("Celtics (67 %)") && m.body().contains("majorité")));
        verify(pushService).sendToUser(eq("c"), argThat(m -> m.body().contains("33 %")));

        SeasonMilestone done = new SeasonMilestone();
        done.setSeason("2026-27");
        done.setRevealedAt(NOW);
        when(milestoneRepository.findById("2026-27")).thenReturn(Optional.of(done));
        clearInvocations(pushService);
        service.runMilestones();
        verifyNoInteractions(pushService);
    }

    @Test
    void visiteur_sans_compte_voit_la_page_sans_pronostics_a_lui() {
        seasonStartsAt(Instant.parse("2026-10-21T23:30:00Z"));
        when(pickRepository.findBySeason("2026-27")).thenReturn(List.of(pick("a", "champion", "BOS"), pick("a", "east_finalist", "BOS")));

        SeasonPickService.Overview o = service.overview(null);

        assertThat(o.myPicks()).isEmpty();
        assertThat(o.participants()).isEqualTo(1);
    }
}
