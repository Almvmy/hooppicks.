package com.hooppicks.backendapplication.live;

import com.hooppicks.backendapplication.dto.PlayerBoxScoreDto;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.espn.EspnLiveGame;
import com.hooppicks.backendapplication.espn.EspnLiveSummary;
import com.hooppicks.backendapplication.espn.EspnScheduleService;
import com.hooppicks.backendapplication.espn.EspnStatsClient;
import com.hooppicks.backendapplication.repository.MatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Le direct : quart-temps, chrono, score du moment, stats et feuille de match
 * pendant le match, lus chez ESPN à la demande.
 *
 * <p><b>Affichage seulement, jamais écrit en base.</b> Le statut et le score
 * des matchs restent ceux de la synchro (balldontlie), seule à régler les
 * paris : si ESPN se trompe ou tombe, aucun pari n'est touché.
 *
 * <p>Une réponse ESPN sert à tout le monde pendant {@link #FRESH} (un appel
 * par match, quel que soit le nombre de joueurs qui regardent). ESPN
 * injoignable : on garde la dernière réponse au plus {@link #STALE_LIMIT},
 * puis on ne renvoie plus rien, et l'app retombe sur le score et le statut de
 * la synchro plutôt que d'afficher un chrono figé.
 */
@Service
public class LiveMatchService {

    static final Duration FRESH = Duration.ofSeconds(20);
    static final Duration STALE_LIMIT = Duration.ofMinutes(2);

    // Stats d'équipe montrées face à face, dans cet ordre (noms ESPN → libellés).
    static final List<Map.Entry<String, String>> TEAM_STATS = List.of(
            Map.entry("fieldGoalsMade-fieldGoalsAttempted", "Tirs réussis"),
            Map.entry("fieldGoalPct", "Adresse (%)"),
            Map.entry("threePointFieldGoalsMade-threePointFieldGoalsAttempted", "Tirs à 3 pts"),
            Map.entry("threePointFieldGoalPct", "Adresse à 3 pts (%)"),
            Map.entry("freeThrowsMade-freeThrowsAttempted", "Lancers francs"),
            Map.entry("totalRebounds", "Rebonds"),
            Map.entry("offensiveRebounds", "Rebonds offensifs"),
            Map.entry("assists", "Passes décisives"),
            Map.entry("steals", "Interceptions"),
            Map.entry("blocks", "Contres"),
            Map.entry("totalTurnovers", "Balles perdues"),
            Map.entry("pointsInPaint", "Points dans la raquette"),
            Map.entry("fastBreakPoints", "Points en contre-attaque"),
            Map.entry("largestLead", "Plus gros écart"));

    public record LiveStatusDto(String matchId, String state, int period, String clock, String detail,
                                Integer homeScore, Integer awayScore) {}

    public record TeamStatLine(String label, String home, String away) {}

    public record LiveMatchDto(LiveStatusDto status, List<Integer> homeLinescores, List<Integer> awayLinescores,
                               List<TeamStatLine> teamStats, List<PlayerBoxScoreDto> players) {}

    private record Cached<T>(T value, Instant fetchedAt) {}

    private final EspnStatsClient espnStatsClient;
    private final MatchRepository matchRepository;
    private final Clock clock;
    private final Map<String, Cached<?>> cache = new ConcurrentHashMap<>();

    @Autowired
    public LiveMatchService(EspnStatsClient espnStatsClient, MatchRepository matchRepository) {
        this(espnStatsClient, matchRepository, Clock.systemUTC());
    }

    LiveMatchService(EspnStatsClient espnStatsClient, MatchRepository matchRepository, Clock clock) {
        this.espnStatsClient = espnStatsClient;
        this.matchRepository = matchRepository;
        this.clock = clock;
    }

    /**
     * Statut en direct des matchs en cours (ou tout juste finis chez ESPN),
     * pour les listes. Vide si ESPN ne répond plus depuis plus de
     * {@link #STALE_LIMIT}.
     */
    public List<LiveStatusDto> board() {
        List<EspnLiveGame> games = cached("board", () -> {
            // Journée ESPN = journée de New York ; la veille aussi, pour un
            // match commencé avant minuit là-bas et pas encore fini.
            LocalDate today = LocalDate.now(clock.withZone(EspnScheduleService.NBA_ZONE));
            Optional<List<EspnLiveGame>> t = espnStatsClient.fetchLiveScoreboard(today);
            Optional<List<EspnLiveGame>> y = espnStatsClient.fetchLiveScoreboard(today.minusDays(1));
            if (t.isEmpty() && y.isEmpty()) return null;
            List<EspnLiveGame> all = new ArrayList<>(y.orElse(List.of()));
            all.addAll(t.orElse(List.of()));
            return all;
        });
        if (games == null) return List.of();
        List<EspnLiveGame> started = games.stream().filter(g -> !"pre".equals(g.state())).toList();
        if (started.isEmpty()) return List.of();
        Map<String, EspnLiveGame> byEvent = new HashMap<>();
        started.forEach(g -> byEvent.put(g.eventId(), g));
        return matchRepository.findByEspnEventIdIn(byEvent.keySet()).stream()
                .map(m -> toStatus(m.getId(), byEvent.get(m.getEspnEventId())))
                .toList();
    }

    /** Détail du direct d'un match. Vide : match inconnu, pas relié à ESPN, ou ESPN muet trop longtemps. */
    public Optional<LiveMatchDto> detail(String matchId) {
        Match match = matchRepository.findById(matchId).orElse(null);
        if (match == null || match.getEspnEventId() == null) return Optional.empty();
        EspnLiveSummary summary = cached("summary:" + match.getEspnEventId(),
                () -> espnStatsClient.fetchLiveSummary(match.getEspnEventId()).orElse(null));
        if (summary == null || "pre".equals(summary.status().state())) return Optional.empty();

        List<TeamStatLine> stats = TEAM_STATS.stream()
                .filter(e -> summary.home().stats().containsKey(e.getKey()) || summary.away().stats().containsKey(e.getKey()))
                .map(e -> new TeamStatLine(e.getValue(),
                        summary.home().stats().getOrDefault(e.getKey(), "-"),
                        summary.away().stats().getOrDefault(e.getKey(), "-")))
                .toList();
        List<PlayerBoxScoreDto> players = summary.players().stream()
                .map(PlayerBoxScoreDto::from)
                .sorted(Comparator.comparingInt(PlayerBoxScoreDto::points).reversed())
                .toList();
        return Optional.of(new LiveMatchDto(toStatus(matchId, summary.status()), summary.home().linescores(),
                summary.away().linescores(), stats, players));
    }

    private static LiveStatusDto toStatus(String matchId, EspnLiveGame g) {
        return new LiveStatusDto(matchId, g.state(), g.period(), g.clock(), g.detail(), g.homeScore(), g.awayScore());
    }

    /**
     * Valeur fraîche si on en a une de moins de {@link #FRESH}, sinon nouvel
     * appel. Appel raté (null) : la dernière valeur tant qu'elle a moins de
     * {@link #STALE_LIMIT}, puis null.
     */
    @SuppressWarnings("unchecked")
    private <T> T cached(String key, Supplier<T> fetch) {
        Instant now = clock.instant();
        Cached<T> current = (Cached<T>) cache.get(key);
        if (current != null && current.fetchedAt().plus(FRESH).isAfter(now)) return current.value();
        T fresh = fetch.get();
        if (fresh != null) {
            cache.put(key, new Cached<>(fresh, now));
            return fresh;
        }
        if (current != null && current.fetchedAt().plus(STALE_LIMIT).isAfter(now)) return current.value();
        cache.remove(key);
        return null;
    }
}
