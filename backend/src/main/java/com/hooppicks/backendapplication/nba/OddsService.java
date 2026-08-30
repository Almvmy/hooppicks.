package com.hooppicks.backendapplication.nba;

import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.Team;
import org.springframework.stereotype.Service;

/**
 * Calcule des cotes plausibles à partir de la force Elo des deux équipes
 * (cf. EloService). L'API gratuite ne fournit pas de vraies cotes : celles-ci
 * sont donc synthétiques mais varient selon les équipes, contrairement aux
 * anciennes cotes fixes identiques pour tous les matchs.
 */
@Service
public class OddsService {

    private static final double HOME_ADVANTAGE = 100.0; // avantage terrain, en points Elo
    private static final double VIG = 1.06; // marge bookmaker ~6%, répartie sur les deux côtés
    private static final double ELO_POINTS_PER_MARGIN_POINT = 28.0; // ~1 pt d'écart au score par 28 Elo
    private static final double BASE_TOTAL = 220.5;

    /**
     * Calcule et pose les cotes (moneyline, spread, total) sur le match à
     * partir des Elo actuels des deux équipes. N'appeler que sur un match
     * SCHEDULED sans pari en attente dessus (cf. NbaSyncService) : sinon on
     * risque de faire bouger une ligne sur laquelle un pari a déjà été posé.
     */
    public void applyOdds(Match match, Team home, Team away) {
        double homeElo = home.getEloRating();
        double awayElo = away.getEloRating();

        double probHome = clamp(
                1.0 / (1.0 + Math.pow(10, (awayElo - (homeElo + HOME_ADVANTAGE)) / 400.0)),
                0.05, 0.95
        );
        double probAway = 1.0 - probHome;

        match.setMoneylineHome(round2(1.0 / (probHome * VIG)));
        match.setMoneylineAway(round2(1.0 / (probAway * VIG)));

        double eloDiff = (homeElo + HOME_ADVANTAGE) - awayElo;
        double spread = clamp(-eloDiff / ELO_POINTS_PER_MARGIN_POINT, -20, 20);
        match.setSpreadValue(roundHalf(spread));
        match.setSpreadOddsHome(1.91);
        match.setSpreadOddsAway(1.91);

        double total = BASE_TOTAL + clamp((homeElo + awayElo - 3000) / 400.0, -10, 10);
        match.setTotalValue(roundHalf(total));
        match.setTotalOddsOver(1.91);
        match.setTotalOddsUnder(1.91);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double roundHalf(double value) {
        return Math.round(value * 2.0) / 2.0;
    }
}
