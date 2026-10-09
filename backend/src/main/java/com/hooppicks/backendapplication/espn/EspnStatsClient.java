package com.hooppicks.backendapplication.espn;

import tools.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Client pour l'API (non officielle, non documentée) d'ESPN : pas de clé,
 * pas de contrat de stabilité garanti, mais c'est la seule source qui donne
 * la feuille de match par joueur gratuitement (balldontlie la verrouille
 * derrière un tier payant). Akamai (le CDN d'ESPN) bloque en 403 tout
 * User-Agent qui ressemble à un navigateur, voir le commentaire sur
 * fetchWithRetry : le vrai correctif est là, pas dans les tentatives. On
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
    private static final String NEWS_URL =
            "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/news?limit=%d";
    private static final String ATHLETE_GAMELOG_URL =
            "https://site.web.api.espn.com/apis/common/v3/sports/basketball/nba/athletes/%s/gamelog";

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

    /** Sens inverse de {@link #toEspnAbbreviation} : sigle ESPN -> sigle balldontlie (le nôtre). */
    public static String fromEspnAbbreviation(String espnAbbreviation) {
        return ABBREVIATION_OVERRIDES.entrySet().stream()
                .filter(e -> e.getValue().equals(espnAbbreviation))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(espnAbbreviation);
    }

    /**
     * Dernières actualités NBA (API JSON, bien plus riche que le flux RSS :
     * photo, équipes étiquetées par ESPN, distinction article/vidéo). Liste
     * vide si ESPN ne répond pas : l'appelant garde alors son cache ou se
     * replie sur le RSS.
     */
    public List<EspnNewsRow> fetchNews(int limit) {
        JsonNode root = fetchWithRetry(String.format(NEWS_URL, limit));
        if (root == null) return List.of();

        List<EspnNewsRow> rows = new ArrayList<>();
        for (JsonNode article : root.path("articles")) {
            String headline = article.path("headline").asText(null);
            String link = article.path("links").path("web").path("href").asText(null);
            if (headline == null || link == null) continue;

            String published = article.path("published").asText(null);
            Instant publishedAt;
            try {
                publishedAt = published != null ? Instant.parse(published) : Instant.now();
            } catch (Exception e) {
                publishedAt = Instant.now();
            }

            // La photo "header" (16:9, ~1296 px) en priorité, sinon la première disponible.
            String imageUrl = null;
            for (JsonNode image : article.path("images")) {
                if (imageUrl == null || "header".equals(image.path("type").asText())) {
                    imageUrl = image.path("url").asText(null);
                }
                if ("header".equals(image.path("type").asText())) break;
            }

            List<String> teams = new ArrayList<>();
            for (JsonNode category : article.path("categories")) {
                if (!"team".equals(category.path("type").asText())) continue;
                String espnAbbr = category.path("team").path("abbreviation").asText(null);
                if (espnAbbr != null && !teams.contains(fromEspnAbbreviation(espnAbbr))) {
                    teams.add(fromEspnAbbreviation(espnAbbr));
                }
            }

            rows.add(new EspnNewsRow(
                    headline,
                    article.path("description").asText(null),
                    link,
                    publishedAt,
                    imageUrl,
                    teams,
                    "Media".equals(article.path("type").asText())
            ));
        }
        return rows;
    }

    /**
     * Tous les matchs d'une journée ESPN (journée à l'heure de la côte Est,
     * comme le calendrier NBA) : sert à relier nos matchs à leur event ESPN
     * (les deux APIs n'ont aucun ID en commun, seuls date + équipes les
     * relient), à lire leur phase, et à créer les matchs de présaison que
     * balldontlie ne connaît pas. Optional vide = ESPN injoignable, à
     * distinguer d'une journée sans match (liste vide).
     */
    public Optional<List<EspnGameRow>> fetchScoreboard(LocalDate date) {
        JsonNode root = fetchWithRetry(String.format(SCOREBOARD_URL, date.format(DateTimeFormatter.BASIC_ISO_DATE)));
        if (root == null) return Optional.empty();

        List<EspnGameRow> rows = new ArrayList<>();
        for (JsonNode event : root.path("events")) {
            JsonNode competition = event.path("competitions").path(0);
            String home = null;
            String away = null;
            Integer homeScore = null;
            Integer awayScore = null;
            for (JsonNode competitor : competition.path("competitors")) {
                String abbr = fromEspnAbbreviation(competitor.path("team").path("abbreviation").asText());
                Integer score = competitor.hasNonNull("score") ? parseInt(competitor.path("score").asText()) : null;
                if ("home".equals(competitor.path("homeAway").asText())) {
                    home = abbr;
                    homeScore = score;
                } else {
                    away = abbr;
                    awayScore = score;
                }
            }
            JsonNode statusType = competition.path("status").path("type");
            String series = competition.path("series").path("summary").asText("");
            rows.add(new EspnGameRow(
                    event.path("id").asText(),
                    parseEventDate(event.path("date").asText("")),
                    home,
                    away,
                    homeScore,
                    awayScore,
                    statusType.path("state").asText(""),
                    statusType.path("completed").asBoolean(false),
                    event.path("season").path("type").asInt(0),
                    competition.path("type").path("abbreviation").asText(""),
                    competition.path("notes").path(0).path("headline").asText(""),
                    series.isBlank() ? null : series
            ));
        }
        return Optional.of(rows);
    }

    /**
     * Statut en direct de tous les matchs d'une journée ESPN : quart-temps,
     * chrono, score. Même appel que {@link #fetchScoreboard}, lu autrement :
     * gardé à part pour ne rien changer à la liaison des matchs. Optional
     * vide = ESPN injoignable.
     */
    public Optional<List<EspnLiveGame>> fetchLiveScoreboard(LocalDate date) {
        JsonNode root = fetchWithRetry(String.format(SCOREBOARD_URL, date.format(DateTimeFormatter.BASIC_ISO_DATE)));
        if (root == null) return Optional.empty();
        List<EspnLiveGame> games = new ArrayList<>();
        for (JsonNode event : root.path("events")) {
            JsonNode competition = event.path("competitions").path(0);
            games.add(liveStatus(event.path("id").asText(), competition));
        }
        return Optional.of(games);
    }

    /**
     * Résumé d'un match pour le direct : statut, score par quart-temps, stats
     * des équipes, feuille de match du moment. Optional vide = ESPN
     * injoignable ou réponse inexploitable.
     */
    public Optional<EspnLiveSummary> fetchLiveSummary(String eventId) {
        JsonNode root = fetchWithRetry(String.format(SUMMARY_URL, eventId));
        if (root == null) return Optional.empty();
        JsonNode competition = root.path("header").path("competitions").path(0);
        if (competition.isMissingNode()) return Optional.empty();

        Map<String, Map<String, String>> statsByTeam = new HashMap<>();
        for (JsonNode team : root.path("boxscore").path("teams")) {
            Map<String, String> stats = new HashMap<>();
            for (JsonNode stat : team.path("statistics")) {
                stats.put(stat.path("name").asText(), stat.path("displayValue").asText(""));
            }
            statsByTeam.put(fromEspnAbbreviation(team.path("team").path("abbreviation").asText()), stats);
        }

        EspnLiveSummary.Side home = null;
        EspnLiveSummary.Side away = null;
        Map<String, String> abbrByTeamId = new HashMap<>();
        for (JsonNode competitor : competition.path("competitors")) {
            String abbr = fromEspnAbbreviation(competitor.path("team").path("abbreviation").asText());
            abbrByTeamId.put(competitor.path("team").path("id").asText(), abbr);
            List<Integer> lines = new ArrayList<>();
            competitor.path("linescores").forEach(l -> lines.add(parseInt(l.path("displayValue").asText(null))));
            JsonNode fouls = competitor.path("fouls");
            String bonus = fouls.path("bonusState").asText("");
            EspnLiveSummary.Side side = new EspnLiveSummary.Side(abbr, lines, statsByTeam.getOrDefault(abbr, Map.of()),
                    competitor.path("possession").asBoolean(false),
                    fouls.has("teamFoulsCurrent") ? fouls.path("teamFoulsCurrent").asInt() : null,
                    bonus.isBlank() || "NONE".equalsIgnoreCase(bonus) ? null : bonus);
            if ("home".equals(competitor.path("homeAway").asText())) home = side;
            else away = side;
        }
        if (home == null || away == null) return Optional.empty();

        // Sigles à la sauce balldontlie (« NYK », pas « NY ») : c'est avec eux
        // que l'app range les joueurs par équipe.
        List<PlayerBoxScoreRow> players = parseBoxScore(root).stream()
                .map(r -> new PlayerBoxScoreRow(r.playerName(), fromEspnAbbreviation(r.teamAbbreviation()), r.starter(),
                        r.minutes(), r.points(), r.rebounds(), r.assists(), r.steals(), r.blocks(), r.turnovers(),
                        r.plusMinus(), r.fieldGoals(), r.threePoints(), r.freeThrows()))
                .toList();
        return Optional.of(new EspnLiveSummary(liveStatus(eventId, competition),
                home, away, players, parseDetails(root, abbrByTeamId)));
    }

    // Au-delà, ce sont des tirs désespérés depuis l'autre moitié du terrain :
    // hors de la carte des tirs.
    private static final double SHOT_MAX_Y = 43;
    private static final int MAX_WIN_POINTS = 200;

    /**
     * Courbe de probabilité de victoire, tirs et temps forts, lus dans les
     * actions du match. Une action mal formée est ignorée, jamais bloquante.
     */
    private EspnLiveSummary.Details parseDetails(JsonNode root, Map<String, String> abbrByTeamId) {
        Map<String, String> nameByAthleteId = new HashMap<>();
        for (JsonNode teamBlock : root.path("boxscore").path("players")) {
            for (JsonNode a : teamBlock.path("statistics").path(0).path("athletes")) {
                nameByAthleteId.put(a.path("athlete").path("id").asText(), a.path("athlete").path("displayName").asText());
            }
        }

        Map<String, Integer> elapsedByPlay = new HashMap<>();
        List<EspnLiveSummary.Shot> shots = new ArrayList<>();
        List<EspnLiveSummary.KeyPlay> keyPlays = new ArrayList<>();
        for (JsonNode play : root.path("plays")) {
            int period = play.path("period").path("number").asInt(0);
            String clock = play.path("clock").path("displayValue").asText("");
            elapsedByPlay.put(play.path("id").asText(), elapsedSeconds(period, clock));
            String type = play.path("type").path("text").asText("");
            String team = abbrByTeamId.get(play.path("team").path("id").asText());
            String player = nameByAthleteId.get(play.path("participants").path(0).path("athlete").path("id").asText());

            boolean freeThrow = type.startsWith("Free Throw");
            JsonNode coordinate = play.path("coordinate");
            double x = coordinate.path("x").asDouble(-1);
            double y = coordinate.path("y").asDouble(-1);
            if (play.path("shootingPlay").asBoolean(false) && !freeThrow && x >= 0 && x <= 50 && y <= SHOT_MAX_Y) {
                boolean made = play.path("scoringPlay").asBoolean(false);
                shots.add(new EspnLiveSummary.Shot(x, y, made, play.path("pointsAttempted").asInt(2), team, player));
            }

            String kind = null;
            if (play.path("scoringPlay").asBoolean(false)) kind = shotKind(type, play.path("scoreValue").asInt());
            else if ("End Period".equals(type)) kind = "end_period";
            else if ("End Game".equals(type)) kind = "end_game";
            if (kind != null) {
                keyPlays.add(new EspnLiveSummary.KeyPlay(period, clock, kind, play.path("scoreValue").asInt(0), team,
                        player, play.path("awayScore").asInt(0), play.path("homeScore").asInt(0)));
            }
        }

        List<EspnLiveSummary.WinPoint> win = new ArrayList<>();
        for (JsonNode point : root.path("winprobability")) {
            Integer elapsed = elapsedByPlay.get(point.path("playId").asText());
            if (elapsed != null) win.add(new EspnLiveSummary.WinPoint(elapsed, point.path("homeWinPercentage").asDouble()));
        }
        // ESPN insère parfois une action corrigée après coup : la courbe
        // revenait en arrière dans le temps. Tri stable par temps de jeu.
        win.sort(java.util.Comparator.comparingInt(EspnLiveSummary.WinPoint::elapsedSeconds));
        return new EspnLiveSummary.Details(downsample(win), shots, keyPlays);
    }

    /** Secondes de jeu écoulées : quarts-temps de 12 min, prolongations de 5 min. */
    static int elapsedSeconds(int period, String clock) {
        if (period <= 0) return 0;
        int remaining;
        String[] parts = clock.split(":");
        try {
            remaining = parts.length == 2
                    ? Integer.parseInt(parts[0]) * 60 + (int) Double.parseDouble(parts[1])
                    : (int) Double.parseDouble(clock);
        } catch (NumberFormatException e) {
            remaining = 0;
        }
        int before = period <= 4 ? (period - 1) * 720 : 4 * 720 + (period - 5) * 300;
        int length = period <= 4 ? 720 : 300;
        return before + Math.max(0, length - remaining);
    }

    private static String shotKind(String type, int points) {
        if (type.startsWith("Free Throw")) return "free_throw";
        if (points == 3) return "three";
        if (type.contains("Alley Oop")) return "alley_oop";
        if (type.contains("Dunk")) return "dunk";
        if (type.contains("Layup") || type.contains("Finger Roll")) return "layup";
        if (type.contains("Hook")) return "hook";
        return "jumper";
    }

    // Une courbe lisible n'a pas besoin des ~500 actions : on en garde au plus
    // MAX_WIN_POINTS, à intervalles réguliers, la dernière toujours comprise.
    private static List<EspnLiveSummary.WinPoint> downsample(List<EspnLiveSummary.WinPoint> points) {
        if (points.size() <= MAX_WIN_POINTS) return points;
        List<EspnLiveSummary.WinPoint> kept = new ArrayList<>();
        double step = (double) (points.size() - 1) / (MAX_WIN_POINTS - 1);
        for (int i = 0; i < MAX_WIN_POINTS; i++) kept.add(points.get((int) Math.round(i * step)));
        return kept;
    }

    private EspnLiveGame liveStatus(String eventId, JsonNode competition) {
        Integer homeScore = null;
        Integer awayScore = null;
        for (JsonNode competitor : competition.path("competitors")) {
            Integer score = competitor.hasNonNull("score") ? parseInt(competitor.path("score").asText()) : null;
            if ("home".equals(competitor.path("homeAway").asText())) homeScore = score;
            else awayScore = score;
        }
        JsonNode status = competition.path("status");
        return new EspnLiveGame(eventId, status.path("type").path("state").asText(""),
                status.path("period").asInt(0), status.path("displayClock").asText(""),
                status.path("type").path("detail").asText(""), homeScore, awayScore);
    }

    // ESPN écrit ses dates sans secondes ("2026-10-05T23:00Z"), format
    // qu'Instant.parse refuse mais qu'OffsetDateTime accepte.
    private static Instant parseEventDate(String raw) {
        try {
            return OffsetDateTime.parse(raw).toInstant();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Feuille de match complète (les deux équipes) pour un event ESPN déjà
     * identifié. Une ligne "athlete" ignorée (DNP, données incomplètes) ne
     * fait pas échouer le reste : on récupère ce qu'on peut plutôt que rien.
     */
    public List<PlayerBoxScoreRow> fetchBoxScore(String eventId) {
        JsonNode root = fetchWithRetry(String.format(SUMMARY_URL, eventId));
        if (root == null) return List.of();
        return parseBoxScore(root);
    }

    private List<PlayerBoxScoreRow> parseBoxScore(JsonNode root) {
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
     * majuscules : vérifié en direct, pas une supposition.
     */
    public List<EspnRosterRow> fetchRoster(String balldontlieAbbreviation) {
        String espnAbbr = toEspnAbbreviation(balldontlieAbbreviation).toLowerCase();
        JsonNode root = fetchWithRetry(String.format(ROSTER_URL, espnAbbr));
        if (root == null) return List.of();

        List<EspnRosterRow> result = new ArrayList<>();
        for (JsonNode athlete : root.path("athletes")) {
            // Le tableau "injuries" ne contient qu'une entrée à la fois côté
            // ESPN (le statut courant), pas un historique : la première (et
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
     * conférence) des 30 équipes : un seul appel pour toute la ligue,
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
     * Moyennes saison d'un joueur, à partir de son id athlète ESPN : celui
     * déjà stocké sur RosterPlayer (cf. EspnRosterService), pas besoin de
     * rapprocher par nom. On prend la dernière entrée de la catégorie
     * "averages" : les saisons y sont dans l'ordre chronologique, donc la
     * plus récente est toujours en dernier (vérifié en direct sur un joueur
     * avec un historique multi-saisons). Si le joueur n'a pas encore joué
     * cette saison (blessure, recrue en attente), c'est alors la dernière
     * saison jouée qui ressort : le seasonLabel renvoyé permet à l'appelant
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
                parseDouble(byLabel.get("FT%")),
                // « 3PT » vaut « réussis-tentés » par match (« 2.6-7.1 ») : on garde les réussis.
                parseDouble(madePart(byLabel.get("3PT")))
        ));
    }

    /**
     * Les 5 derniers matchs joués par un joueur (forme récente) : saison la
     * plus récente uniquement (seasonTypes[0]), qui regroupe elle-même les
     * matchs par mois (categories[]) plutôt qu'en une seule liste plate. Le
     * détail par match (date, adversaire, résultat) est dans un dictionnaire
     * "events" séparé côté ESPN, à corréler par eventId avec la ligne de
     * stats : les deux ne sont pas au même endroit dans la réponse.
     */
    public List<PlayerRecentGameRow> fetchRecentGames(String espnAthleteId) {
        JsonNode root = fetchWithRetry(String.format(ATHLETE_GAMELOG_URL, espnAthleteId));
        if (root == null) return List.of();

        List<String> labels = new ArrayList<>();
        root.path("labels").forEach(l -> labels.add(l.asText()));
        if (labels.isEmpty()) return List.of();

        JsonNode seasonTypes = root.path("seasonTypes");
        if (!seasonTypes.isArray() || seasonTypes.isEmpty()) return List.of();
        JsonNode latestSeason = seasonTypes.get(0);

        JsonNode eventsById = root.path("events");
        List<PlayerRecentGameRow> result = new ArrayList<>();

        for (JsonNode category : latestSeason.path("categories")) {
            for (JsonNode eventStat : category.path("events")) {
                String eventId = eventStat.path("eventId").asText(null);
                if (eventId == null) continue;
                JsonNode eventInfo = eventsById.path(eventId);
                if (eventInfo.isMissingNode()) continue;

                List<String> values = new ArrayList<>();
                eventStat.path("stats").forEach(v -> values.add(v.asText()));
                if (values.size() < labels.size()) continue;

                Map<String, String> byLabel = new HashMap<>();
                for (int i = 0; i < labels.size(); i++) byLabel.put(labels.get(i), values.get(i));

                result.add(new PlayerRecentGameRow(
                        eventInfo.path("gameDate").asText(null),
                        eventInfo.path("opponent").path("abbreviation").asText(null),
                        eventInfo.path("gameResult").asText(null),
                        eventInfo.path("score").asText(null),
                        byLabel.getOrDefault("MIN", "0"),
                        parseInt(byLabel.get("PTS")),
                        parseInt(byLabel.get("REB")),
                        parseInt(byLabel.get("AST"))
                ));
            }
        }

        result.sort((a, b) -> {
            if (a.date() == null || b.date() == null) return 0;
            return b.date().compareTo(a.date()); // ISO-8601 se compare lexicographiquement dans l'ordre chronologique
        });
        return result.stream().limit(5).toList();
    }

    private static String madePart(String madeAttempted) {
        return madeAttempted == null ? null : madeAttempted.split("-", 2)[0];
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
        // Java : mais laisse passer un UA de la forme "curl/x.y.z". Le 403
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
