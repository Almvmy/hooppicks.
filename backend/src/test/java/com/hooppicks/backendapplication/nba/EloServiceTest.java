package com.hooppicks.backendapplication.nba;

import com.hooppicks.backendapplication.entity.Team;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class EloServiceTest {

    private final EloService eloService = new EloService();

    private Team teamWithElo(double elo) {
        Team team = new Team();
        team.setEloRating(elo);
        return team;
    }

    @Test
    void egalite_de_force_victoire_a_domicile_rapporte_la_moitie_du_facteur_k() {
        Team home = teamWithElo(1500);
        Team away = teamWithElo(1500);

        eloService.applyResult(home, away, 100, 90);

        // Elo égaux -> probabilité attendue 50%, donc gain = K * (1 - 0.5) = 10
        assertThat(home.getEloRating()).isCloseTo(1510.0, within(0.01));
        assertThat(away.getEloRating()).isCloseTo(1490.0, within(0.01));
    }

    @Test
    void le_gain_et_la_perte_sont_toujours_symetriques() {
        Team home = teamWithElo(1620);
        Team away = teamWithElo(1400);
        double homeBefore = home.getEloRating();
        double awayBefore = away.getEloRating();

        eloService.applyResult(home, away, 88, 102);

        double homeDelta = home.getEloRating() - homeBefore;
        double awayDelta = away.getEloRating() - awayBefore;
        assertThat(homeDelta).isCloseTo(-awayDelta, within(0.0001));
    }

    @Test
    void une_victoire_surprise_de_l_outsider_rapporte_plus_qu_une_victoire_attendue() {
        Team favorite = teamWithElo(1700);
        Team outsider = teamWithElo(1300);

        // Cas 1 : le favori gagne, comme attendu.
        Team favoriteCopy = teamWithElo(1700);
        Team outsiderCopy = teamWithElo(1300);
        eloService.applyResult(favoriteCopy, outsiderCopy, 110, 90);
        double favoriteGainAttendu = favoriteCopy.getEloRating() - 1700;

        // Cas 2 : l'outsider cause l'exploit (gagne à l'extérieur).
        eloService.applyResult(favorite, outsider, 90, 110);
        double outsiderGainSurprise = outsider.getEloRating() - 1300;

        assertThat(outsiderGainSurprise).isGreaterThan(favoriteGainAttendu);
    }

    @Test
    void une_defaite_du_favori_lui_coute_plus_cher_qu_une_defaite_de_l_outsider() {
        Team favorite = teamWithElo(1700);
        Team outsider = teamWithElo(1300);
        double favoriteBefore = favorite.getEloRating();

        eloService.applyResult(favorite, outsider, 90, 110);

        double favoriteLoss = favoriteBefore - favorite.getEloRating();
        assertThat(favoriteLoss).isGreaterThan(10.0); // plus que le cas "force égale"
    }
}
