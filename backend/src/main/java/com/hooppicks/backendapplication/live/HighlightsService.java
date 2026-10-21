package com.hooppicks.backendapplication.live;

import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.espn.EspnStatsClient;
import com.hooppicks.backendapplication.nba.NbaSyncService;
import com.hooppicks.backendapplication.repository.MatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Résumé vidéo officiel des matchs terminés, intégré avec le lecteur prévu
 * pour ça (YouTube, lecteur « syndicated » d'ESPN). Jamais de diffusion des
 * matchs eux-mêmes : droits NBA / diffuseurs.
 *
 * La vidéo lisible la plus longue l'emporte (choix du propriétaire : les longs
 * résumés d'abord) : « full game highlights » de la chaîne YouTube de la NBA
 * (~10 min), sinon le résumé ESPN du match (~1 min), lu seulement quand
 * YouTube n'a rien de plus long. Un match terminé depuis moins de 2 jours est
 * revu toutes les 10 minutes : une vidéo plus longue remplace la précédente.
 *
 * YouTube : une vidéo est reliée si son titre parle de « highlights » et
 * nomme les deux équipes (mot entier), publiée après le coup d'envoi. Avec
 * une clé YouTube Data API (`youtube.api-key`, `YOUTUBE_API_KEY` en
 * production) : les 50 dernières vidéos (playlistItems, 1 unité de quota),
 * leur durée et les pays où elles sont bloquées (videos.list, 1 unité pour 50
 * vidéos). La NBA bloque des résumés complets en Côte d'Ivoire, pays des
 * joueurs (`REGION`) : ils sont écartés, et une vidéo déjà reliée qui s'avère
 * bloquée est retirée. Sans clé ou si l'API échoue : flux RSS public (15
 * dernières vidéos), durée estimée d'après le titre, pays bloqués inconnus.
 *
 * Chaînes des équipes (`TeamChannels`) : tant qu'un match n'a pas de résumé
 * d'au moins 8 minutes, on lit aussi les chaînes officielles de ses deux
 * équipes (flux RSS, sans quota) : elles publient parfois un résumé long
 * (« Extended Highlights ») lisible là où celui de la NBA est bloqué. Une
 * vidéo d'équipe compte si son titre parle de « highlights » et nomme
 * l'adversaire ; les vidéos d'un seul joueur (« … (19 Points) ») sont écartées.
 */
@Service
public class HighlightsService {

    private static final Logger log = LoggerFactory.getLogger(HighlightsService.class);
    static final String FEED_URL = "https://www.youtube.com/feeds/videos.xml?channel_id=UCWJ2lWNubArHWmf3FIHbfcQ";
    // Vidéos mises en ligne par la chaîne = playlist « UU » + identifiant de chaîne sans « UC ».
    static final String API_URL = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=UUWJ2lWNubArHWmf3FIHbfcQ&key=";
    static final String VIDEOS_URL = "https://www.googleapis.com/youtube/v3/videos?part=contentDetails&id=";
    /** Pays des joueurs (GMT, Côte d'Ivoire), code ISO 3166-1. */
    static final String REGION = "CI";
    static final String YOUTUBE = "youtube";
    static final String ESPN = "espn";
    private static final Duration LOOKBACK = Duration.ofDays(2);
    // Le résumé ESPN dure environ une minute : inutile de le chercher quand
    // une vidéo plus longue est déjà là.
    private static final int ESPN_WORTH_CHECKING = 180;
    // Au-delà, le résumé est assez long : plus besoin de lire les chaînes des équipes.
    private static final int LONG_ENOUGH = 480;
    // Une vidéo publiée plus tard parle d'un autre match (même affiche, rare).
    private static final Duration PUBLISHED_WITHIN = Duration.ofHours(36);
    private static final Pattern PLAYER_CLIP = Pattern.compile(
            "\\(\\s*\\d+\\s*(pts|points?)|\\d+\\s+points|#shorts|quarter|half(time)?\\s+highlights", Pattern.CASE_INSENSITIVE);

