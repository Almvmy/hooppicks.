package com.hooppicks.backendapplication.cup;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.entity.MatchType;
import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.leaderboard.LeaderboardPeriod;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Le classement des poules de la Coupe NBA, recalculé à partir de nos matchs.
 * La composition des poules n'existe dans aucune de nos sources (ESPN dit seulement
 * « Group Play ») : elle est écrite ici, saison par saison. Celle de 2026-27 vient de
 * l'annonce de la NBA, recoupée avec le calendrier ESPN (60 matchs, chaque équipe
 * contre les 4 autres de sa poule).
 *
 * Départage de la NBA : bilan, confrontations directes, écart de points, points
 * marqués. Qualifiés (premier de chaque poule et meilleur deuxième de la conférence)
 * marqués seulement quand tous les matchs de la conférence sont joués : avant, il
 * faudrait envisager tous les scénarios, et un « qualifié » faux serait pire que rien.
 */
@Service
public class CupStandingsService {

    private static final int GAMES_PER_TEAM = 4;

    private record GroupDef(String conference, String name, List<String> teams) {}

    public record TeamRow(String teamId, String abbreviation, String name, String logoUrl, int wins, int losses, int diff, int pointsFor, String status) {}

    public record Group(String conference, String name, List<TeamRow> teams) {}

    public record Standings(List<Group> groups) {}

    private static final Map<String, List<GroupDef>> GROUPS = Map.of("2026-27", List.of(
            new GroupDef("Ouest", "A", List.of("DEN", "HOU", "PHX", "DAL", "UTA")),
            new GroupDef("Ouest", "B", List.of("OKC", "MIN", "LAC", "NOP", "MEM")),
            new GroupDef("Ouest", "C", List.of("SAS", "LAL", "POR", "GSW", "SAC")),
            new GroupDef("Est", "A", List.of("DET", "TOR", "ORL", "MIL", "BKN")),
            new GroupDef("Est", "B", List.of("NYK", "CLE", "PHI", "MIA", "IND")),
            new GroupDef("Est", "C", List.of("BOS", "ATL", "CHA", "CHI", "WAS"))));

    private static final class Line {
        final String abbreviation;
        int wins, losses, diff, pointsFor;

        Line(String abbreviation) {
            this.abbreviation = abbreviation;
        }
    }

    private record Game(String home, String away, int homeScore, int awayScore) {}

    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final Clock clock;

    @Autowired
    public CupStandingsService(MatchRepository matchRepository, TeamRepository teamRepository) {
        this(matchRepository, teamRepository, Clock.system(BankrollService.ZONE));
    }

    CupStandingsService(MatchRepository matchRepository, TeamRepository teamRepository, Clock clock) {
        this.matchRepository = matchRepository;
        this.teamRepository = teamRepository;
        this.clock = clock;
    }

    private static String seasonOf(ZonedDateTime now) {
        int start = now.getMonthValue() >= 9 ? now.getYear() : now.getYear() - 1;
        return start + "-" + String.valueOf(start + 1).substring(2);
    }

    @Transactional(readOnly = true)
    public Standings standings() {
        ZonedDateTime now = clock.instant().atZone(BankrollService.ZONE);
        List<GroupDef> defs = GROUPS.get(seasonOf(now));
        if (defs == null) return new Standings(List.of());

        // Seulement les poules : un quart peut opposer deux équipes de la même poule.
        List<Game> games = matchRepository.findByTypeAndStatusAndDateAfter(MatchType.NBA_CUP, MatchStatus.FINISHED, LeaderboardPeriod.seasonStart(now)).stream()
                .filter(m -> m.getStageLabel() != null && m.getStageLabel().contains("Phase de groupes"))
                .filter(m -> m.getHomeScore() != null && m.getAwayScore() != null)
                .map(CupStandingsService::game)
                .toList();
        Map<String, Team> teams = teamRepository.findAll().stream().collect(Collectors.toMap(Team::getAbbreviation, Function.identity(), (a, b) -> a));

        Map<GroupDef, List<Line>> ranked = new LinkedHashMap<>();
        for (GroupDef def : defs) ranked.put(def, rank(def.teams(), games));

        Map<String, String> statuses = new HashMap<>();
        for (String conference : List.of("Est", "Ouest")) statuses.putAll(statuses(conference, ranked));

        return new Standings(ranked.entrySet().stream().map(e -> new Group(e.getKey().conference(), e.getKey().name(), e.getValue().stream().map(l -> {
            Team t = teams.get(l.abbreviation);
            return new TeamRow(t == null ? null : t.getId(), l.abbreviation, t == null ? l.abbreviation : t.getName(), t == null ? null : t.getLogoUrl(),
                    l.wins, l.losses, l.diff, l.pointsFor, statuses.get(l.abbreviation));
        }).toList())).toList());
    }

