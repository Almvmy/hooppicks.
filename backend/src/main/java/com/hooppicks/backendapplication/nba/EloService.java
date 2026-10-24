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
    // reste revient vers la moyenne (effectifs qui changent). 75 % au départ,
    // comme les modèles Elo NBA publics, mais le Brooklyn de la saison passée
    // pesait encore tout début de saison : un 3-0 restait coté 6,94. Choix du
    // propriétaire : 50 %, et les premiers matchs comptent double (K).
    static final double CARRY_OVER = 0.5;
    static final double PREVIOUS_CARRY_OVER = 0.75;
    // Matchs de la saison pendant lesquels un résultat compte plus (K jusqu'à
    // 2 × K au premier match, puis K normal au 10e) : l'Elo rattrape vite une
    // équipe qui a changé pendant l'été.
    static final int EARLY_GAMES = 10;
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
        team.setEloCarryOver(CARRY_OVER);
        return true;
    }

    /**
     * Équipe amorcée avec l'ancien réglage (75 %) : son écart à 1500 est
     * ramené au nouveau (50 %), une seule fois. Les quelques résultats déjà
     * comptés sont ramenés avec, c'est sans conséquence en début de saison.
     * Renvoie true si l'Elo a changé.
     */
    public boolean rebaseCarryOver(Team team) {
        if (!Boolean.TRUE.equals(team.getEloSeeded()) || team.getEloCarryOver() != null) return false;
        team.setEloRating(1500 + (team.getEloRating() - 1500) * CARRY_OVER / PREVIOUS_CARRY_OVER);
        team.setEloCarryOver(CARRY_OVER);
        return true;
    }

    public void applyResult(Team home, Team away, int homeScore, int awayScore) {
        double homeElo = home.getEloRating();
        double awayElo = away.getEloRating();

        double expectedHome = 1.0 / (1.0 + Math.pow(10, (awayElo - homeElo) / 400.0));
        double actualHome = homeScore > awayScore ? 1.0 : 0.0;
        double delta = k(home, away) * (actualHome - expectedHome);

        home.setEloRating(homeElo + delta);
        away.setEloRating(awayElo - delta);
    }

    // Matchs joués cette saison d'après le classement ESPN (avant le premier
    // match, il a encore le bilan complet de la saison passée : K normal).
    static double k(Team home, Team away) {
        int games = Math.min(gamesPlayed(home), gamesPlayed(away));
        if (games >= EARLY_GAMES) return K;
        return K * (1 + (double) (EARLY_GAMES - games) / EARLY_GAMES);
    }

    private static int gamesPlayed(Team t) {
        Integer w = t.getWins(), l = t.getLosses();
        return w == null || l == null ? EARLY_GAMES : w + l;
    }
}
