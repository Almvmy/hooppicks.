package com.hooppicks.backendapplication.nba;

import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.Team;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class OddsServiceTest {

    private final OddsService oddsService = new OddsService();

    private Team teamWithElo(double elo) {
        Team team = new Team();
        team.setEloRating(elo);
        return team;
    }

    @Test
    void equipes_de_force_egale_donnent_des_cotes_proches_avec_la_marge_bookmaker() {
        Match match = new Match();
        Team home = teamWithElo(1500);
        Team away = teamWithElo(1500);

        oddsService.applyOdds(match, home, away);

        // L'avantage terrain fait légèrement pencher vers l'équipe qui reçoit.
        assertThat(match.getMoneylineHome()).isLessThan(match.getMoneylineAway());

        // Somme des probabilités implicites ~ 1.06 (marge bookmaker visée), pas 1.0 pile.
        double impliedSum = 1 / match.getMoneylineHome() + 1 / match.getMoneylineAway();
        assertThat(impliedSum).isCloseTo(1.06, within(0.01));
    }

    @Test
    void une_equipe_nettement_plus_forte_a_domicile_est_favorite() {
        Match match = new Match();
        Team strongHome = teamWithElo(1800);
        Team weakAway = teamWithElo(1300);

        oddsService.applyOdds(match, strongHome, weakAway);

        assertThat(match.getMoneylineHome()).isLessThan(match.getMoneylineAway());
        // Ligne de spread négative = équipe à domicile favorite (cf. BetResolutionService.evaluateSelection).
        assertThat(match.getSpreadValue()).isNegative();
    }

    @Test
    void un_ecart_de_force_extreme_reste_dans_des_bornes_raisonnables() {
        Match match = new Match();
        Team dominant = teamWithElo(2600);
        Team faible = teamWithElo(900);

        oddsService.applyOdds(match, dominant, faible);

        // Probabilité clampée à 95% -> cote minimale ~ 1/(0.95*1.06)
        assertThat(match.getMoneylineHome()).isGreaterThanOrEqualTo(0.99);
        assertThat(match.getSpreadValue()).isGreaterThanOrEqualTo(-20.0);
    }

    @Test
    void la_ligne_de_spread_est_arrondie_au_demi_point() {
        Match match = new Match();
        Team home = teamWithElo(1550);
        Team away = teamWithElo(1480);

        oddsService.applyOdds(match, home, away);

        double roundedToHalf = Math.round(match.getSpreadValue() * 2.0) / 2.0;
        assertThat(match.getSpreadValue()).isEqualTo(roundedToHalf);
    }
}
