package com.hooppicks.backendapplication.season;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.Bracket;
import com.hooppicks.backendapplication.entity.SeasonResult;
import com.hooppicks.backendapplication.repository.BracketRepository;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.SeasonResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SeasonalTabsServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");

    private final MatchRepository matches = mock(MatchRepository.class);
    private final BracketRepository brackets = mock(BracketRepository.class);
    private final SeasonPickService picks = mock(SeasonPickService.class);
    private final SeasonResultRepository results = mock(SeasonResultRepository.class);
    private final SeasonalTabsService service = new SeasonalTabsService(matches, brackets, picks, results, Clock.fixed(NOW, BankrollService.ZONE));

    @BeforeEach
    void setUp() {
        when(picks.currentSeason()).thenReturn("2026-27");
        when(picks.deadline("2026-27")).thenReturn(NOW.minus(Duration.ofDays(10)));
        when(results.findBySeason("2026-27")).thenReturn(List.of());
        when(brackets.findTopByOrderByLocksAtDesc()).thenReturn(Optional.empty());
    }

    @Test
    void coupe_visible_autour_de_ses_matchs_et_cachee_le_reste_du_temps() {
        when(matches.existsByTypeInAndDateBetween(any(), any(), any())).thenReturn(true);
        assertThat(service.tabs().cup()).isTrue();

        when(matches.existsByTypeInAndDateBetween(any(), any(), any())).thenReturn(false);
        assertThat(service.tabs().cup()).isFalse();
    }

    @Test
    void un_tableau_en_cours_garde_la_coupe_visible_meme_sans_match_connu() {
        Bracket running = new Bracket();
        when(brackets.findTopByOrderByLocksAtDesc()).thenReturn(Optional.of(running));
        assertThat(service.tabs().cup()).isTrue();

        running.setAwarded(true);
        assertThat(service.tabs().cup()).isFalse();
    }

    @Test
    void saison_cachee_entre_la_fin_des_pronostics_et_les_premiers_resultats() {
        assertThat(service.tabs().season()).isFalse();
    }

    @Test
    void saison_visible_tant_que_les_pronostics_sont_ouverts() {
        when(picks.deadline("2026-27")).thenReturn(NOW.plus(Duration.ofDays(1)));
        assertThat(service.tabs().season()).isTrue();
    }

    @Test
    void saison_visible_avant_que_le_calendrier_soit_connu() {
        when(picks.deadline("2026-27")).thenReturn(null);
        assertThat(service.tabs().season()).isTrue();
    }

    @Test
    void saison_visible_des_qu_un_resultat_est_saisi() {
        when(results.findBySeason("2026-27")).thenReturn(List.of(new SeasonResult()));
        assertThat(service.tabs().season()).isTrue();
    }
}
