package com.hooppicks.backendapplication.season;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchType;
import com.hooppicks.backendapplication.repository.MatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Repères de la saison NBA en cours : premier match de saison régulière et
 * « soirée d'ouverture » (la soirée NBA qui le contient, 12h → 12h GMT comme
 * la semaine de jeu). Lus dans les matchs synchronisés : la date officielle
 * n'est écrite nulle part ailleurs.
 */
@Service
public class SeasonCalendar {

    static final LocalTime NIGHT_START = LocalTime.NOON;

    private final MatchRepository matchRepository;
    private final Clock clock;

    @Autowired
    public SeasonCalendar(MatchRepository matchRepository) {
        this(matchRepository, Clock.system(BankrollService.ZONE));
    }

    /** Horloge fixe : pour les tests (aussi ceux d'autres paquets). */
    public SeasonCalendar(MatchRepository matchRepository, Clock clock) {
        this.matchRepository = matchRepository;
        this.clock = clock;
    }

    /** « 2026-27 » : une saison NBA commence en octobre, on bascule au 1er septembre. */
    public String currentSeason() {
        LocalDate today = LocalDate.now(clock.withZone(BankrollService.ZONE));
        int start = today.getMonthValue() >= 9 ? today.getYear() : today.getYear() - 1;
        return start + "-" + String.valueOf(start + 1).substring(2);
    }

    public Optional<Match> opener(String season) {
        int start = Integer.parseInt(season.substring(0, 4));
        Instant from = LocalDate.of(start, 9, 1).atStartOfDay(BankrollService.ZONE).toInstant();
        return matchRepository.findFirstByTypeAndDateAfterOrderByDateAsc(MatchType.REGULAR, from);
    }

    public static LocalDate nightOf(Instant instant) {
        ZonedDateTime t = instant.atZone(BankrollService.ZONE);
        return t.toLocalTime().isBefore(NIGHT_START) ? t.toLocalDate().minusDays(1) : t.toLocalDate();
    }

    public static Instant nightStart(LocalDate night) {
        return night.atTime(NIGHT_START).atZone(BankrollService.ZONE).toInstant();
    }

    public Optional<LocalDate> openingNight() {
        return opener(currentSeason()).map(m -> nightOf(m.getDate()));
    }

    /** Matchs de saison régulière de la soirée d'ouverture (vide tant que le calendrier n'est pas connu). */
    public List<Match> openingNightMatches() {
        return openingNight().map(night -> {
            Instant from = nightStart(night);
            return matchRepository.findByDateBetween(from, from.plus(Duration.ofDays(1))).stream()
                    .filter(m -> m.getType() == MatchType.REGULAR)
                    .toList();
        }).orElse(List.of());
    }

    public Set<String> openingNightMatchIds() {
        return openingNightMatches().stream().map(Match::getId).collect(Collectors.toSet());
    }

    /** Badge « Soirée d'ouverture » : un ticket sur au moins un match de cette soirée. */
    public boolean playedOpeningNight(List<Bet> bets) {
        if (bets.isEmpty()) return false;
        Set<String> ids = openingNightMatchIds();
        return !ids.isEmpty() && bets.stream()
                .flatMap(b -> b.getSelections().stream())
                .anyMatch(s -> ids.contains(s.getMatchId()));
    }

    public Instant now() {
        return clock.instant();
    }
}
