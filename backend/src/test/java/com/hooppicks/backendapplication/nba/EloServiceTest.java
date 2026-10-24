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

        // 56-26 : ~+67 ; 20-62 : ~-98 (50 % du bilan gardé).
        assertThat(strong.getEloRating()).isCloseTo(1567, within(5.0));
        assertThat(weak.getEloRating()).isCloseTo(1401, within(5.0));
        assertThat(strong.getEloCarryOver()).isEqualTo(0.5);
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
    void une_equipe_amorcee_a_75_pourcent_est_ramenee_a_50_une_seule_fois() {
        Team t = team(20, 62, 1350);
        t.setEloSeeded(true);
        assertThat(eloService.rebaseCarryOver(t)).isTrue();
        assertThat(t.getEloRating()).isCloseTo(1400, within(0.01));
        assertThat(eloService.rebaseCarryOver(t)).isFalse();
        assertThat(t.getEloRating()).isCloseTo(1400, within(0.01));
    }

    @Test
    void les_premiers_matchs_de_la_saison_comptent_plus() {
        Team a = team(0, 0, 1500), b = team(0, 0, 1500);
        eloService.applyResult(a, b, 110, 100);
        assertThat(a.getEloRating()).isCloseTo(1520, within(0.01)); // 2 × K × 0,5

        Team c = team(30, 20, 1500), d = team(25, 25, 1500);
        eloService.applyResult(c, d, 110, 100);
        assertThat(c.getEloRating()).isCloseTo(1510, within(0.01)); // K normal
    }

    @Test
    void un_bilan_trop_court_n_amorce_rien() {
        Team t = team(3, 1, 1500);
        assertThat(eloService.seedFromRecord(t)).isFalse();
        assertThat(t.getEloSeeded()).isNull();
    }
}
