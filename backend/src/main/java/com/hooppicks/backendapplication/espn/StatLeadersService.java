package com.hooppicks.backendapplication.espn;

import com.hooppicks.backendapplication.bet.PlayerPropsService;
import com.hooppicks.backendapplication.dto.PlayerLeadersDto;
import com.hooppicks.backendapplication.dto.RosterPlayerDto;
import com.hooppicks.backendapplication.dto.TopPerformanceDto;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.PlayerMatchStat;
import com.hooppicks.backendapplication.entity.RosterPlayer;
import com.hooppicks.backendapplication.repository.PlayerMatchStatRepository;
import com.hooppicks.backendapplication.repository.RosterPlayerRepository;
import com.hooppicks.backendapplication.season.SeasonCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

/**
 * Meneurs statistiques et perfs de la nuit, page Joueurs.
 *
 * Meneurs : saison en cours seulement (avant le premier match de la saison,
 * la dernière saison connue), avec un minimum de matchs joués (la moitié de
 * ce qu'a joué le plus assidu) : sinon un joueur à un seul match à 40 points
 * passait devant tout le monde. Pourcentage aux tirs réservé aux joueurs à
 * 10 points de moyenne ou plus.
 *
 * Perfs de la nuit : les meilleures lignes de la dernière soirée NBA jouée
 * (12h → 12h GMT), d'après nos feuilles de match, classées par un Game Score
 * simplifié (pas de rebonds offensifs ni de fautes sur nos feuilles).
 */
@Service
public class StatLeadersService {

    private static final int TOP = 5;
    private static final int FULL_LIST = 25;
    private static final int TOP_PERFORMANCES = 6;
    private static final double MIN_POINTS_FOR_FG_PCT = 10.0;

    private final RosterPlayerRepository rosterRepository;
    private final PlayerMatchStatRepository statRepository;
    private final SeasonCalendar seasonCalendar;

    public StatLeadersService(RosterPlayerRepository rosterRepository, PlayerMatchStatRepository statRepository,
                              SeasonCalendar seasonCalendar) {
        this.rosterRepository = rosterRepository;
        this.statRepository = statRepository;
        this.seasonCalendar = seasonCalendar;
    }

    @Transactional(readOnly = true)
    /** Catégories des meneurs, par leur nom dans l'API. */
    enum Category {
        POINTS(RosterPlayer::getPointsPerGame),
        REBOUNDS(RosterPlayer::getReboundsPerGame),
        ASSISTS(RosterPlayer::getAssistsPerGame),
        STEALS(RosterPlayer::getStealsPerGame),
        BLOCKS(RosterPlayer::getBlocksPerGame),
        THREES(RosterPlayer::getThreePointersMadePerGame),
        FIELD_GOAL_PCT(RosterPlayer::getFieldGoalPct);

        final Function<RosterPlayer, Double> stat;

        Category(Function<RosterPlayer, Double> stat) {
            this.stat = stat;
        }

        boolean eligible(RosterPlayer p) {
            return this != FIELD_GOAL_PCT || (p.getPointsPerGame() != null && p.getPointsPerGame() >= MIN_POINTS_FOR_FG_PCT);
        }

        /** « points », « fieldGoalPct » : comme les champs de PlayerLeadersDto. */
        static Optional<Category> parse(String raw) {
            if (raw == null) return Optional.empty();
            String key = raw.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
            return Arrays.stream(values()).filter(c -> c.name().equals(key)).findFirst();
        }
    }

    /** Joueurs de la saison retenue ayant assez joué, avec la saison et le minimum de matchs. */
    private record Pool(List<RosterPlayer> players, String season, int minGames) {}

    private Pool pool() {
        List<RosterPlayer> all = rosterRepository.findAll();
        String current = seasonCalendar.currentSeason();
        String season = all.stream().anyMatch(p -> current.equals(p.getStatsSeasonLabel()))
                ? current
                : all.stream().map(RosterPlayer::getStatsSeasonLabel).filter(Objects::nonNull)
                        .max(Comparator.naturalOrder()).orElse(current);
        List<RosterPlayer> seasonPlayers = all.stream().filter(p -> season.equals(p.getStatsSeasonLabel())).toList();
        int maxGames = seasonPlayers.stream().map(RosterPlayer::getGamesPlayed).filter(Objects::nonNull).max(Integer::compare).orElse(0);
        int minGames = minGames(maxGames);
        List<RosterPlayer> eligible = seasonPlayers.stream()
                .filter(p -> p.getGamesPlayed() != null && p.getGamesPlayed() >= minGames).toList();
        return new Pool(eligible, season, minGames);
    }

