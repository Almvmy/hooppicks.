package com.hooppicks.backendapplication.bracket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * La forme d'un tableau, pour n'importe quelle taille (8 équipes pour la Coupe,
 * 16 pour les playoffs). Matchs numérotés tour par tour : à 8 équipes, 0 à 3 les
 * quarts, 4 et 5 les demies, 6 la finale. Les enfants de la demie 4 sont les quarts
 * 0 et 1, ceux de la finale les demies 4 et 5. Un résultat vide vaut null.
 */
final class BracketTree {

    private BracketTree() {}

    static int games(int teams) {
        return teams - 1;
    }

    private static int firstRoundGames(int teams) {
        return teams / 2;
    }

    /** Le tour d'un match : 0 pour le premier tour. */
    static int round(int teams, int game) {
        int first = firstRoundGames(teams);
        return game < first ? 0 : 1 + round(teams, (game - first) * 2);
    }

    /** Chaque tour vaut le double du précédent : bien voir la finale compte plus qu'un quart. */
    static int points(int teams, int game) {
        return 10 << round(teams, game);
    }

    /** Les deux équipes d'un match d'après les résultats (réels ou choisis) des tours d'avant. */
    static List<String> participants(List<String> teams, List<String> results, int game) {
        int first = firstRoundGames(teams.size());
        if (game < first) return Arrays.asList(teams.get(game * 2), teams.get(game * 2 + 1));
        int child = (game - first) * 2;
        return Arrays.asList(results.get(child), results.get(child + 1));
    }

    /** Remplie, et chaque vainqueur choisi joue bien ce match d'après les tours d'avant. */
    static boolean isValid(List<String> teams, List<String> picks) {
        if (picks.size() != games(teams.size())) return false;
        for (int game = 0; game < picks.size(); game++) {
            String pick = picks.get(game);
            if (pick == null || pick.isBlank() || !participants(teams, picks, game).contains(pick)) return false;
        }
        return true;
    }

    /** Un résultat corrigé : les tours suivants qui reposaient sur l'ancien vainqueur sont effacés. */
    static List<String> withWinner(List<String> teams, List<String> winners, int game, String team) {
        List<String> updated = new ArrayList<>(winners);
        updated.set(game, team);
        for (int g = game + 1; g < updated.size(); g++) {
            List<String> pair = participants(teams, updated, g);
            // Tant que l'adversaire n'est pas connu, ce match n'a pas de vainqueur.
            if (updated.get(g) != null && (pair.contains(null) || !pair.contains(updated.get(g)))) updated.set(g, null);
        }
        return updated;
    }

    static int score(List<String> teams, List<String> winners, List<String> picks) {
        int total = 0;
        for (int game = 0; game < winners.size() && game < picks.size(); game++) {
            if (winners.get(game) != null && winners.get(game).equals(picks.get(game))) total += points(teams.size(), game);
        }
        return total;
    }

    static List<String> parse(String csv) {
        if (csv == null || csv.isEmpty()) return new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (String v : csv.split(",", -1)) values.add(v.isEmpty() ? null : v);
        return values;
    }

    static String format(List<String> values) {
        return String.join(",", values.stream().map(v -> v == null ? "" : v).toList());
    }
}
