package com.hooppicks.backendapplication.espn;

import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchType;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Phase d'un match (présaison, Coupe NBA, playoffs…) et son intitulé en
 * français, déduits du scoreboard ESPN. Les intitulés ESPN sont en anglais
 * et suivent des formats stables ("East 1st Round - Game 2") : on les
 * traduit ici une fois pour toutes plutôt que côté frontend.
 */
public record EspnMatchStage(MatchType type, String label, String seriesSummary) {

    private static final Pattern GAME_NUMBER = Pattern.compile("^(.*) - Game (\\d+)$");
    private static final Pattern PLACE = Pattern.compile("(\\d+)(?:st|nd|rd|th) Place");
    private static final Pattern LEADS = Pattern.compile("^(\\S+) leads series (\\d+-\\d+)$");
    private static final Pattern WINS = Pattern.compile("^(\\S+) wins series (\\d+-\\d+)$");
    private static final Pattern TIED = Pattern.compile("^Series tied (\\d+-\\d+)$");

    private static final Map<String, String> CUP_ROUNDS = Map.of(
            "Group Play", "Phase de groupes",
            "Quarterfinals", "Quarts de finale",
            "Semifinals", "Demi-finales"
    );

    public static EspnMatchStage of(EspnGameRow row) {
        String note = row.note() == null ? "" : row.note().trim();

        if ("ALLSTAR".equals(row.competitionType())) {
            return new EspnMatchStage(MatchType.ALL_STAR, "All-Star Game", null);
        }
        switch (row.seasonType()) {
            case 1 -> {
                return new EspnMatchStage(MatchType.PRESEASON, note.isEmpty() ? "Présaison" : "Présaison · " + note, null);
            }
            case 5 -> {
                return new EspnMatchStage(MatchType.PLAY_IN, playInLabel(note), null);
            }
            case 3 -> {
                return new EspnMatchStage(MatchType.PLAYOFFS, playoffLabel(note), seriesLabel(row.seriesSummary()));
            }
            default -> { }
        }
        if (note.startsWith("NBA Cup")) {
            return new EspnMatchStage(MatchType.NBA_CUP, cupLabel(note), null);
        }
        // Saison régulière : pas d'intitulé, et ESPN y appelle "series" le
        // bilan des confrontations de la saison, sans intérêt ici.
        return new EspnMatchStage(MatchType.REGULAR, null, null);
    }

    public void applyTo(Match match) {
        match.setType(type);
        match.setStageLabel(label);
        match.setSeriesSummary(seriesSummary);
    }

    private static String conference(String english) {
        return switch (english) {
            case "East" -> "Est";
            case "West" -> "Ouest";
            default -> english;
        };
    }

    // "NBA Play-In - East - 7th Place vs 8th Place" -> "Play-in Est · 7e contre 8e"
    private static String playInLabel(String note) {
        String[] parts = note.split(" - ");
        if (parts.length < 3) return "Play-in";
        String places = PLACE.matcher(parts[2]).replaceAll("$1e").replace(" vs ", " contre ");
        return "Play-in " + conference(parts[1]) + " · " + places;
    }

    // "East 1st Round - Game 2" -> "1er tour Est · Match 2", "NBA Finals - Game 3" -> "Finales NBA · Match 3"
    private static String playoffLabel(String note) {
        Matcher m = GAME_NUMBER.matcher(note);
        if (!m.matches()) return "Playoffs";
        String round = m.group(1);
        String game = " · Match " + m.group(2);
        if (round.equals("NBA Finals")) return "Finales NBA" + game;

        String[] words = round.split(" ", 2);
        if (words.length < 2) return "Playoffs" + game;
        String conf = conference(words[0]);
        String stage = switch (words[1]) {
            case "1st Round" -> "1er tour";
            case "Semifinals" -> "Demi-finale";
            case "Finals" -> "Finale";
            default -> words[1];
        };
        return stage + " " + conf + game;
    }

    // "NBA Cup - Quarterfinals" -> "Coupe NBA · Quarts de finale", "NBA Cup Championship" -> "Coupe NBA · Finale"
    private static String cupLabel(String note) {
        if (note.contains("Championship")) return "Coupe NBA · Finale";
        String[] parts = note.split(" - ", 2);
        if (parts.length < 2) return "Coupe NBA";
        return "Coupe NBA · " + CUP_ROUNDS.getOrDefault(parts[1], parts[1]);
    }

    // "NYK leads series 2-0" -> "NYK mène 2-0" (sigles déjà convertis aux nôtres)
    private static String seriesLabel(String summary) {
        if (summary == null || summary.isBlank()) return null;
        Matcher m = LEADS.matcher(summary);
        if (m.matches()) return EspnStatsClient.fromEspnAbbreviation(m.group(1)) + " mène " + m.group(2);
        m = WINS.matcher(summary);
        if (m.matches()) return EspnStatsClient.fromEspnAbbreviation(m.group(1)) + " remporte la série " + m.group(2);
        m = TIED.matcher(summary);
        if (m.matches()) return "Égalité " + m.group(1);
        return summary;
    }
}
