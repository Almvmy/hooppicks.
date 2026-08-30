package com.hooppicks.backendapplication.espn;

import com.hooppicks.backendapplication.entity.RosterPlayer;
import com.hooppicks.backendapplication.repository.RosterPlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EspnPlayerStatsServiceTest {

    @Mock
    private EspnStatsClient espnStatsClient;
    @Mock
    private RosterPlayerRepository rosterPlayerRepository;

    private EspnPlayerStatsService service;

    @BeforeEach
    void setUp() {
        service = new EspnPlayerStatsService(espnStatsClient, rosterPlayerRepository);
    }

    private RosterPlayer player(String id) {
        RosterPlayer p = new RosterPlayer();
        p.setId(id);
        return p;
    }

    @Test
    void applique_les_moyennes_saison_et_marque_le_joueur_synchronise() {
        RosterPlayer p = player("100");
        when(rosterPlayerRepository.findAllOrderByStatsUpdatedAtAscNullsFirst(any(Pageable.class)))
                .thenReturn(List.of(p));
        when(espnStatsClient.fetchSeasonStats("100")).thenReturn(Optional.of(
                new PlayerSeasonStatsRow("2025-26", 78, 78, 33.4, 20.8, 3.4, 3.7, 1.3, 0.5, 2.1, 46.0, 38.0, 84.0)
        ));

        service.syncBatch();

        assertThat(p.getPointsPerGame()).isEqualTo(20.8);
        assertThat(p.getStatsSeasonLabel()).isEqualTo("2025-26");
        assertThat(p.getStatsUpdatedAt()).isNotNull();
        verify(rosterPlayerRepository).save(p);
    }

    @Test
    void marque_quand_meme_le_joueur_synchronise_si_espn_n_a_aucune_donnee() {
        // Cas d'une recrue qui n'a pas encore joué : pas d'exception à
        // lever, juste rien à appliquer : mais on avance quand même le
        // curseur pour ne pas bloquer le lot suivant sur ce joueur en boucle.
        RosterPlayer rookie = player("200");
        when(rosterPlayerRepository.findAllOrderByStatsUpdatedAtAscNullsFirst(any(Pageable.class)))
                .thenReturn(List.of(rookie));
        when(espnStatsClient.fetchSeasonStats("200")).thenReturn(Optional.empty());

        service.syncBatch();

        assertThat(rookie.getPointsPerGame()).isNull();
        assertThat(rookie.getStatsUpdatedAt()).isNotNull();
        verify(rosterPlayerRepository).save(rookie);
    }

    @Test
    void un_joueur_en_echec_n_empeche_pas_le_traitement_des_autres() {
        RosterPlayer failing = player("300");
        RosterPlayer ok = player("301");
        when(rosterPlayerRepository.findAllOrderByStatsUpdatedAtAscNullsFirst(any(Pageable.class)))
                .thenReturn(List.of(failing, ok));
        when(espnStatsClient.fetchSeasonStats("300")).thenThrow(new RuntimeException("boom"));
        when(espnStatsClient.fetchSeasonStats("301")).thenReturn(Optional.of(
                new PlayerSeasonStatsRow("2025-26", 10, 0, 12.0, 5.0, 2.0, 1.0, 0.5, 0.2, 1.0, 45.0, 30.0, 80.0)
        ));

        service.syncBatch();

        verify(rosterPlayerRepository, never()).save(failing);
        verify(rosterPlayerRepository).save(ok);
    }
}
