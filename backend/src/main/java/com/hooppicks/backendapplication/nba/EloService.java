package com.hooppicks.backendapplication.nba;

import com.hooppicks.backendapplication.entity.Team;
import org.springframework.stereotype.Service;

/**
 * Système Elo classique (comme aux échecs) pour estimer la force relative
 * des équipes à partir de leurs résultats. Sert de base au calcul des cotes
 * dans OddsService. Volontairement simple pour un premier jet : pas de
 * multiplicateur d'écart de score, juste victoire/défaite.
 */
@Service
public class EloService {

    private static final double K = 20.0;

    /**
     * Met à jour les notes Elo des deux équipes après un match terminé.
     * À n'appeler qu'une seule fois par match (cf. détection "vient de
     * passer à FINISHED" dans NbaSyncService) sous peine de fausser les
     * notes en les appliquant plusieurs fois au même résultat.
     */
    public void applyResult(Team home, Team away, int homeScore, int awayScore) {
        double homeElo = home.getEloRating();
        double awayElo = away.getEloRating();

        double expectedHome = 1.0 / (1.0 + Math.pow(10, (awayElo - homeElo) / 400.0));
        double actualHome = homeScore > awayScore ? 1.0 : 0.0;
        double delta = K * (actualHome - expectedHome);

        home.setEloRating(homeElo + delta);
        away.setEloRating(awayElo - delta);
    }
}
