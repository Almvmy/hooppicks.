package com.hooppicks.backendapplication.live;

import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.espn.EspnLiveGame;
import com.hooppicks.backendapplication.espn.EspnLiveSummary;
import com.hooppicks.backendapplication.espn.EspnStatsClient;
import com.hooppicks.backendapplication.espn.PlayerBoxScoreRow;
import com.hooppicks.backendapplication.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LiveMatchServiceTest {

    private final EspnStatsClient espn = mock(EspnStatsClient.class);
    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private Instant now = Instant.parse("2026-10-09T13:00:00Z");
    private final Clock clock = new Clock() {
        public ZoneOffset getZone() { return ZoneOffset.UTC; }
        public Clock withZone(java.time.ZoneId zone) { return Clock.fixed(now, zone); }
        public Instant instant() { return now; }
    };
    private final LiveMatchService service = new LiveMatchService(espn, matchRepository, clock);

    private final Match match = new Match();

    @BeforeEach
    void setUp() {
        match.setId("m1");
        match.setEspnEventId("e1");
        when(matchRepository.findById("m1")).thenReturn(Optional.of(match));
        when(matchRepository.findByEspnEventIdIn(any())).thenReturn(List.of(match));
    }

    private static EspnLiveSummary summary(int homeScore) {
        return summary("in", homeScore);
    }

    private static EspnLiveSummary summary(String state, int homeScore) {
        EspnLiveGame status = new EspnLiveGame("e1", state, 3, "9:29", "9:29 - 3rd Quarter", homeScore, 69);
        PlayerBoxScoreRow flagg = new PlayerBoxScoreRow("Cooper Flagg", "DAL", true, "24", 19, 6, 3, 1, 1, 2, -15,
                new PlayerBoxScoreRow.ShotSplit(7, 14), new PlayerBoxScoreRow.ShotSplit(1, 5), new PlayerBoxScoreRow.ShotSplit(4, 4));
        return new EspnLiveSummary(status,
                new EspnLiveSummary.Side("DAL", List.of(19, 26, 9), Map.of("totalRebounds", "24")),
                new EspnLiveSummary.Side("HOU", List.of(27, 38, 4), Map.of("totalRebounds", "30")),
                List.of(flagg));
    }

    @Test
    void detail_du_direct_lu_chez_espn() {
        when(espn.fetchLiveSummary("e1")).thenReturn(Optional.of(summary(54)));

        LiveMatchService.LiveMatchDto live = service.detail("m1").orElseThrow();

        assertThat(live.status().clock()).isEqualTo("9:29");
        assertThat(live.status().homeScore()).isEqualTo(54);
        assertThat(live.homeLinescores()).containsExactly(19, 26, 9);
        assertThat(live.teamStats()).extracting(LiveMatchService.TeamStatLine::label).containsExactly("Rebonds");
        assertThat(live.players()).extracting(p -> p.playerName()).containsExactly("Cooper Flagg");
    }

    @Test
    void un_seul_appel_espn_par_match_tant_que_la_reponse_est_fraiche() {
        when(espn.fetchLiveSummary("e1")).thenReturn(Optional.of(summary(54)));

        service.detail("m1");
        now = now.plusSeconds(10);
        service.detail("m1");

        verify(espn, times(1)).fetchLiveSummary("e1");
    }

    @Test
    void espn_en_panne_la_derniere_reponse_sert_deux_minutes_puis_plus_rien() {
        when(espn.fetchLiveSummary("e1")).thenReturn(Optional.of(summary(54)), Optional.empty());
        service.detail("m1");

        // Panne courte : on garde le dernier état connu.
        now = now.plus(Duration.ofSeconds(90));
        assertThat(service.detail("m1")).map(l -> l.status().homeScore()).contains(54);

        // Panne qui dure : plus de direct, l'app retombe sur le score de la synchro.
        now = now.plus(Duration.ofSeconds(60));
        assertThat(service.detail("m1")).isEmpty();
    }

    @Test
    void match_termine_consultable_apres_coup_sans_rappeler_espn_a_chaque_visite() {
        when(espn.fetchLiveSummary("e1")).thenReturn(Optional.of(summary("post", 101)));

        service.detail("m1");
        now = now.plus(Duration.ofMinutes(5));
        assertThat(service.detail("m1")).map(l -> l.status().state()).contains("post");

        verify(espn, times(1)).fetchLiveSummary("e1");
    }

    @Test
    void match_pas_relie_a_espn_pas_de_direct() {
        match.setEspnEventId(null);
        assertThat(service.detail("m1")).isEmpty();
        verifyNoInteractions(espn);
    }

    @Test
    void la_liste_ne_garde_que_les_matchs_commences() {
        when(espn.fetchLiveScoreboard(any())).thenReturn(Optional.of(List.of(
                new EspnLiveGame("e1", "in", 3, "9:29", "", 54, 69),
                new EspnLiveGame("e2", "pre", 0, "0.0", "", 0, 0))));

        assertThat(service.board()).extracting(LiveMatchService.LiveStatusDto::matchId).containsExactly("m1");
    }

    @Test
    void liste_vide_si_espn_ne_repond_pas() {
        when(espn.fetchLiveScoreboard(any())).thenReturn(Optional.empty());
        assertThat(service.board()).isEmpty();
    }
}
