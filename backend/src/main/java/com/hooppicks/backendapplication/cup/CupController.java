package com.hooppicks.backendapplication.cup;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public, comme le classement officiel de la NBA : aucune donnée de joueur. */
@RestController
public class CupController {

    private final CupStandingsService service;

    public CupController(CupStandingsService service) {
        this.service = service;
    }

    @GetMapping("/cup/standings")
    public CupStandingsService.Standings standings() {
        return service.standings();
    }
}
