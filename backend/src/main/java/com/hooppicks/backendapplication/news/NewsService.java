package com.hooppicks.backendapplication.news;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);
    private static final String FEED_URL = "https://www.espn.com/espn/rss/nba/news";
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);

    // ESPN étiquette toujours ses pubDate "EST" (UTC-5), y compris en plein
    // été quand l'heure réelle de l'Est américain est EDT (UTC-4) : un quirk
    // du flux jamais corrigé côté ESPN. Se fier à l'abréviation décale donc
    // tout d'1h en heure d'été (articles affichés dans le futur). On ignore
    // l'abréviation et on interprète la date/heure locale comme un vrai
    // horaire America/New_York, qui applique lui-même la bonne règle EST/EDT
    // selon le calendrier.
    private static final Pattern PUB_DATE_PATTERN =
            Pattern.compile("^(.*\\d{2}:\\d{2}:\\d{2})\\s+[A-Z]{2,4}$");
    private static final DateTimeFormatter PUB_DATE_LOCAL_FORMAT =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss", java.util.Locale.US);
    private static final ZoneId EASTERN = ZoneId.of("America/New_York");

    private final RestTemplate restTemplate;
    private final DeepLService deepLService;

    private volatile List<NewsItemDto> cache = List.of();
    private volatile Instant cachedAt = Instant.EPOCH;
    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    public NewsService(RestTemplate restTemplate, DeepLService deepLService) {
        this.restTemplate = restTemplate;
        this.deepLService = deepLService;
    }

    /**
     * Rafraîchit depuis ESPN au plus toutes les {@link #CACHE_TTL} : sinon
     * chaque visite de la page actus ferait un aller-retour externe. Si ESPN
     * est indisponible, on garde le dernier cache connu (même périmé) plutôt
     * que de vider la page.
     */
    public List<NewsItemDto> fetchLatest() {
        if (Duration.between(cachedAt, Instant.now()).compareTo(CACHE_TTL) < 0) {
            return cache;
        }

        // Un seul appelant effectue le rafraîchissement à la fois (les tentatives
        // + pauses réseau peuvent tenir ~35s) : les autres reçoivent le cache
        // existant immédiatement plutôt que d'attendre en file derrière un
        // verrou. Même principe de dégradation gracieuse que pour ESPN indisponible.
        if (!refreshing.compareAndSet(false, true)) {
            return cache;
        }
        try {
            // ESPN sert son flux depuis plusieurs IP en répartition de charge
            // (Akamai) ; certaines peuvent être injoignables depuis un réseau
            // donné pendant que d'autres répondent normalement. Java ne bascule
            // pas automatiquement d'une IP à l'autre comme le font curl/les
            // navigateurs : on retente donc quelques fois plutôt que d'abandonner
            // à la première IP malchanceuse.
            Exception lastError = null;
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    String xml = restTemplate.getForObject(FEED_URL, String.class);
                    cache = parse(xml);
                    cachedAt = Instant.now();
                    return cache;
                } catch (Exception e) {
                    lastError = e;
                    try {
                        // > networkaddress.cache.ttl (10s, fixé dans BackendApplication) :
                        // sinon cette pause ne fait que retaper la même IP en cache.
                        Thread.sleep(11_000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            log.warn("Synchro actualités NBA échouée après 3 tentatives, conservation du cache précédent", lastError);
            return cache;
        } finally {
            refreshing.set(false);
        }
    }

    private List<NewsItemDto> parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // Contenu XML externe : on désactive DOCTYPE/entités externes pour se
        // prémunir d'une injection XXE, même si la source est de confiance.
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new org.xml.sax.InputSource(new StringReader(xml)));

        NodeList items = doc.getElementsByTagName("item");
        List<NewsItemDto> result = new ArrayList<>();

        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            String title = text(item, "title");
            String link = text(item, "link");
            String description = text(item, "description");
            String pubDate = text(item, "pubDate");
            if (title == null || link == null) continue;

            result.add(new NewsItemDto(title, link, description, "ESPN", parseDate(pubDate)));
        }

        // Le flux ESPN n'est pas garanti strictement chronologique (des items
        // partagent parfois le même pubDate, ou remontent des "top stories"
        // avant les plus récentes) : trié explicitement plutôt que de se fier
        // à l'ordre du flux, pour un fil vraiment du plus récent au plus ancien.
        result.sort(Comparator.comparing(NewsItemDto::publishedAt).reversed());

        return translate(result);
    }

    /**
     * Traduit titre + description en un seul appel DeepL (2 textes par
     * article) plutôt qu'un appel par champ : le flux entier tient dans une
     * seule requête, ce qui compte vu que ça tourne à chaque rafraîchissement
     * de cache (15 min), pas par visite de page.
     */
    private List<NewsItemDto> translate(List<NewsItemDto> items) {
        if (items.isEmpty()) return items;

        List<String> texts = new ArrayList<>();
        for (NewsItemDto item : items) {
            texts.add(item.title());
            texts.add(item.description() != null ? item.description() : "");
        }

        List<String> translated = deepLService.translateToFrench(texts);
        if (translated.size() != texts.size()) return items;

        List<NewsItemDto> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            NewsItemDto item = items.get(i);
            String translatedDescription = translated.get(i * 2 + 1);
            result.add(new NewsItemDto(
                    translated.get(i * 2),
                    item.link(),
                    translatedDescription.isBlank() ? null : translatedDescription,
                    item.source(),
                    item.publishedAt()
            ));
        }
        return result;
    }

    private String text(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return null;
        Node node = nodes.item(0);
        String content = node.getTextContent();
        return content == null ? null : content.trim();
    }

    // Package-privé pour être testable directement (voir NewsServiceTest).
    Instant parseDate(String raw) {
        if (raw == null) return Instant.now();
        String normalized = raw.trim();
        Matcher matcher = PUB_DATE_PATTERN.matcher(normalized);
        if (!matcher.matches()) return Instant.now();
        try {
            LocalDateTime local = LocalDateTime.parse(matcher.group(1), PUB_DATE_LOCAL_FORMAT);
            return local.atZone(EASTERN).toInstant();
        } catch (Exception e) {
            return Instant.now();
        }
    }
}
