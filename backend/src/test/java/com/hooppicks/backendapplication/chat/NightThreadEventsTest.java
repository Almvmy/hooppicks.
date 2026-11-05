package com.hooppicks.backendapplication.chat;

import com.hooppicks.backendapplication.dto.PlayerBoxScoreDto;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.live.LiveMatchService.LiveStatusDto;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.NightMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NightThreadEventsTest {

    private static final Instant NOW = Instant.parse("2026-11-06T23:30:00Z");

    private final NightMessageRepository messages = mock(NightMessageRepository.class);
    private final MatchRepository matches = mock(MatchRepository.class);
    private final BetRepository bets = mock(BetRepository.class);
    private final NightThreadEvents events = new NightThreadEvents(messages, matches, bets);

    @BeforeEach
    void setUp() {
        Team home = new Team();
        home.setAbbreviation("CLE");
        Team away = new Team();
        away.setAbbreviation("BKN");
        Match m = new Match();
        m.setId("x");
        m.setHomeTeam(home);
        m.setAwayTeam(away);
        m.setDate(NOW.minusSeconds(3600));
        when(matches.findById("x")).thenReturn(Optional.of(m));
        when(bets.findWonSince(any())).thenReturn(List.of());
    }

    private static LiveStatusDto live(String state, int period, String clock, String detail) {
        return new LiveStatusDto("x", state, period, clock, detail, 88, 85);
    }

    private static PlayerBoxScoreDto player(String name, int points) {
        return new PlayerBoxScoreDto(name, "CLE", true, "30", points, 0, 0, 0, 0, 0, 0, "0-0", "0-0", "0-0");
    }

    private String postedText() {
        ArgumentCaptor<NightMessage> saved = ArgumentCaptor.forClass(NightMessage.class);
        verify(messages).save(saved.capture());
        return saved.getValue().getText();
    }

    @Test
    void fin_de_quart_temps() {
        assertThat(events.record(List.of(live("in", 3, "0.0", "End of 3rd")), Map.of(), NOW)).isEqualTo(1);
        assertThat(postedText()).isEqualTo("Fin du 3e quart-temps : BKN 85 - 88 CLE");
    }

    @Test
    void mi_temps_et_resultat() {
        events.record(List.of(live("in", 2, "0.0", "Halftime")), Map.of(), NOW);
        assertThat(postedText()).isEqualTo("Mi-temps : BKN 85 - 88 CLE");

        clearInvocations(messages);
        events.record(List.of(live("post", 4, "0.0", "Final")), Map.of(), NOW);
        assertThat(postedText()).isEqualTo("Terminé : BKN 85 - 88 CLE");
    }

    @Test
    void rien_pendant_le_jeu() {
        assertThat(events.record(List.of(live("in", 3, "5:42", "5:42 - 3rd")), Map.of(), NOW)).isZero();
        verify(messages, never()).save(any());
    }

    @Test
    void un_match_de_la_veille_encore_dans_le_direct_n_entre_pas_dans_le_fil() {
        matches.findById("x").orElseThrow().setDate(Instant.parse("2026-11-06T02:00:00Z"));

        assertThat(events.record(List.of(live("post", 4, "0.0", "Final")), Map.of(), NOW)).isZero();
        verify(messages, never()).save(any());
    }

    @Test
    void jamais_deux_fois_le_meme_message() {
        when(messages.existsById("auto:period:x:3")).thenReturn(true);

        assertThat(events.record(List.of(live("in", 3, "0.0", "End of 3rd")), Map.of(), NOW)).isZero();
        verify(messages, never()).save(any());
    }

    @Test
    void grosse_perf_au_palier_le_plus_haut_seulement() {
        Map<String, List<PlayerBoxScoreDto>> boxes = Map.of("x", List.of(player("Victor Wembanyama", 42), player("Remplaçant", 12)));

        assertThat(events.record(List.of(live("in", 3, "5:42", "")), boxes, NOW)).isEqualTo(1);
        assertThat(postedText()).isEqualTo("Victor Wembanyama passe les 40 pts");
    }

    @Test
    void gros_gain_annonce_petit_gain_ignore() {
        User kader = new User();
        kader.setUsername("Kader");
        Bet big = new Bet();
        big.setId("b1");
        big.setUser(kader);
        big.setStake(100);
        big.setPotentialPayout(450);
        Bet small = new Bet();
        small.setId("b2");
        small.setUser(kader);
        small.setStake(100);
        small.setPotentialPayout(180);
        when(bets.findWonSince(any())).thenReturn(List.of(big, small));

        assertThat(events.record(List.of(), Map.of(), NOW)).isEqualTo(1);
        assertThat(postedText()).isEqualTo("Kader encaisse +350 pts");
    }
}
