package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.boost.BoostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Public comme /matches : la cote boostée se voit sans compte.
@RestController
public class BoostController {

    private final BoostService boostService;

    public BoostController(BoostService boostService) {
        this.boostService = boostService;
    }

    /** Boost de la soirée NBA en cours ; 204 s'il n'y a aucun match à booster. */
    @GetMapping("/boost")
    public ResponseEntity<BoostService.Boost> current() {
        return boostService.current().map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }
}
