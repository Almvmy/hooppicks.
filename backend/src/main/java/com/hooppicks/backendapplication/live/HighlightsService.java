package com.hooppicks.backendapplication.live;

import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.nba.NbaSyncService;
import com.hooppicks.backendapplication.repository.MatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Résumé vidéo officiel des matchs terminés : vidéos de la chaîne YouTube de
 * la NBA, intégrées avec le lecteur YouTube (autorisé par YouTube, contrairement
 * à une diffusion des matchs, qu'on ne fait jamais : droits NBA / diffuseurs).
 *
 * Lu dans le flux RSS public de la chaîne (sans clé ni quota), qui ne donne
 * que les 15 dernières vidéos : d'où une lecture toutes les 10 minutes, les
 * résumés arrivant une heure environ après le match au milieu de nombreux
 * extraits. Une vidéo est reliée à un match terminé depuis moins de 2 jours
 * si son titre parle de « highlights » et nomme les deux équipes, publiée
 * après le coup d'envoi ; un « full game highlights » remplace un premier
 * résumé plus court.
 */
@Service
public class HighlightsService {

    private static final Logger log = LoggerFactory.getLogger(HighlightsService.class);
    static final String FEED_URL = "https://www.youtube.com/feeds/videos.xml?channel_id=UCWJ2lWNubArHWmf3FIHbfcQ";
    private static final Duration LOOKBACK = Duration.ofDays(2);

    private static final Pattern ENTRY = Pattern.compile("<entry>(.*?)</entry>", Pattern.DOTALL);
    private static final Pattern VIDEO_ID = Pattern.compile("<yt:videoId>([^<]+)</yt:videoId>");
    private static final Pattern TITLE = Pattern.compile("<title>([^<]*)</title>");
    private static final Pattern PUBLISHED = Pattern.compile("<published>([^<]+)</published>");

    record Video(String id, String title, Instant published) {}

    private final RestTemplate restTemplate;
    private final MatchRepository matchRepository;
    private final NbaSyncService nbaSyncService;

    public HighlightsService(RestTemplate restTemplate, MatchRepository matchRepository, NbaSyncService nbaSyncService) {
        this.restTemplate = restTemplate;
        this.matchRepository = matchRepository;
        this.nbaSyncService = nbaSyncService;
    }

    /** Relie les nouveaux résumés vidéo. Renvoie le nombre de matchs mis à jour. */
    public int refresh() {
        String xml;
        try {
            xml = restTemplate.getForObject(FEED_URL, String.class);
        } catch (Exception e) {
            log.warn("Résumés vidéo : chaîne NBA injoignable ({})", e.getMessage());
            return 0;
        }
        List<Video> videos = parse(xml);
        if (videos.isEmpty()) return 0;
        int[] updated = {0};
        Instant now = Instant.now();
        // Sous le verrou de la synchro : elle enregistre les matchs en entier,
        // une copie chargée avant notre écriture l'effacerait.
        nbaSyncService.tryRunExclusive(() -> {
            for (Match match : matchRepository.findByStatusAndDateBetween(MatchStatus.FINISHED, now.minus(LOOKBACK), now)) {
                Video video = bestFor(match, videos);
                if (video == null || video.id().equals(match.getHighlightsVideoId())) continue;
                match.setHighlightsVideoId(video.id());
                match.setHighlightsTitle(video.title());
                matchRepository.save(match);
                updated[0]++;
            }
        });
        if (updated[0] > 0) log.info("Résumés vidéo : {} match(s) relié(s)", updated[0]);
        return updated[0];
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

    /** Meilleure vidéo pour ce match, ou null s'il n'y a rien de mieux que l'actuelle. */
    static Video bestFor(Match match, List<Video> videos) {
        if (match.getHomeTeam() == null || match.getAwayTeam() == null) return null;
        boolean hasFullGame = isFullGame(match.getHighlightsTitle());
        Video best = null;
        for (Video v : videos) {
            if (match.getDate() != null && v.published().isBefore(match.getDate())) continue;
            String title = v.title().toLowerCase(Locale.ROOT);
            if (!title.contains("highlight")) continue;
            if (!names(title, match.getHomeTeam().getName()) || !names(title, match.getAwayTeam().getName())) continue;
            if (match.getHighlightsVideoId() != null && (hasFullGame || !isFullGame(v.title()))) continue;
            if (best == null || (isFullGame(v.title()) && !isFullGame(best.title()))) best = v;
        }
        return best;
    }

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