    private static final Pattern ENTRY = Pattern.compile("<entry>(.*?)</entry>", Pattern.DOTALL);
    private static final Pattern VIDEO_ID = Pattern.compile("<yt:videoId>([^<]+)</yt:videoId>");
    private static final Pattern TITLE = Pattern.compile("<title>([^<]*)</title>");
    private static final Pattern PUBLISHED = Pattern.compile("<published>([^<]+)</published>");

    /** team : sigle de l'équipe dont c'est la chaîne, null pour la chaîne de la NBA. */
    record Video(String id, String title, Instant published, String team) {
        Video(String id, String title, Instant published) {
            this(id, title, published, null);
        }
    }

    /** Ce qu'on sait d'un match terminé, lu en base avant les appels réseau. */
    record Candidate(String matchId, String home, String away, String homeAbbr, String awayAbbr, Instant date,
                     String espnEventId, String videoId, String provider, int seconds) {
        static Candidate of(Match m) {
            String provider = m.getHighlightsProvider() == null ? YOUTUBE : m.getHighlightsProvider();
            int seconds = m.getHighlightsSeconds() != null ? m.getHighlightsSeconds()
                    : m.getHighlightsVideoId() == null ? 0 : estimate(m.getHighlightsTitle());
            return new Candidate(m.getId(), m.getHomeTeam() == null ? null : m.getHomeTeam().getName(),
                    m.getAwayTeam() == null ? null : m.getAwayTeam().getName(),
                    m.getHomeTeam() == null ? null : m.getHomeTeam().getAbbreviation(),
                    m.getAwayTeam() == null ? null : m.getAwayTeam().getAbbreviation(),
                    m.getDate(), m.getEspnEventId(), m.getHighlightsVideoId(), provider, seconds);
        }
    }

    /** Vidéo choisie pour un match ; videoId null = retirer celle en place (bloquée). */
    record Choice(String videoId, String title, String provider, int seconds) {}

    private final RestTemplate restTemplate;
    private final MatchRepository matchRepository;
    private final NbaSyncService nbaSyncService;
    private final String apiKey;
    private final EspnStatsClient espn;
    // Les équipes d'un match sont chargées à la demande : lues hors
    // transaction, Hibernate échouait (LazyInitializationException).
    private final Consumer<Runnable> inTransaction;

