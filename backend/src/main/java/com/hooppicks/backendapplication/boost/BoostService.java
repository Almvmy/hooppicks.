package com.hooppicks.backendapplication.boost;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.repository.DailyBoostRepository;
import com.hooppicks.backendapplication.repository.MatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
 */
@Service
public class BoostService {

    public static final double FACTOR = 1.15;
    public static final int MAX_STAKE = 100;
    private static final LocalTime NIGHT_START = LocalTime.NOON;

    private final DailyBoostRepository boostRepository;
    private final MatchRepository matchRepository;
    private final Clock clock;

    @Autowired
    public BoostService(DailyBoostRepository boostRepository, MatchRepository matchRepository) {
        this(boostRepository, matchRepository, Clock.system(BankrollService.ZONE));
    }

    BoostService(DailyBoostRepository boostRepository, MatchRepository matchRepository, Clock clock) {
        this.boostRepository = boostRepository;
        this.matchRepository = matchRepository;
        this.clock = clock;
    }

    public record Boost(String matchId, double factor, int maxStake, double homeOdds, double awayOdds) {}

    /** Le boost de la soirée en cours, s'il y a un match à booster. */
    public Optional<Boost> current() {
        LocalDate night = currentNight();
        Optional<String> matchId = boostRepository.findById(night).map(DailyBoost::getMatchId)
                .or(() -> choose(night));
        return matchId.flatMap(matchRepository::findById)
                .map(m -> new Boost(m.getId(), FACTOR, MAX_STAKE, boosted(m.getMoneylineHome()), boosted(m.getMoneylineAway())));
    }

    /**
     * Match boosté de la soirée s'il est déjà choisi, sans jamais le choisir :
     * pour la pose d'un pari, dont la transaction ne doit pas risquer un
     * conflit d'écriture (le choix se fait à l'affichage, avant tout pari).
     */
    public Optional<String> chosenMatchId() {
        return boostRepository.findById(currentNight()).map(DailyBoost::getMatchId);
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
