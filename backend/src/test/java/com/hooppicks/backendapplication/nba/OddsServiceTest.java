package com.hooppicks.backendapplication.nba;

import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.repository.MatchRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class OddsServiceTest {

    // Sans historique (mock vide) : la ligne des totaux part de la moyenne de la ligue.
    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final OddsService oddsService = new OddsService(matchRepository);

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

        // Probabilité plafonnée à 95 %, cote plancher à 1,03.
        assertThat(match.getMoneylineHome()).isGreaterThanOrEqualTo(1.03);
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

    private Team team(String id, double elo) {
        Team t = teamWithElo(elo);
        t.setId(id);
        return t;
    }

    private Match finished(Team home, Team away, int homeScore, int awayScore) {
        Match m = new Match();
        m.setHomeTeam(home);
        m.setAwayTeam(away);
        m.setHomeScore(homeScore);
        m.setAwayScore(awayScore);
        m.setStatus(MatchStatus.FINISHED);
        return m;
    }

    @Test
    void sans_historique_la_ligne_des_totaux_est_la_moyenne_de_la_ligue_en_demi_point() {
        Match match = new Match();
        oddsService.applyOdds(match, team("h", 1500), team("a", 1500));

        // 2 × 114 = 228 → 228,5 : toujours un demi-point, jamais d'égalité pile.
        assertThat(match.getTotalValue()).isEqualTo(228.5);
    }

    @Test
    void deux_equipes_offensives_ont_une_ligne_plus_haute() {
        Team home = team("h", 1500);
        Team away = team("a", 1500);
        Team other = team("x", 1500);
        // 10 matchs chacune : 125 marqués, 120 encaissés.
        List<Match> homeGames = java.util.Collections.nCopies(10, finished(home, other, 125, 120));
        List<Match> awayGames = java.util.Collections.nCopies(10, finished(other, away, 120, 125));
        when(matchRepository.findRecentFinishedForTeam(eq(home), any(), any())).thenReturn(homeGames);
        when(matchRepository.findRecentFinishedForTeam(eq(away), any(), any())).thenReturn(awayGames);

        Match match = new Match();
        oddsService.applyOdds(match, home, away);

        // Par équipe : (10×125 + 5×114) / 15 ≈ 121,3 marqués, (10×120 + 5×114) / 15 = 118 encaissés
        // → (121,3 + 118) / 2 × 2 ≈ 239,3 → 239,5.
        assertThat(match.getTotalValue()).isEqualTo(239.5);
    }

    @Test
    void une_cote_ne_descend_jamais_sous_1_03() {
        Match match = new Match();
        oddsService.applyOdds(match, teamWithElo(2600), teamWithElo(900));

        assertThat(match.getMoneylineHome()).isGreaterThanOrEqualTo(1.03);
    }
}