    @Autowired
    public HighlightsService(RestTemplate restTemplate, MatchRepository matchRepository, NbaSyncService nbaSyncService,
                             PlatformTransactionManager transactionManager, EspnStatsClient espn,
                             @Value("${youtube.api-key:}") String apiKey) {
        this(restTemplate, matchRepository, nbaSyncService, apiKey, espn,
                work -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> work.run()));
    }

    /** Pour les tests : pas de vraie transaction. */
    HighlightsService(RestTemplate restTemplate, MatchRepository matchRepository, NbaSyncService nbaSyncService,
                      String apiKey, EspnStatsClient espn) {
        this(restTemplate, matchRepository, nbaSyncService, apiKey, espn, Runnable::run);
    }

    HighlightsService(RestTemplate restTemplate, MatchRepository matchRepository, NbaSyncService nbaSyncService, String apiKey) {
        this(restTemplate, matchRepository, nbaSyncService, apiKey, null);
    }

    HighlightsService(RestTemplate restTemplate, MatchRepository matchRepository, NbaSyncService nbaSyncService) {
        this(restTemplate, matchRepository, nbaSyncService, "");
    }

    private HighlightsService(RestTemplate restTemplate, MatchRepository matchRepository, NbaSyncService nbaSyncService,
                              String apiKey, EspnStatsClient espn, Consumer<Runnable> inTransaction) {
        this.restTemplate = restTemplate;
        this.matchRepository = matchRepository;
        this.nbaSyncService = nbaSyncService;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.espn = espn;
        this.inTransaction = inTransaction;
    }

    /** Relie ou remplace les résumés vidéo. Renvoie le nombre de matchs mis à jour. */
    public int refresh() {
        Instant now = Instant.now();
        List<Candidate> candidates = new ArrayList<>();
        inTransaction.accept(() -> matchRepository.findByStatusAndDateBetween(MatchStatus.FINISHED, now.minus(LOOKBACK), now)
                .forEach(m -> candidates.add(Candidate.of(m))));

        List<Video> videos = new ArrayList<>(youtubeVideos());
        Set<String> teams = new java.util.LinkedHashSet<>();
        for (Candidate c : candidates) {
            if (c.seconds() >= LONG_ENOUGH) continue;
            if (TeamChannels.has(c.homeAbbr())) teams.add(c.homeAbbr());
            if (TeamChannels.has(c.awayAbbr())) teams.add(c.awayAbbr());
        }
        for (String team : teams) videos.addAll(teamVideos(team));

        Map<String, Integer> seconds = new HashMap<>();
        Set<String> blocked = new HashSet<>();
        if (!apiKey.isEmpty() && !videos.isEmpty()) readDetails(videos, blocked, seconds);
        List<Video> playable = videos.stream().filter(v -> !blocked.contains(v.id())).toList();

        // Appels ESPN hors verrou et hors transaction.
        Map<String, Choice> choices = new HashMap<>();
        for (Candidate c : candidates) {
            Choice choice = choose(c, playable, seconds, blocked);
            if (choice != null) choices.put(c.matchId(), choice);
        }
        if (choices.isEmpty()) return 0;

        int[] updated = {0};
        // Sous le verrou de la synchro : elle enregistre les matchs en entier,
        // une copie chargée avant notre écriture l'effacerait.
        nbaSyncService.tryRunExclusive(() -> inTransaction.accept(() -> choices.forEach((matchId, choice) ->
                matchRepository.findById(matchId).ifPresent(match -> {
                    match.setHighlightsVideoId(choice.videoId());
                    match.setHighlightsTitle(choice.title());
                    match.setHighlightsProvider(choice.provider());
                    match.setHighlightsSeconds(choice.videoId() == null ? null : choice.seconds());
                    matchRepository.save(match);
                    updated[0]++;
                }))));
        if (updated[0] > 0) log.info("Résumés vidéo : {} match(s) mis à jour", updated[0]);
        return updated[0];
    }

    /** Ce qui change pour ce match, ou null s'il garde sa vidéo. */
    Choice choose(Candidate c, List<Video> playable, Map<String, Integer> seconds, Set<String> blocked) {
        boolean currentBlocked = c.videoId() != null && YOUTUBE.equals(c.provider()) && blocked.contains(c.videoId());
        int current = currentBlocked ? 0 : c.seconds();
        Choice best = null;
        Video yt = longestFor(c, playable, seconds);
        if (yt != null && !yt.id().equals(c.videoId())) {
            int ytSeconds = seconds.getOrDefault(yt.id(), estimate(yt.title()));
            if (ytSeconds > current) best = new Choice(yt.id(), yt.title(), YOUTUBE, ytSeconds);
        }
        int bestSeconds = best != null ? best.seconds() : current;
        if (espn != null && c.espnEventId() != null && bestSeconds < ESPN_WORTH_CHECKING) {
            EspnStatsClient.EspnVideo e = espn.fetchGameHighlights(c.espnEventId()).orElse(null);
            int espnSeconds = e == null ? 0 : Math.max(e.seconds(), 1);
            if (e != null && !e.id().equals(c.videoId()) && espnSeconds > bestSeconds) {
                best = new Choice(e.id(), e.headline(), ESPN, espnSeconds);
            }
        }
        if (best != null) return best;
        return currentBlocked ? new Choice(null, null, null, 0) : null;
    }

    private List<Video> youtubeVideos() {
        List<Video> videos = apiKey.isEmpty() ? List.of() : fromApi();
        if (!videos.isEmpty()) return videos;
        try {
            return parse(restTemplate.getForObject(FEED_URL, String.class));
        } catch (Exception e) {
            log.warn("Résumés vidéo : chaîne NBA injoignable ({})", e.getMessage());
            return List.of();
        }
    }

    private List<Video> teamVideos(String team) {
        try {
            String xml = restTemplate.getForObject(TeamChannels.feedUrl(TeamChannels.BY_ABBREVIATION.get(team).channelId()), String.class);
            return parse(xml).stream().map(v -> new Video(v.id(), v.title(), v.published(), team)).toList();
        } catch (Exception e) {
            log.warn("Résumés vidéo : chaîne {} injoignable ({})", team, e.getClass().getSimpleName());
            return List.of();
        }
    }

    private List<Video> fromApi() {
        try {
            return parseApi(restTemplate.getForObject(API_URL + apiKey, JsonNode.class));
        } catch (Exception e) {
            // Jamais le message : il peut contenir l'adresse appelée, donc la clé.
            log.warn("Résumés vidéo : API YouTube en échec ({}), retour au flux RSS", e.getClass().getSimpleName());
            return List.of();
        }
    }

    // Pays bloqués et durée des vidéos. En cas d'échec, rien n'est écarté
    // (mieux vaut une vidéo parfois bloquée que plus de vidéo du tout).
    private void readDetails(List<Video> videos, Set<String> blocked, Map<String, Integer> seconds) {
        List<String> ids = videos.stream().map(Video::id).distinct().toList();
        // videos.list : 50 vidéos au plus par appel, 1 unité de quota chacun.
        for (int i = 0; i < ids.size(); i += 50) {
            String batch = String.join(",", ids.subList(i, Math.min(ids.size(), i + 50)));
            try {
                JsonNode response = restTemplate.getForObject(VIDEOS_URL + batch + "&key=" + apiKey, JsonNode.class);
                blocked.addAll(parseBlocked(response, REGION));
                seconds.putAll(parseDurations(response));
            } catch (Exception e) {
                log.warn("Résumés vidéo : détails des vidéos illisibles ({})", e.getClass().getSimpleName());
            }
        }
    }

    static Set<String> parseBlocked(JsonNode response, String region) {
        Set<String> blocked = new HashSet<>();
        if (response == null) return blocked;
        for (JsonNode item : response.path("items")) {
            String id = item.path("id").asText("");
            JsonNode restriction = item.path("contentDetails").path("regionRestriction");
            if (id.isEmpty() || restriction.isMissingNode()) continue;
            boolean notAllowed = restriction.has("allowed") && !contains(restriction.path("allowed"), region);
            if (notAllowed || contains(restriction.path("blocked"), region)) blocked.add(id);
        }
        return blocked;
    }

    static Map<String, Integer> parseDurations(JsonNode response) {
        Map<String, Integer> seconds = new HashMap<>();
        if (response == null) return seconds;
        for (JsonNode item : response.path("items")) {
            String id = item.path("id").asText("");
            String iso = item.path("contentDetails").path("duration").asText("");
            if (id.isEmpty() || iso.isEmpty()) continue;
            try {
                seconds.put(id, (int) Duration.parse(iso).getSeconds());
            } catch (Exception ignored) {
                // Durée illisible : estimée d'après le titre.
            }
        }
        return seconds;
    }

    private static boolean contains(JsonNode countries, String region) {
        for (JsonNode c : countries) if (region.equalsIgnoreCase(c.asText(""))) return true;
        return false;
    }

    static List<Video> parseApi(JsonNode response) {
        List<Video> videos = new ArrayList<>();
        if (response == null) return videos;
        for (JsonNode item : response.path("items")) {
            JsonNode snippet = item.path("snippet");
            String id = snippet.path("resourceId").path("videoId").asText("");
            String title = snippet.path("title").asText("");
            String published = snippet.path("publishedAt").asText("");
            if (id.isEmpty() || title.isEmpty() || published.isEmpty()) continue;
            try {
                videos.add(new Video(id, title.trim(), Instant.parse(published)));
            } catch (Exception ignored) {
                // Date illisible : vidéo ignorée.
            }
        }
        return videos;
    }

    static List<Video> parse(String xml) {
        List<Video> videos = new ArrayList<>();
        if (xml == null) return videos;
        Matcher entries = ENTRY.matcher(xml);
        while (entries.find()) {
            String entry = entries.group(1);
            Matcher id = VIDEO_ID.matcher(entry);
            Matcher title = TITLE.matcher(entry);
            Matcher published = PUBLISHED.matcher(entry);
            if (!id.find() || !title.find() || !published.find()) continue;
            try {
                videos.add(new Video(id.group(1).trim(), unescape(title.group(1)), Instant.parse(published.group(1).replace("+00:00", "Z"))));
            } catch (Exception ignored) {
                // Date illisible : vidéo ignorée.
            }
        }
        return videos;
    }

    /** Résumé YouTube le plus long pour ce match, quel que soit celui en place. */
    static Video longestFor(Candidate c, List<Video> videos, Map<String, Integer> seconds) {
        if (c.home() == null || c.away() == null) return null;
        Video best = null;
        int bestSeconds = -1;
        for (Video v : videos) {
            if (!isAbout(c, v)) continue;
            int s = seconds.getOrDefault(v.id(), estimate(v.title()));
            if (s > bestSeconds) {
                best = v;
                bestSeconds = s;
            }
        }
        return best;
    }

    /** Résumé de ce match : chaîne NBA (les deux équipes nommées) ou chaîne d'une des deux équipes (l'adversaire nommé). */
    static boolean isAbout(Candidate c, Video v) {
        if (c.date() != null && (v.published().isBefore(c.date()) || v.published().isAfter(c.date().plus(PUBLISHED_WITHIN)))) {
            return false;
        }
        String title = v.title().toLowerCase(Locale.ROOT);
        if (!title.contains("highlight") || PLAYER_CLIP.matcher(v.title()).find()) return false;
        List<String> home = TeamChannels.names(c.homeAbbr(), c.home());
        List<String> away = TeamChannels.names(c.awayAbbr(), c.away());
        if (v.team() == null) return mentions(title, home) && mentions(title, away);
        if (v.team().equals(c.homeAbbr())) return mentions(title, away);
        if (v.team().equals(c.awayAbbr())) return mentions(title, home);
        return false;
    }

    private static boolean mentions(String title, List<String> names) {
        return names.stream().anyMatch(n -> names(title, n));
    }

    /** Vidéo YouTube plus longue que celle en place, ou null (durées estimées d'après le titre). */
    static Video bestFor(Match match, List<Video> videos) {
        Candidate c = Candidate.of(match);
        Video v = longestFor(c, videos, Map.of());
        return v != null && !v.id().equals(c.videoId()) && estimate(v.title()) > c.seconds() ? v : null;
    }

    // Sans l'API (flux RSS seul), pas de durée : un « full game highlights »
    // dure une dizaine de minutes, les autres résumés bien moins.
    static int estimate(String title) {
        return isFullGame(title) ? 600 : 120;
    }

    // « Full game highlights » de la NBA. Pas « Extended highlights » : celui
    // des Celtics pour Detroit-Boston ne dure que 2 min (constaté).
    private static boolean isFullGame(String title) {
        return title != null && title.toLowerCase(Locale.ROOT).contains("full game");
    }

    // Mot entier : « Nets » ne doit pas trouver « Hornets ».
    private static boolean names(String title, String teamName) {
        if (teamName == null || teamName.isBlank()) return false;
        return Pattern.compile("(?<![a-z0-9])" + Pattern.quote(teamName.toLowerCase(Locale.ROOT)) + "(?![a-z0-9])").matcher(title).find();
    }

    private static String unescape(String s) {
        return s.replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">").trim();
    }
}
