package com.hooppicks.backendapplication.espn;

import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EspnStandingsServiceTest {

    @Mock
    private EspnStatsClient espnStatsClient;
    @Mock
    private TeamRepository teamRepository;

    private EspnStandingsService service;

    @BeforeEach
    void setUp() {
        service = new EspnStandingsService(espnStatsClient, teamRepository);
    }

    private Team team(String abbreviation) {
        Team t = new Team();
        t.setId(abbreviation.toLowerCase());
        t.setAbbreviation(abbreviation);
        return t;
    }

    @Test
    void met_a_jour_l_equipe_correspondante_par_sigle_espn() {
        Team hawks = team("ATL"); // sigle identique côté ESPN, pas dans les 6 exceptions
        when(teamRepository.findAll()).thenReturn(List.of(hawks));
        when(espnStatsClient.fetchStandings()).thenReturn(List.of(
                new EspnStandingRow("ATL", 46, 36, "L1", 2, "14", "https://example.com/atl.png")
        ));

        service.syncStandings();

        assertThat(hawks.getWins()).isEqualTo(46);
        assertThat(hawks.getLosses()).isEqualTo(36);
        assertThat(hawks.getStreak()).isEqualTo("L1");
        assertThat(hawks.getConferenceSeed()).isEqualTo(2);
        assertThat(hawks.getGamesBehind()).isEqualTo("14");
        assertThat(hawks.getLogoUrl()).isEqualTo("https://example.com/atl.png");
        verify(teamRepository).save(hawks);
    }

    @Test
    void utilise_la_traduction_de_sigle_pour_les_6_equipes_qui_different() {
        Team knicks = team("NYK"); // ESPN attend "NY"
        when(teamRepository.findAll()).thenReturn(List.of(knicks));
        when(espnStatsClient.fetchStandings()).thenReturn(List.of(
                new EspnStandingRow("NY", 53, 29, "L1", 3, "7", null)
        ));

        service.syncStandings();

        assertThat(knicks.getWins()).isEqualTo(53);
    }

    @Test
    void ignore_un_sigle_espn_sans_correspondance_locale() {
        Team hawks = team("ATL");
        when(teamRepository.findAll()).thenReturn(List.of(hawks));
        when(espnStatsClient.fetchStandings()).thenReturn(List.of(
                new EspnStandingRow("ZZZ", 1, 1, null, 1, null, null)
        ));

        service.syncStandings();

        assertThat(hawks.getWins()).isNull();
        verify(teamRepository, never()).save(any());
    }

    @Test
    void reponse_vide_garde_l_ancien_classement() {
        when(espnStatsClient.fetchStandings()).thenReturn(List.of());

        service.syncStandings();

        verify(teamRepository, never()).findAll();
        verify(teamRepository, never()).save(any());
    }
}
