package com.hooppicks.backendapplication.season;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.MatchType;
import com.hooppicks.backendapplication.repository.BracketRepository;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.SeasonResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Les onglets qui n'apparaissent que quand ils servent (choix du propriétaire) :
 * Compétition en avait 7, ceux de droite sortaient de l'écran sur téléphone et
 * restaient là des mois pour rien. Les pages restent ouvertes par leur adresse.
 */
@Service
public class SeasonalTabsService {

    // Plus large que l'écart entre deux soirées de Coupe (7 jours) et assez pour
    // l'annoncer un peu avant, et laisser le résultat visible après la finale.
    static final Duration CUP_MARGIN = Duration.ofDays(8);

    public record SeasonalTabs(boolean cup, boolean season) {}

    private final MatchRepository matchRepository;
    private final BracketRepository bracketRepository;
    private final SeasonPickService seasonPickService;
    private final SeasonResultRepository resultRepository;
    private final Clock clock;

    @Autowired
    public SeasonalTabsService(MatchRepository matchRepository, BracketRepository bracketRepository,
                               SeasonPickService seasonPickService, SeasonResultRepository resultRepository) {
        this(matchRepository, bracketRepository, seasonPickService, resultRepository, Clock.system(BankrollService.ZONE));
    }

    SeasonalTabsService(MatchRepository matchRepository, BracketRepository bracketRepository,
                        SeasonPickService seasonPickService, SeasonResultRepository resultRepository, Clock clock) {
        this.matchRepository = matchRepository;
        this.bracketRepository = bracketRepository;
        this.seasonPickService = seasonPickService;
        this.resultRepository = resultRepository;
        this.clock = clock;
    }

    public SeasonalTabs tabs() {
        Instant now = clock.instant();
        return new SeasonalTabs(cup(now), season(now));
    }

    private boolean cup(Instant now) {
        // Les matchs à élimination directe peuvent manquer chez balldontlie : un tableau pas encore fini suffit.
        boolean bracketRunning = bracketRepository.findTopByOrderByLocksAtDesc().filter(b -> !Boolean.TRUE.equals(b.getAwarded())).isPresent();
        return bracketRunning || matchRepository.existsByTypeInAndDateBetween(List.of(MatchType.NBA_CUP), now.minus(CUP_MARGIN), now.plus(CUP_MARGIN));
    }

    /** Pronostics encore ouverts, ou résultats saisis : entre les deux, il n'y a rien à y voir de nouveau. */
    private boolean season(Instant now) {
        String season = seasonPickService.currentSeason();
        Instant deadline = seasonPickService.deadline(season);
        return deadline == null || now.isBefore(deadline) || !resultRepository.findBySeason(season).isEmpty();
    }
}
