package com.hooppicks.backendapplication.espn;

import java.util.List;
import java.util.Map;

/**
 * Résumé ESPN d'un match en direct : état, score par quart-temps, stats des
 * deux équipes et feuille de match du moment. Affichage seulement, comme
 * {@link EspnLiveGame}.
 */
public record EspnLiveSummary(EspnLiveGame status, Side home, Side away, List<PlayerBoxScoreRow> players) {

    /**
     * @param stats statistiques d'équipe par nom ESPN (« totalRebounds » → « 27 »)
     */
    public record Side(String abbreviation, List<Integer> linescores, Map<String, String> stats) {}
}
