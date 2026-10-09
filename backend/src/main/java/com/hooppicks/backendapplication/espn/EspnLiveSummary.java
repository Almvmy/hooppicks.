package com.hooppicks.backendapplication.espn;

import java.util.List;
import java.util.Map;

/**
 * Résumé ESPN d'un match en direct : état, score par quart-temps, stats des
 * deux équipes, feuille de match du moment et le détail du jeu. Affichage
 * seulement, comme {@link EspnLiveGame}.
 */
public record EspnLiveSummary(EspnLiveGame status, Side home, Side away, List<PlayerBoxScoreRow> players,
                              Details details) {

    /**
     * @param stats statistiques d'équipe par nom ESPN (« totalRebounds » → « 27 »)
     * @param teamFouls fautes d'équipe du quart-temps en cours (null si inconnu)
     * @param bonus « SINGLE » / « DOUBLE » quand l'adversaire tire des lancers sur chaque faute, sinon null
     */
    public record Side(String abbreviation, List<Integer> linescores, Map<String, String> stats,
                       boolean possession, Integer teamFouls, String bonus) {}

    /** Probabilité de victoire de l'équipe à domicile après `elapsedSeconds` de jeu. */
    public record WinPoint(int elapsedSeconds, double homeWinPct) {}

    /**
     * Un tir sur le demi-terrain, en pieds : panier vers (25, 0), ligne de fond
     * vers y = −5. Les deux équipes sont ramenées sur le même panier par ESPN.
     */
    public record Shot(double x, double y, boolean made, int points, String teamAbbreviation, String playerName) {}

    /**
     * Temps fort : un panier ou une fin de période. `kind` est un code
     * (« three », « dunk », « layup », « jumper », « hook », « alley_oop »,
     * « free_throw », « end_period », « end_game ») mis en mots côté app.
     */
    public record KeyPlay(int period, String clock, String kind, int points, String teamAbbreviation,
                          String playerName, int awayScore, int homeScore) {}

    public record Details(List<WinPoint> winProbability, List<Shot> shots, List<KeyPlay> keyPlays) {
        public static Details empty() {
            return new Details(List.of(), List.of(), List.of());
        }
    }
}
