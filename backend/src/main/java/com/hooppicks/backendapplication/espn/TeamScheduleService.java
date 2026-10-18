package com.hooppicks.backendapplication.espn;

import com.hooppicks.backendapplication.season.SeasonCalendar;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Calendrier de saison régulière d'une équipe, pour l'abonnement agenda
 * (fichier .ics de l'app). Lu chez ESPN : notre base n'a que les matchs des
 * jours qui viennent. Gardé 6 h par équipe (la réponse ESPN pèse ~1,5 Mo) ;
 * ESPN injoignable : dernière version connue.
 */
@Service
public class TeamScheduleService {

    static final Duration TTL = Duration.ofHours(6);

    private final EspnStatsClient client;
    private final SeasonCalendar calendar;
    private final Clock clock = Clock.systemUTC();
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(Instant at, List<EspnStatsClient.ScheduledGame> games) {}

    public TeamScheduleService(EspnStatsClient client, SeasonCalendar calendar) {
        this.client = client;
        this.calendar = calendar;
    }

    public Optional<List<EspnStatsClient.ScheduledGame>> schedule(String abbreviation) {
        Cached hit = cache.get(abbreviation);
        if (hit != null && hit.at().plus(TTL).isAfter(clock.instant())) return Optional.of(hit.games());
        int seasonYear = Integer.parseInt(calendar.currentSeason().substring(0, 4)) + 1;
        Optional<List<EspnStatsClient.ScheduledGame>> fresh = client.fetchTeamSchedule(abbreviation, seasonYear, 2);
        if (fresh.isPresent()) {
            cache.put(abbreviation, new Cached(clock.instant(), fresh.get()));
            return fresh;
        }
        return hit == null ? Optional.empty() : Optional.of(hit.games());
    }
}
