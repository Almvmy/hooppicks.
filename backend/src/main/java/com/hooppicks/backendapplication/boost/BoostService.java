package com.hooppicks.backendapplication.boost;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.repository.DailyBoostRepository;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.season.SeasonCalendar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

/**
 * Cote boostée du jour : un match par soirée NBA (12h GMT → 12h GMT, comme la
 * semaine de jeu) dont le pari « vainqueur » est payé +15 %, mise plafonnée à
 * 100 pts pour tout ticket qui le contient (choix du propriétaire).
 *
 * <p>Le match retenu est « le choc de la soirée » : les deux équipes au
 * meilleur bilan cumulé (présaison seulement s'il n'y a rien d'autre), à
 * égalité le plus serré selon les cotes. Choisi au premier besoin puis figé en
 * base pour toute la soirée. Les cotes boostées viennent toujours d'ici, côté
 * serveur, comme les autres cotes.
 *
 * <p>Soirée d'ouverture de la saison (choix du propriétaire) : tous ses matchs
 * de saison régulière ont leur cote « vainqueur » boostée, quel que soit le
 * moment où le ticket est posé, avec le même plafond de mise.
 */
@Service
public class BoostService {

    public static final double FACTOR = 1.15;
    public static final int MAX_STAKE = 100;
    private static final LocalTime NIGHT_START = LocalTime.NOON;

    private final DailyBoostRepository boostRepository;
    private final MatchRepository matchRepository;
    private final SeasonCalendar calendar;
    private final Clock clock;

    @Autowired
    public BoostService(DailyBoostRepository boostRepository, MatchRepository matchRepository, SeasonCalendar calendar) {
        this(boostRepository, matchRepository, calendar, Clock.system(BankrollService.ZONE));
    }

    BoostService(DailyBoostRepository boostRepository, MatchRepository matchRepository, Clock clock) {
        this(boostRepository, matchRepository, new SeasonCalendar(matchRepository, clock), clock);
    }

    BoostService(DailyBoostRepository boostRepository, MatchRepository matchRepository, SeasonCalendar calendar, Clock clock) {
        this.boostRepository = boostRepository;
        this.matchRepository = matchRepository;
        this.calendar = calendar;
        this.clock = clock;
    }

    public record BoostedOdds(String matchId, double homeOdds, double awayOdds) {}

    /**
     * matchId/homeOdds/awayOdds : le choc de la soirée (null s'il n'y en a pas).
     * matches : tous les matchs boostés, choc compris (plusieurs seulement pour
     * la soirée d'ouverture, tant qu'elle n'est pas passée).
     */
    public record Boost(String matchId, double factor, int maxStake, Double homeOdds, Double awayOdds,
                        boolean openingNight, List<BoostedOdds> matches) {}

    /** Les boosts en cours : choc de la soirée et, à son approche, la soirée d'ouverture. */
    public Optional<Boost> current() {
        LocalDate night = currentNight();
        Optional<Match> choc = boostRepository.findById(night).map(DailyBoost::getMatchId)
                .or(() -> choose(night))
                .flatMap(matchRepository::findById);

        Map<String, BoostedOdds> all = new LinkedHashMap<>();
        choc.ifPresent(m -> all.put(m.getId(), odds(m)));
        List<Match> opening = upcomingOpeningNight();
        opening.stream().filter(m -> m.getMoneylineHome() > 1 && m.getMoneylineAway() > 1)
                .forEach(m -> all.putIfAbsent(m.getId(), odds(m)));
        if (all.isEmpty()) return Optional.empty();

        return Optional.of(new Boost(choc.map(Match::getId).orElse(null), FACTOR, MAX_STAKE,
                choc.map(m -> boosted(m.getMoneylineHome())).orElse(null),
                choc.map(m -> boosted(m.getMoneylineAway())).orElse(null),
                !opening.isEmpty(), List.copyOf(all.values())));
    }

    /**
     * Matchs boostés pour la pose d'un pari : le choc de la soirée s'il est
     * déjà choisi (jamais choisi ici : la transaction du pari ne doit pas
     * risquer un conflit d'écriture, le choix se fait à l'affichage) et ceux
     * de la soirée d'ouverture.
     */
    public Set<String> boostedMatchIds() {
        Set<String> ids = new HashSet<>(calendar.openingNightMatchIds());
        boostRepository.findById(currentNight()).map(DailyBoost::getMatchId).ifPresent(ids::add);
        return ids;
    }

    // Soirée d'ouverture pas encore terminée : ses matchs restent affichés boostés.
    private List<Match> upcomingOpeningNight() {
        Optional<LocalDate> night = calendar.openingNight();
        if (night.isEmpty() || night.get().isBefore(currentNight())) return List.of();
        return calendar.openingNightMatches();
    }

    private static BoostedOdds odds(Match m) {
        return new BoostedOdds(m.getId(), boosted(m.getMoneylineHome()), boosted(m.getMoneylineAway()));
    }

    // En décimal exact : en double, 1,7 × 1,15 vaut 1,9549999… et s'arrondirait à 1,95.
    public static double boosted(double odds) {
        return BigDecimal.valueOf(odds).multiply(BigDecimal.valueOf(FACTOR)).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    LocalDate currentNight() {
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(BankrollService.ZONE));
        return now.toLocalTime().isBefore(NIGHT_START) ? now.toLocalDate().minusDays(1) : now.toLocalDate();
    }

    private Optional<String> choose(LocalDate night) {
        Instant from = night.atTime(NIGHT_START).atZone(BankrollService.ZONE).toInstant();
        Instant now = clock.instant();
        List<Match> candidates = matchRepository.findByStatusAndDateBetween(MatchStatus.SCHEDULED,
                        from.isAfter(now) ? from : now, from.plus(Duration.ofDays(1))).stream()
                .filter(m -> m.getMoneylineHome() > 1 && m.getMoneylineAway() > 1)
                .toList();
        List<Match> pool = candidates.stream().anyMatch(m -> m.getType() != MatchType.PRESEASON)
                ? candidates.stream().filter(m -> m.getType() != MatchType.PRESEASON).toList()
                : candidates;
        Optional<Match> pick = pool.stream().max(Comparator
                .comparingDouble((Match m) -> winPct(m.getHomeTeam()) + winPct(m.getAwayTeam()))
                .thenComparingDouble(m -> -Math.abs(m.getMoneylineHome() - m.getMoneylineAway())));
        if (pick.isEmpty()) return Optional.empty();

        DailyBoost boost = new DailyBoost();
        boost.setNight(night);
        boost.setMatchId(pick.get().getId());
        try {
            boostRepository.saveAndFlush(boost);
        } catch (DataIntegrityViolationException e) {
            // Choisi en même temps par une autre requête : on garde le sien.
            return boostRepository.findById(night).map(DailyBoost::getMatchId);
        }
        return Optional.of(pick.get().getId());
    }

    private static double winPct(Team t) {
        int w = t.getWins() == null ? 0 : t.getWins();
        int l = t.getLosses() == null ? 0 : t.getLosses();
        return w + l == 0 ? 0.5 : (double) w / (w + l);
    }
}
