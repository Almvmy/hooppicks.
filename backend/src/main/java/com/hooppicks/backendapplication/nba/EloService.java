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
    // Part du bilan de la saison passée gardée d'une saison à l'autre : le
    // reste revient vers la moyenne (effectifs qui changent), comme les
    // modèles Elo NBA publics.
    static final double CARRY_OVER = 0.75;
    // Bilan minimum pour être pris en compte : une saison quasi complète.
    private static final int MIN_GAMES = 60;

    /**
     * Elo de départ tiré du bilan de la saison passée (importé d'ESPN), une
     * seule fois par équipe. Sans ça, les 30 équipes partaient à 1500 et
     * presque tous les matchs avaient exactement les mêmes cotes. Le
     * décalage s'ajoute à l'Elo actuel : les résultats déjà comptés restent.
     * Renvoie true si l'équipe a été amorcée.
     */
    public boolean seedFromRecord(Team team) {
        if (Boolean.TRUE.equals(team.getEloSeeded())) return false;
        Integer w = team.getWins(), l = team.getLosses();
        if (w == null || l == null || w + l < MIN_GAMES) return false;
        double p = Math.min(0.85, Math.max(0.15, (double) w / (w + l)));
        double offset = 400 * Math.log10(p / (1 - p)) * CARRY_OVER;
        team.setEloRating(team.getEloRating() + offset);
        team.setEloSeeded(true);
        return true;
    }

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
