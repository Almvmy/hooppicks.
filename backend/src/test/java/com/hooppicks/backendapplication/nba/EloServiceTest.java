package com.hooppicks.backendapplication.nba;

import com.hooppicks.backendapplication.entity.Team;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class EloServiceTest {

    private final EloService eloService = new EloService();

    private Team team(int wins, int losses, double elo) {
        Team t = new Team();
        t.setWins(wins);
        t.setLosses(losses);
        t.setEloRating(elo);
        return t;
    }

    @Test
    void un_bon_bilan_releve_l_elo_un_mauvais_le_baisse() {
        Team strong = team(56, 26, 1500);
        Team weak = team(20, 62, 1500);

        assertThat(eloService.seedFromRecord(strong)).isTrue();
        assertThat(eloService.seedFromRecord(weak)).isTrue();

        // 56-26 : ~+100 ; 20-62 : ~-148 (75 % du bilan gardé).
        assertThat(strong.getEloRating()).isCloseTo(1600, within(5.0));
        assertThat(weak.getEloRating()).isCloseTo(1352, within(5.0));
    }

    @Test
    void l_amorcage_ne_se_fait_qu_une_fois_et_garde_les_resultats_deja_comptes() {
        Team t = team(41, 41, 1508);
        eloService.seedFromRecord(t);
        assertThat(t.getEloRating()).isCloseTo(1508, within(0.01)); // bilan à 50 % : aucun décalage
        t.setWins(60);
        assertThat(eloService.seedFromRecord(t)).isFalse();
        assertThat(t.getEloRating()).isCloseTo(1508, within(0.01));
    }

    @Test
    void un_bilan_trop_court_n_amorce_rien() {
        Team t = team(3, 1, 1500);
        assertThat(eloService.seedFromRecord(t)).isFalse();
        assertThat(t.getEloSeeded()).isNull();
    }
}
