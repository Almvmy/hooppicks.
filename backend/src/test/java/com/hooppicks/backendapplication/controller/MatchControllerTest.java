package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.bet.PickPercentagesService;
import com.hooppicks.backendapplication.live.LiveMatchService;
import com.hooppicks.backendapplication.live.MatchPreviewService;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.PlayerMatchStatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchControllerTest {

    @Mock private MatchRepository matchRepository;
    @Mock private PlayerMatchStatRepository playerMatchStatRepository;
    @Mock private PickPercentagesService pickPercentagesService;
    @Mock private LiveMatchService liveMatchService;
    @Mock private MatchPreviewService matchPreviewService;

    private MatchController controller;

    @BeforeEach
    void setUp() {
        controller = new MatchController(matchRepository, playerMatchStatRepository, pickPercentagesService,
                liveMatchService, matchPreviewService);
    }

    @Test
    void une_fenetre_de_dates_inclut_le_dernier_jour_en_entier() {
        controller.getAllMatches("2026-10-01", "2026-10-13", null, null);

        verify(matchRepository).findByDateBetween(Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-14T00:00:00Z"));
        verify(matchRepository, never()).findAll();
    }

    @Test
    void la_saison_d_une_equipe_ne_charge_que_ses_matchs() {
        controller.getAllMatches(null, null, "lal", null);

        verify(matchRepository).findByTeamId("lal");
        verify(matchRepository, never()).findAll();
    }

    @Test
    void les_matchs_des_paris_sont_charges_par_id_sans_doublon() {
        controller.getAllMatches(null, null, null, "a, b,a,,c");

        verify(matchRepository).findAllById(List.of("a", "b", "c"));
        verify(matchRepository, never()).findAll();
    }

    @Test
    void sans_filtre_tout_reste_renvoye_pour_les_anciennes_versions_de_l_app() {
        controller.getAllMatches(null, null, null, null);

        verify(matchRepository).findAll();
        verify(matchRepository, never()).findByDateBetween(any(), any());
    }
}