    @Transactional(readOnly = true)
    public PlayerLeadersDto leaders() {
        Pool pool = pool();
        List<RosterPlayer> eligible = pool.players();
        return new PlayerLeadersDto(
                top(eligible, Category.POINTS, TOP),
                top(eligible, Category.REBOUNDS, TOP),
                top(eligible, Category.ASSISTS, TOP),
                top(eligible, Category.STEALS, TOP),
                top(eligible, Category.BLOCKS, TOP),
                top(eligible, Category.THREES, TOP),
                top(eligible, Category.FIELD_GOAL_PCT, TOP),
                pool.season(),
                pool.minGames());
    }

    /** Classement plus long d'une catégorie (« Voir le top 25 »). Vide si la catégorie est inconnue. */
    @Transactional(readOnly = true)
    public Optional<com.hooppicks.backendapplication.dto.LeaderListDto> leaders(String category, int limit) {
        return Category.parse(category).map(c -> {
            Pool pool = pool();
            return new com.hooppicks.backendapplication.dto.LeaderListDto(
                    top(pool.players(), c, Math.max(1, Math.min(limit, FULL_LIST))), pool.season(), pool.minGames());
        });
    }

    /** La moitié des matchs du plus assidu, 1 au moins : 1 en tout début de saison, ~40 en fin. */
    static int minGames(int maxGames) {
        return Math.max(1, (int) Math.ceil(maxGames / 2.0));
    }

    private static List<RosterPlayerDto> top(List<RosterPlayer> players, Category category, int limit) {
        return players.stream()
                .filter(category::eligible)
                .filter(p -> category.stat.apply(p) != null)
                .sorted(Comparator.comparing(category.stat, Comparator.reverseOrder()))
                .limit(limit)
                .map(RosterPlayerDto::from)
                .toList();
    }

    /** Meilleures lignes de la dernière soirée jouée ces 7 derniers jours (vide sinon). */
    @Transactional(readOnly = true)
    public List<TopPerformanceDto> lastNight() {
        Instant now = seasonCalendar.now();
        List<PlayerMatchStat> recent = statRepository.findFinishedBetween(now.minus(Duration.ofDays(7)), now);
        Optional<LocalDate> night = recent.stream().map(s -> SeasonCalendar.nightOf(s.getMatch().getDate()))
                .max(Comparator.naturalOrder());
        if (night.isEmpty()) return List.of();

        Map<String, RosterPlayer> roster = new HashMap<>();
        for (RosterPlayer p : rosterRepository.findAll()) {
            roster.putIfAbsent(PlayerPropsService.normalize(p.getFirstName() + " " + p.getLastName()), p);
        }
        return recent.stream()
                .filter(s -> SeasonCalendar.nightOf(s.getMatch().getDate()).equals(night.get()))
                .filter(s -> minutes(s) > 0)
                .sorted(Comparator.comparingDouble(StatLeadersService::gameScore).reversed())
                .limit(TOP_PERFORMANCES)
                .map(s -> toDto(s, roster.get(PlayerPropsService.normalize(s.getPlayerName()))))
                .toList();
    }

    /** Game Score de Hollinger sans rebonds offensifs ni fautes (absents de nos feuilles). */
    static double gameScore(PlayerMatchStat s) {
        return s.getPoints() + 0.4 * s.getFieldGoalsMade() - 0.7 * s.getFieldGoalsAttempted()
                - 0.4 * (s.getFreeThrowsAttempted() - s.getFreeThrowsMade()) + 0.5 * s.getRebounds()
                + s.getSteals() + 0.7 * s.getAssists() + 0.7 * s.getBlocks() - s.getTurnovers();
    }

    static int minutes(PlayerMatchStat s) {
        String m = s.getMinutes();
        if (m == null) return 0;
        String digits = m.trim().replaceAll("[^0-9].*$", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

    private static TopPerformanceDto toDto(PlayerMatchStat s, RosterPlayer player) {
        Match m = s.getMatch();
        String team = EspnStatsClient.fromEspnAbbreviation(s.getTeamAbbreviation());
        String home = m.getHomeTeam() == null ? null : m.getHomeTeam().getAbbreviation();
        String away = m.getAwayTeam() == null ? null : m.getAwayTeam().getAbbreviation();
        String opponent = Objects.equals(team, home) ? away : home;
        return new TopPerformanceDto(s.getPlayerName(), team, opponent, m.getId(),
                player == null ? null : RosterPlayerDto.from(player),
                minutes(s), s.getPoints(), s.getRebounds(), s.getAssists(), s.getSteals(), s.getBlocks(),
                s.getThreePointsMade(), s.getFieldGoalsMade(), s.getFieldGoalsAttempted());
    }
}
