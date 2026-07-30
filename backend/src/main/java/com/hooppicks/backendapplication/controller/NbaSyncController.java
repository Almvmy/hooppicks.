package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.nba.NbaSyncService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

@RestController
@RequestMapping("/admin/nba")
public class NbaSyncController {

    private final NbaSyncService nbaSyncService;

    public NbaSyncController(NbaSyncService nbaSyncService) {
        this.nbaSyncService = nbaSyncService;
    }

    @PostMapping("/sync-teams")
    public Map<String, Integer> syncTeams() {
        return Map.of("teamsSynced", nbaSyncService.syncTeams());
    }

    @PostMapping("/sync-games")
    public Map<String, Integer> syncGames(
            @RequestParam(defaultValue = "3") int daysAhead,
            @RequestParam(required = false) String startDate
    ) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(1);
        List<LocalDate> dates = LongStream.rangeClosed(0, daysAhead)
                .mapToObj(start::plusDays)
                .collect(Collectors.toList());
        return Map.of("gamesSynced", nbaSyncService.syncGames(dates));
    }
}