package com.hooppicks.backendapplication.live;

import java.util.List;
import java.util.Map;

/**
 * Chaînes YouTube officielles des 30 équipes (relevées et vérifiées une à une
 * en octobre 2026 : nom de la chaîne et résumés publiés) et les noms sous
 * lesquels une équipe apparaît dans les titres de vidéos (« Sixers » pour les
 * 76ers, la ville seule…). Clé : sigle de l'équipe chez nous.
 */
final class TeamChannels {

    record Channel(String channelId, List<String> names) {}

    static final Map<String, Channel> BY_ABBREVIATION = Map.ofEntries(
            Map.entry("ATL", new Channel("UCpfcwELvR1wtcRJ0UxNXHYw", List.of("Hawks", "Atlanta"))),
            Map.entry("BOS", new Channel("UCMfT9dr6xC_RIWoA9hI0meQ", List.of("Celtics", "Boston"))),
            Map.entry("BKN", new Channel("UCTenKHt0h3VjdMvRWP6Lbvw", List.of("Nets", "Brooklyn"))),
            Map.entry("CHA", new Channel("UCPhFI5lIFnSggz7HgY9UuuQ", List.of("Hornets", "Charlotte"))),
            Map.entry("CHI", new Channel("UCvZi1jVVZ2yq0k5kkjzmuGw", List.of("Bulls", "Chicago"))),
            Map.entry("CLE", new Channel("UCOdS-I1sYkKWhtTjMUWP_TA", List.of("Cavaliers", "Cavs", "Cleveland"))),
            Map.entry("DAL", new Channel("UCwMUtT-zjoJOrtNmSWoMo0w", List.of("Mavericks", "Mavs", "Dallas"))),
            Map.entry("DEN", new Channel("UCl8hzdP5wVlhuzNG3WCJa1w", List.of("Nuggets", "Denver"))),
            Map.entry("DET", new Channel("UCtcSBo9EzOtXHxiPhU6RN8A", List.of("Pistons", "Detroit"))),
            Map.entry("GSW", new Channel("UCeYc_OjHs3QNxIjti2whKzg", List.of("Warriors", "Golden State"))),
            Map.entry("HOU", new Channel("UCVD7l69MVGFq_wzQvbk9HbQ", List.of("Rockets", "Houston"))),
            Map.entry("IND", new Channel("UCUQDCnAwU-35cOo8WCzg6zA", List.of("Pacers", "Indiana"))),
            Map.entry("LAC", new Channel("UCoK6pw3iIVF9WAWnQd3hj-g", List.of("Clippers"))),
            Map.entry("LAL", new Channel("UC8CSt-oVqy8pUAoKSApTxQw", List.of("Lakers"))),
            Map.entry("MEM", new Channel("UCCK5EpWKYrAmILfaZThCV-Q", List.of("Grizzlies", "Memphis"))),
            Map.entry("MIA", new Channel("UC8bZbiKoPNRi3taABIaFeBw", List.of("Heat", "Miami"))),
            Map.entry("MIL", new Channel("UCRZDEVva3Z8h_Q0VetTgDUA", List.of("Bucks", "Milwaukee"))),
            Map.entry("MIN", new Channel("UCXWDN5NKVFgnPt25CMh98Cg", List.of("Timberwolves", "Wolves", "Minnesota"))),
            Map.entry("NOP", new Channel("UCHvG7tf62PwI04ZRfoptRSw", List.of("Pelicans", "New Orleans"))),
            Map.entry("NYK", new Channel("UC0hb8f0OXHEzDrJDUq-YVVw", List.of("Knicks", "New York"))),
            Map.entry("OKC", new Channel("UCpXdQhy6kb5CTD8hKlmOL3w", List.of("Thunder", "Oklahoma City", "OKC"))),
            Map.entry("ORL", new Channel("UCxHFH-yfbhUrsWY4prPx3oQ", List.of("Magic", "Orlando"))),
            Map.entry("PHI", new Channel("UC5qJUyng_ezl0TVjVJFqtfQ", List.of("76ers", "Sixers", "Philadelphia"))),
            Map.entry("PHX", new Channel("UCLxlWVVHz2a8SdCfxzVXzQw", List.of("Suns", "Phoenix"))),
            Map.entry("POR", new Channel("UCXk66yyzXo7-2M1BMqLhltQ", List.of("Trail Blazers", "Blazers", "Portland"))),
            Map.entry("SAC", new Channel("UCSgFigczGdNMilV1K23JgUQ", List.of("Kings", "Sacramento"))),
            Map.entry("SAS", new Channel("UCEZHE-0CoHqeL1LGFa2EmQw", List.of("Spurs", "San Antonio"))),
            Map.entry("TOR", new Channel("UCYBFE432C2AmNRDGEXE4uVg", List.of("Raptors", "Toronto"))),
            Map.entry("UTA", new Channel("UCv9iSdeI9IzWfV8yTDsMYWA", List.of("Jazz", "Utah"))),
            Map.entry("WAS", new Channel("UCTkdqMgbT6bx0XAuAMMpYcQ", List.of("Wizards", "Washington")))
    );

    /** Map.of refuse une clé nulle : un match sans sigle d'équipe la ferait planter. */
    static boolean has(String abbreviation) {
        return abbreviation != null && BY_ABBREVIATION.containsKey(abbreviation);
    }

    /** Noms d'une équipe dans un titre : ceux de la table, sinon son surnom chez nous. */
    static List<String> names(String abbreviation, String fallbackName) {
        Channel c = abbreviation == null ? null : BY_ABBREVIATION.get(abbreviation);
        if (c != null) return c.names();
        return fallbackName == null ? List.of() : List.of(fallbackName);
    }

    static String feedUrl(String channelId) {
        return "https://www.youtube.com/feeds/videos.xml?channel_id=" + channelId;
    }

    private TeamChannels() {}
}
