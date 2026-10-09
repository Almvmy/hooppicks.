package com.hooppicks.backendapplication.live;

import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.espn.EspnFormGame;
import com.hooppicks.backendapplication.espn.EspnPastGame;
import com.hooppicks.backendapplication.espn.EspnStatsClient;
import com.hooppicks.backendapplication.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MatchPreviewServiceTest {

    private final EspnStatsClient espn = mock(EspnStatsClient.class);
    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC);
    private final MatchPreviewService service = new MatchPreviewService(espn, matchRepository, clock);

    private final Team bos = team("BOS");
    private final Team nyk = team("NYK");
    private final Match match = new Match();

    private static Team team(String abbr) {
        Team t = new Team();
        t.setAbbreviation(abbr);
        return t;
    }

    private static EspnPastGame game(String date, String home, String away, int hs, int as) {
        return new EspnPastGame(Instant.parse(date), home, away, hs, as, false);
    }

    @BeforeEach
    void setUp() {
        match.setId("m1");
        match.setHomeTeam(bos);
        match.setAwayTeam(nyk);
        match.setEspnEventId("e1");
        match.setDate(Instant.parse("2026-10-23T23:00:00Z"));
        when(matchRepository.findById("m1")).thenReturn(Optional.of(match));
        when(matchRepository.findFinishedMeetings(bos, nyk)).thenReturn(List.of());
        when(espn.fetchTeamResults(anyString(), anyInt(), anyInt())).thenReturn(Optional.of(List.of()));
    }

    @Test
    void forme_des_deux_equipes_lue_dans_le_resume_espn() {
        when(espn.fetchForm("e1")).thenReturn(Optional.of(Map.of(
                "BOS", List.of(new EspnFormGame(Instant.parse("2026-10-08T23:00:00Z"), "CLE", false, 124, 113, true)),
                "NYK", List.of())));

        MatchPreviewService.PreviewDto p = service.preview("m1").orElseThrow();

        assertThat(p.homeForm()).singleElement().satisfies(g -> assertThat(g.won()).isTrue());
        assertThat(p.awayForm()).isEmpty();
    }

    @Test
    void face_a_face_saison_passee_seulement_contre_cet_adversaire_et_plus_recent_d_abord() {
        when(espn.fetchTeamResults("BOS", 2026, 2)).thenReturn(Optional.of(List.of(
                game("2025-11-01T23:00:00Z", "BOS", "NYK", 110, 100),
                game("2025-12-25T17:00:00Z", "NYK", "BOS", 99, 104),
                game("2026-01-10T00:00:00Z", "BOS", "MIA", 120, 90))));

        List<MatchPreviewService.H2HGame> h2h = service.preview("m1").orElseThrow().headToHead();

        assertThat(h2h).extracting(MatchPreviewService.H2HGame::awayAbbreviation).containsExactly("BOS", "NYK");
        // Deux confrontations seulement la saison passée : on va chercher celle d'avant.
        verify(espn).fetchTeamResults("BOS", 2025, 2);
    }

    @Test
    void notre_base_et_espn_ne_comptent_pas_deux_fois_le_meme_match() {
        Match ours = new Match();
        ours.setId("old");
        ours.setHomeTeam(bos);
        ours.setAwayTeam(nyk);
        ours.setDate(Instant.parse("2026-05-02T23:30:00Z"));
        ours.setHomeScore(100);
        ours.setAwayScore(109);
        when(matchRepository.findFinishedMeetings(bos, nyk)).thenReturn(List.of(ours));
        when(espn.fetchTeamResults("BOS", 2026, 3)).thenReturn(Optional.of(List.of(
                new EspnPastGame(Instant.parse("2026-05-02T23:30:00Z"), "BOS", "NYK", 100, 109, true))));

        assertThat(service.preview("m1").orElseThrow().headToHead()).hasSize(1);
    }

    @Test
    void espn_injoignable_forme_vide_et_face_a_face_de_notre_base() {
        when(espn.fetchForm(any())).thenReturn(Optional.empty());
        when(espn.fetchTeamResults(anyString(), anyInt(), anyInt())).thenReturn(Optional.empty());

        MatchPreviewService.PreviewDto p = service.preview("m1").orElseThrow();

        assertThat(p.homeForm()).isEmpty();
        assertThat(p.headToHead()).isEmpty();
    }

    @Test
    void le_calendrier_espn_d_une_equipe_n_est_lu_qu_une_fois_par_jour() {
        service.preview("m1");
        service.preview("m1");
        verify(espn, times(1)).fetchTeamResults("BOS", 2026, 2);
    }

    @Test
    void annee_de_fin_de_saison() {
        assertThat(MatchPreviewService.seasonEndYear(Instant.parse("2026-10-23T00:00:00Z"))).isEqualTo(2027);
        assertThat(MatchPreviewService.seasonEndYear(Instant.parse("2027-04-10T00:00:00Z"))).isEqualTo(2027);
    }
}
