package com.hooppicks.backendapplication.live;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.espn.EspnFormGame;
import com.hooppicks.backendapplication.espn.EspnPastGame;
import com.hooppicks.backendapplication.espn.EspnStatsClient;
import com.hooppicks.backendapplication.repository.MatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * L'avant-match : la forme des deux équipes (5 derniers matchs) et leurs 5
 * dernières confrontations.
 *
 * <p>Forme : bloc « lastFiveGames » du résumé ESPN du match (saison passée et
 * présaison comprises, utile en début de saison). Face-à-face : nos matchs en
 * base pour la saison en cours, complétés par les saisons précédentes via le
 * calendrier ESPN de l'équipe à domicile, notre base étant trop jeune.
 *
 * <p>Le calendrier ESPN pèse ~1,5 Mo par équipe et par saison : réduit tout de
 * suite au strict nécessaire et gardé {@link #RESULTS_TTL}. ESPN injoignable :
 * la dernière valeur connue sert encore, sinon forme vide et face-à-face tiré
 * de notre base seule. Affichage seulement, comme le direct.
 */
@Service
public class MatchPreviewService {

    static final Duration FORM_TTL = Duration.ofHours(1);
    static final Duration RESULTS_TTL = Duration.ofHours(24);
    static final int MEETINGS = 5;
    static final int MAX_ENTRIES = 300;

    public record H2HGame(Instant date, String homeAbbreviation, String awayAbbreviation, int homeScore,
                          int awayScore, boolean playoffs) {}

    public record PreviewDto(List<EspnFormGame> awayForm, List<EspnFormGame> homeForm, List<H2HGame> headToHead) {}

    private record Cached<T>(T value, Instant fetchedAt) {}

    private final EspnStatsClient espn;
    private final MatchRepository matchRepository;
    private final Clock clock;
    private final Map<String, Cached<?>> cache = new ConcurrentHashMap<>();

    @Autowired
    public MatchPreviewService(EspnStatsClient espn, MatchRepository matchRepository) {
        this(espn, matchRepository, Clock.systemUTC());
    }

    MatchPreviewService(EspnStatsClient espn, MatchRepository matchRepository, Clock clock) {
        this.espn = espn;
        this.matchRepository = matchRepository;
        this.clock = clock;
    }

    public Optional<PreviewDto> preview(String matchId) {
        Match match = matchRepository.findById(matchId).orElse(null);
        if (match == null) return Optional.empty();
        String home = match.getHomeTeam().getAbbreviation();
        String away = match.getAwayTeam().getAbbreviation();

        Map<String, List<EspnFormGame>> form = match.getEspnEventId() == null ? Map.of()
                : Optional.ofNullable(cached("form:" + match.getEspnEventId(), FORM_TTL,
                        () -> espn.fetchForm(match.getEspnEventId()).orElse(null))).orElse(Map.of());

        return Optional.of(new PreviewDto(form.getOrDefault(away, List.of()), form.getOrDefault(home, List.of()),
                headToHead(match, home, away)));
    }

    private List<H2HGame> headToHead(Match match, String home, String away) {
        // Une confrontation par jour : notre base et ESPN se recoupent sur la fin de saison dernière.
        Map<String, H2HGame> byDay = new LinkedHashMap<>();
        for (Match m : matchRepository.findFinishedMeetings(match.getHomeTeam(), match.getAwayTeam())) {
            if (m.getId().equals(match.getId())) continue;
            add(byDay, new H2HGame(m.getDate(), m.getHomeTeam().getAbbreviation(), m.getAwayTeam().getAbbreviation(),
                    m.getHomeScore(), m.getAwayScore(), false));
        }
        int season = seasonEndYear(match.getDate() != null ? match.getDate() : clock.instant());
        // Saison passée (régulière puis playoffs), et celle d'avant si ça ne suffit pas.
        int[][] sources = {{season - 1, 2}, {season - 1, 3}, {season - 2, 2}};
        for (int[] source : sources) {
            if (byDay.size() >= MEETINGS && source[0] < season - 1) break;
            List<EspnPastGame> games = cached("results:" + home + ":" + source[0] + ":" + source[1], RESULTS_TTL,
                    () -> espn.fetchTeamResults(home, source[0], source[1]).orElse(null));
            if (games == null) continue;
            games.stream()
                    .filter(g -> (g.homeAbbreviation().equals(away) || g.awayAbbreviation().equals(away)))
                    .forEach(g -> add(byDay, new H2HGame(g.date(), g.homeAbbreviation(), g.awayAbbreviation(),
                            g.homeScore(), g.awayScore(), g.playoffs())));
        }
        return byDay.values().stream()
                .filter(g -> g.date() != null && g.date().isBefore(clock.instant()))
                .sorted(Comparator.comparing(H2HGame::date).reversed())
                .limit(MEETINGS)
                .toList();
    }

    private static void add(Map<String, H2HGame> byDay, H2HGame game) {
        if (game.date() == null) return;
        byDay.putIfAbsent(game.date().atZone(ZoneOffset.UTC).toLocalDate().toString(), game);
    }

    /** Année de fin de la saison NBA d'une date (octobre 2026 → 2027 : saison 2026-27). */
    static int seasonEndYear(Instant date) {
        LocalDate day = date.atZone(BankrollService.ZONE).toLocalDate();
        return day.getMonthValue() >= 9 ? day.getYear() + 1 : day.getYear();
    }

    /** Valeur gardée `ttl` ; appel raté (null) : la dernière valeur connue, quel que soit son âge. */
    @SuppressWarnings("unchecked")
    private <T> T cached(String key, Duration ttl, Supplier<T> fetch) {
        Instant now = clock.instant();
        Cached<T> current = (Cached<T>) cache.get(key);
        if (current != null && current.fetchedAt().plus(ttl).isAfter(now)) return current.value();
        T fresh = fetch.get();
        if (fresh == null) return current == null ? null : current.value();
        if (cache.size() >= MAX_ENTRIES) {
            cache.entrySet().stream()
                    .sorted(Comparator.comparing(e -> e.getValue().fetchedAt()))
                    .limit(MAX_ENTRIES / 3)
                    .map(Map.Entry::getKey)
                    .toList()
                    .forEach(cache::remove);
        }
        cache.put(key, new Cached<>(fresh, now));
        return fresh;
    }
}
