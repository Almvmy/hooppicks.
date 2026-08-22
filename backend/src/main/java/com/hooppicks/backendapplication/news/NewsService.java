package com.hooppicks.backendapplication.news;

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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class NewsService {

    private static final String FEED_URL = "https://www.espn.com/espn/rss/nba/news";
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);

    // RFC_1123_DATE_TIME ne comprend que les décalages numériques ("+0000") —
    // ESPN envoie des abréviations de fuseaux US ("EST") qu'il faut substituer
    // avant de parser, sinon toutes les dates tombent sur le fallback "now".
    private static final Map<String, String> US_ZONE_OFFSETS = Map.of(
            "EST", "-0500", "EDT", "-0400",
            "CST", "-0600", "CDT", "-0500",
            "MST", "-0700", "MDT", "-0600",
            "PST", "-0800", "PDT", "-0700"
    );

    private final RestTemplate restTemplate;
    private final DeepLService deepLService;

    private volatile List<NewsItemDto> cache = List.of();
    private volatile Instant cachedAt = Instant.EPOCH;

    public NewsService(RestTemplate restTemplate, DeepLService deepLService) {
        this.restTemplate = restTemplate;
        this.deepLService = deepLService;
    }

    /**
     * Rafraîchit depuis ESPN au plus toutes les {@link #CACHE_TTL} — sinon
     * chaque visite de la page actus ferait un aller-retour externe. Si ESPN
     * est indisponible, on garde le dernier cache connu (même périmé) plutôt
     * que de vider la page.
     */
    public synchronized List<NewsItemDto> fetchLatest() {
        if (Duration.between(cachedAt, Instant.now()).compareTo(CACHE_TTL) < 0) {
            return cache;
        }

        // ESPN sert son flux depuis plusieurs IP en répartition de charge
        // (Akamai) ; certaines peuvent être injoignables depuis un réseau
        // donné pendant que d'autres répondent normalement. Java ne bascule
        // pas automatiquement d'une IP à l'autre comme le font curl/les
        // navigateurs — on retente donc quelques fois plutôt que d'abandonner
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
        System.out.println("Synchro actualités NBA échouée après 3 tentatives, conservation du cache précédent : "
                + (lastError != null ? lastError.getMessage() : "raison inconnue"));
        return cache;
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

        return translate(result);
    }

    /**
     * Traduit titre + description en un seul appel DeepL (2 textes par
     * article) plutôt qu'un appel par champ — le flux entier tient dans une
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

    private Instant parseDate(String raw) {
        if (raw == null) return Instant.now();
        String normalized = raw.trim();
        for (Map.Entry<String, String> zone : US_ZONE_OFFSETS.entrySet()) {
            if (normalized.endsWith(zone.getKey())) {
                normalized = normalized.substring(0, normalized.length() - zone.getKey().length()).trim()
                        + " " + zone.getValue();
                break;
            }
        }
        try {
            return DateTimeFormatter.RFC_1123_DATE_TIME.parse(normalized, Instant::from);
        } catch (Exception e) {
            return Instant.now();
        }
    }
}
