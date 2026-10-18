package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.season.SeasonCalendar;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Public comme /matches : le compte à rebours se voit sans compte.
@RestController
public class SeasonLaunchController {

    private final SeasonCalendar calendar;

    public SeasonLaunchController(SeasonCalendar calendar) {
        this.calendar = calendar;
    }

    public record Launch(String season, String openerMatchId, String openerDate, String openingNightStart,
                         String openingNightEnd) {}

    /** Premier match de la saison régulière et soirée d'ouverture ; 204 tant que le calendrier est inconnu. */
    @GetMapping("/season/launch")
    public ResponseEntity<Launch> launch() {
        String season = calendar.currentSeason();
        return calendar.opener(season).map(m -> {
            var night = SeasonCalendar.nightOf(m.getDate());
            var start = SeasonCalendar.nightStart(night);
            return ResponseEntity.ok(new Launch(season, m.getId(), m.getDate().toString(), start.toString(),
                    start.plus(java.time.Duration.ofDays(1)).toString()));
        }).orElseGet(() -> ResponseEntity.noContent().build());
    }
}