    private static Game game(Match m) {
        return new Game(m.getHomeTeam().getAbbreviation(), m.getAwayTeam().getAbbreviation(), m.getHomeScore(), m.getAwayScore());
    }

    private static List<Line> lines(Collection<String> teams, List<Game> games) {
        Map<String, Line> lines = new LinkedHashMap<>();
        for (String team : teams) lines.put(team, new Line(team));
        for (Game g : games) {
            Line home = lines.get(g.home());
            Line away = lines.get(g.away());
            if (home == null || away == null) continue;
            home.diff += g.homeScore() - g.awayScore();
            away.diff += g.awayScore() - g.homeScore();
            home.pointsFor += g.homeScore();
            away.pointsFor += g.awayScore();
            if (g.homeScore() > g.awayScore()) {
                home.wins++;
                away.losses++;
            } else {
                away.wins++;
                home.losses++;
            }
        }
        return new ArrayList<>(lines.values());
    }

    private static final Comparator<Line> BY_DIFF_THEN_POINTS = Comparator.<Line>comparingInt(l -> -l.diff).thenComparingInt(l -> -l.pointsFor);

    static List<String> rankedAbbreviations(List<String> teams, List<Match> matches) {
        return rank(teams, matches.stream().map(CupStandingsService::game).toList()).stream().map(l -> l.abbreviation).toList();
    }

    private static List<Line> rank(List<String> teams, List<Game> games) {
        List<Line> all = lines(teams, games);
        Map<Integer, List<Line>> byWins = all.stream().collect(Collectors.groupingBy(l -> l.wins, TreeMap::new, Collectors.toList()));
        List<Line> ordered = new ArrayList<>();
        new ArrayDeque<>(byWins.values()).descendingIterator().forEachRemaining(tied -> {
            // Les confrontations directes ne comptent qu'entre équipes à égalité.
            Map<String, Integer> headToHead = new HashMap<>();
            if (tied.size() > 1) {
                for (Line l : lines(tied.stream().map(x -> x.abbreviation).toList(), games)) headToHead.put(l.abbreviation, l.wins);
            }
            tied.sort(Comparator.<Line>comparingInt(l -> -headToHead.getOrDefault(l.abbreviation, 0)).thenComparing(BY_DIFF_THEN_POINTS));
            ordered.addAll(tied);
        });
        return ordered;
    }

    private static Map<String, String> statuses(String conference, Map<GroupDef, List<Line>> ranked) {
        List<List<Line>> groups = ranked.entrySet().stream().filter(e -> e.getKey().conference().equals(conference)).map(Map.Entry::getValue).toList();
        boolean over = groups.stream().flatMap(List::stream).allMatch(l -> l.wins + l.losses >= GAMES_PER_TEAM);
        Map<String, String> statuses = new HashMap<>();
        if (!over) return statuses;
        Line wildCard = groups.stream().map(g -> g.get(1))
                .min(Comparator.<Line>comparingInt(l -> -l.wins).thenComparing(BY_DIFF_THEN_POINTS)).orElseThrow();
        for (List<Line> group : groups) {
            for (int i = 0; i < group.size(); i++) {
                Line l = group.get(i);
                statuses.put(l.abbreviation, i == 0 ? "qualified" : l == wildCard ? "wildcard" : "eliminated");
            }
        }
        return statuses;
    }
}
