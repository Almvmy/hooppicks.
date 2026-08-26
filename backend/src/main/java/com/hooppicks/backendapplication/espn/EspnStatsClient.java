package com.hooppicks.backendapplication.espn;

import tools.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Client pour l'API (non officielle, non documentée) d'ESPN — pas de clé,
 * pas de contrat de stabilité garanti, mais c'est la seule source qui donne
 * la feuille de match par joueur gratuitement (balldontlie la verrouille
 * derrière un tier payant). Akamai (le CDN d'ESPN) bloque en 403 tout
 * User-Agent qui ressemble à un navigateur, voir le commentaire sur
 * fetchWithRetry — le vrai correctif est là, pas dans les tentatives. On
 * garde quand même la retry (espacée de plus de networkaddress.cache.ttl,
 * 10s, pour retirer une IP différente du DNS) en filet de sécurité pour
 * d'éventuels vrais problèmes réseau transitoires.
 */
@Component
public class EspnStatsClient {

    private static final Logger log = LoggerFactory.getLogger(EspnStatsClient.class);

    private static final String SCOREBOARD_URL =
            "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/scoreboard?dates=%s";
    private static final String SUMMARY_URL =
            "https://site.web.api.espn.com/apis/site/v2/sports/basketball/nba/summary?event=%s";
    private static final String ROSTER_URL =
            "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/teams/%s/roster";
    private static final String STANDINGS_URL =
            "https://site.api.espn.com/apis/v2/sports/basketball/nba/standings";
    private static final String ATHLETE_STATS_URL =
            "https://site.web.api.espn.com/apis/common/v3/sports/basketball/nba/athletes/%s/stats";

    // balldontlie -> ESPN : seules ces 6 franchises ont un sigle différent
    // entre les deux APIs (vérifié en comparant les deux listes d'équipes),
    // toutes les autres correspondent telles quelles.
    private static final Map<String, String> ABBREVIATION_OVERRIDES = Map.of(
            "GSW", "GS",
            "NOP", "NO",
            "NYK", "NY",
            "SAS", "SA",
            "UTA", "UTAH",
            "WAS", "WSH"
    );

    private final RestTemplate restTemplate;

    public EspnStatsClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public static String toEspnAbbreviation(String balldontlieAbbreviation) {
        return ABBREVIATION_OVERRIDES.getOrDefault(balldontlieAbbreviation, balldontlieAbbreviation);
    }

    /**
     * Cherche, parmi les matchs NBA de ce jour-là côté ESPN, celui qui
     * oppose ces deux équipes — c'est le seul moyen de relier un match
     * balldontlie à son event ESPN, les deux APIs n'ayant aucun ID en commun.
     */
    public Optional<String> findEventId(LocalDate date, String homeAbbreviation, String awayAbbreviation) {
        String espnHome = toEspnAbbreviation(homeAbbreviation);
        String espnAway = toEspnAbbreviation(awayAbbreviation);
        String url = String.format(SCOREBOARD_URL, date.format(DateTimeFormatter.BASIC_ISO_DATE));

        JsonNode root = fetchWithRetry(url);
        if (root == null) return Optional.empty();

        for (JsonNode event : root.path("events")) {
            String home = null;
            String away = null;
            for (JsonNode competitor : event.path("competitions").path(0).path("competitors")) {
                String abbr = competitor.path("team").path("abbreviation").asText();
                if ("home".equals(competitor.path("homeAway").asText())) home = abbr;
                else away = abbr;
            }
            if (espnHome.equalsIgnoreCase(home) && espnAway.equalsIgnoreCase(away)) {
                return Optional.of(event.path("id").asText());
            }
        }
        return Optional.empty();
    }

    /**
     * Feuille de match complète (les deux équipes) pour un event ESPN déjà
     * identifié. Une ligne "athlete" ignorée (DNP, données incomplètes) ne
     * fait pas échouer le reste — on récupère ce qu'on peut plutôt que rien.
     */
    public List<PlayerBoxScoreRow> fetchBoxScore(String eventId) {
        JsonNode root = fetchWithRetry(String.format(SUMMARY_URL, eventId));
        if (root == null) return List.of();

        List<PlayerBoxScoreRow> result = new ArrayList<>();
        for (JsonNode teamBlock : root.path("boxscore").path("players")) {
            String teamAbbr = teamBlock.path("team").path("abbreviation").asText();
            JsonNode statBlock = teamBlock.path("statistics").path(0);

            List<String> labels = new ArrayList<>();
            statBlock.path("labels").forEach(l -> labels.add(l.asText()));
            if (labels.isEmpty()) continue;

            for (JsonNode athleteEntry : statBlock.path("athletes")) {
                if (athleteEntry.path("didNotPlay").asBoolean(false)) continue;

                List<String> values = new ArrayList<>();
                athleteEntry.path("stats").forEach(v -> values.add(v.asText()));
                if (values.size() < labels.size()) continue;

                String name = athleteEntry.path("athlete").path("displayName").asText();
                boolean starter = athleteEntry.path("starter").asBoolean(false);
                result.add(toRow(teamAbbr, name, starter, labels, values));
            }
        }
        return result;
    }

    /**
     * Effectif actuel d'une équipe. L'URL ESPN attend le sigle en minuscules
     * (ex. "lal"), contrairement au scoreboard/summary qui l'attendent en
     * majuscules — vérifié en direct, pas une supposition.
     */
    public List<EspnRosterRow> fetchRoster(String balldontlieAbbreviation) {
        String espnAbbr = toEspnAbbreviation(balldontlieAbbreviation).toLowerCase();
        JsonNode root = fetchWithRetry(String.format(ROSTER_URL, espnAbbr));
        if (root == null) return List.of();

        List<EspnRosterRow> result = new ArrayList<>();
        for (JsonNode athlete : root.path("athletes")) {
            // Le tableau "injuries" ne contient qu'une entrée à la fois côté
            // ESPN (le statut courant), pas un historique — la première (et
            // seule) suffit.
            JsonNode injury = athlete.path("injuries").path(0);
            result.add(new EspnRosterRow(
                    athlete.path("id").asText(null),
                    athlete.path("firstName").asText(null),
                    athlete.path("lastName").asText(null),
                    athlete.path("position").path("abbreviation").asText(null),
                    athlete.path("jersey").asText(null),
                    athlete.path("displayHeight").asText(null),
                    athlete.path("displayWeight").asText(null),
                    athlete.path("headshot").path("href").asText(null),
                    injury.path("status").asText(null),
                    injury.path("date").asText(null)
            ));
        }
        return result;
    }

    /**
     * Classement officiel (victoires/défaites, série en cours, seed
     * conférence) des 30 équipes — un seul appel pour toute la ligue,
     * contrairement au roster qui en coûte un par équipe. Purement
     * informatif côté app : distinct de l'Elo utilisé pour les cotes.
     */
    public List<EspnStandingRow> fetchStandings() {
        JsonNode root = fetchWithRetry(STANDINGS_URL);
        if (root == null) return List.of();

        List<EspnStandingRow> result = new ArrayList<>();
        for (JsonNode conference : root.path("children")) {
            for (JsonNode entry : conference.path("standings").path("entries")) {
                String abbr = entry.path("team").path("abbreviation").asText(null);
                if (abbr == null) continue;

                Map<String, JsonNode> statsByName = new HashMap<>();
                for (JsonNode stat : entry.path("stats")) {
                    statsByName.put(stat.path("name").asText(), stat);
                }

                result.add(new EspnStandingRow(
                        abbr,
                        (int) statByName(statsByName, "wins"),
                        (int) statByName(statsByName, "losses"),
                        statsByName.containsKey("streak")
                                ? statsByName.get("streak").path("displayValue").asText(null) : null,
                        (int) statByName(statsByName, "playoffSeed"),
                        statsByName.containsKey("gamesBehind")
                                ? statsByName.get("gamesBehind").path("displayValue").asText(null) : null,
                        // Premier logo de la liste = variante "default" chez ESPN,
                        // toujours présente (vérifié sur les 30 équipes).
                        entry.path("team").path("logos").path(0).path("href").asText(null)
                ));
            }
        }
        return result;
    }

    /**
     * Moyennes saison d'un joueur, à partir de son id athlète ESPN — celui
     * déjà stocké sur RosterPlayer (cf. EspnRosterService), pas besoin de
     * rapprocher par nom. On prend la dernière entrée de la catégorie
     * "averages" : les saisons y sont dans l'ordre chronologique, donc la
     * plus récente est toujours en dernier (vérifié en direct sur un joueur
     * avec un historique multi-saisons). Si le joueur n'a pas encore joué
     * cette saison (blessure, recrue en attente), c'est alors la dernière
     * saison jouée qui ressort — le seasonLabel renvoyé permet à l'appelant
     * de savoir laquelle.
     */
    public Optional<PlayerSeasonStatsRow> fetchSeasonStats(String espnAthleteId) {
        JsonNode root = fetchWithRetry(String.format(ATHLETE_STATS_URL, espnAthleteId));
        if (root == null) return Optional.empty();

        JsonNode averages = null;
        for (JsonNode category : root.path("categories")) {
            if ("averages".equals(category.path("name").asText())) {
                averages = category;
                break;
            }
        }
        if (averages == null) return Optional.empty();

        List<String> labels = new ArrayList<>();
        averages.path("labels").forEach(l -> labels.add(l.asText()));

        JsonNode statsList = averages.path("statistics");
        if (!statsList.isArray() || statsList.isEmpty()) return Optional.empty();
        JsonNode latest = statsList.get(statsList.size() - 1);

        List<String> values = new ArrayList<>();
        latest.path("stats").forEach(v -> values.add(v.asText()));
        if (values.size() < labels.size()) return Optional.empty();

        Map<String, String> byLabel = new HashMap<>();
        for (int i = 0; i < labels.size(); i++) byLabel.put(labels.get(i), values.get(i));

        return Optional.of(new PlayerSeasonStatsRow(
                latest.path("season").path("displayName").asText(null),
                parseInt(byLabel.get("GP")),
                parseInt(byLabel.get("GS")),
                parseDouble(byLabel.get("MIN")),
                parseDouble(byLabel.get("PTS")),
                parseDouble(byLabel.get("REB")),
                parseDouble(byLabel.get("AST")),
                parseDouble(byLabel.get("STL")),
                parseDouble(byLabel.get("BLK")),
                parseDouble(byLabel.get("TO")),
                parseDouble(byLabel.get("FG%")),
                parseDouble(byLabel.get("3P%")),
                parseDouble(byLabel.get("FT%"))
        ));
    }

    private double parseDouble(String raw) {
        if (raw == null) return 0;
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double statByName(Map<String, JsonNode> statsByName, String name) {
        JsonNode stat = statsByName.get(name);
        return stat == null ? 0 : stat.path("value").asDouble(0);
    }

    private PlayerBoxScoreRow toRow(String teamAbbr, String name, boolean starter, List<String> labels, List<String> values) {
        Map<String, String> byLabel = new HashMap<>();
        for (int i = 0; i < labels.size(); i++) byLabel.put(labels.get(i), values.get(i));

        return new PlayerBoxScoreRow(
                name, teamAbbr, starter,
                byLabel.getOrDefault("MIN", "0"),
                parseInt(byLabel.get("PTS")),
                parseInt(byLabel.get("REB")),
                parseInt(byLabel.get("AST")),
                parseInt(byLabel.get("STL")),
                parseInt(byLabel.get("BLK")),
                parseInt(byLabel.get("TO")),
                parsePlusMinus(byLabel.get("+/-")),
                parseSplit(byLabel.get("FG")),
                parseSplit(byLabel.get("3PT")),
                parseSplit(byLabel.get("FT"))
        );
    }

    private JsonNode fetchWithRetry(String url) {
        HttpHeaders headers = new HttpHeaders();
        // Contre-intuitif mais vérifié à la main (curl, plusieurs User-Agent
        // testés un par un) : Akamai bloque ici un User-Agent de navigateur
        // (Chrome/Firefox), un User-Agent vide, et même le UA par défaut de
        // Java — mais laisse passer un UA de la forme "curl/x.y.z". Le 403
        // n'est donc pas du rate-limiting ponctuel, c'est un filtre sur le
        // contenu du User-Agent qui punit justement l'usurpation d'un
        // navigateur. On imite curl plutôt qu'un navigateur.
        headers.set("User-Agent", "curl/8.7.1");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class).getBody();
            } catch (Exception e) {
                log.warn("Appel ESPN échoué (tentative {}/2) : {}", attempt, e.getMessage());
                if (attempt == 2) break;
                try {
                    Thread.sleep(11_000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return null;
    }

    private int parseInt(String raw) {
        if (raw == null) return 0;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int parsePlusMinus(String raw) {
        if (raw == null || raw.isBlank()) return 0;
        return parseInt(raw.replace("+", ""));
    }

    private PlayerBoxScoreRow.ShotSplit parseSplit(String raw) {
        if (raw == null || !raw.contains("-")) return new PlayerBoxScoreRow.ShotSplit(0, 0);
        String[] parts = raw.split("-", 2);
        return new PlayerBoxScoreRow.ShotSplit(parseInt(parts[0]), parseInt(parts[1]));
    }
}
