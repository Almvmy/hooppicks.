package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.nba.NbaSyncService;
import com.hooppicks.backendapplication.repository.PlayerRepository;
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
    private final PlayerRepository playerRepository;

    public NbaSyncController(NbaSyncService nbaSyncService, PlayerRepository playerRepository) {
        this.nbaSyncService = nbaSyncService;
        this.playerRepository = playerRepository;
    }

    @PostMapping("/sync-teams")
    public Map<String, Integer> syncTeams() {
        return Map.of("teamsSynced", nbaSyncService.syncTeams());
    }

    @PostMapping("/sync-players")
    public Map<String, String> syncPlayers() {
        // Lancé en tâche de fond (cf. NbaSyncService.syncPlayersAsync) pour ne
        // pas garder la requête HTTP ouverte pendant ~10 minutes : le proxy
        // Railway la coupait avant la fin, laissant certaines équipes non
        // traitées. On répond tout de suite ; le job continue côté serveur.
        nbaSyncService.syncPlayersAsync();
        return Map.of(
                "status", "started",
                "message", "Synchro lancée en tâche de fond (~10 min, ~20s entre chaque équipe). "
                        + "Suis la progression via GET /admin/nba/players-count."
        );
    }

    @GetMapping("/players-count")
    public Map<String, Long> playersCount() {
        return Map.of("playersCount", playerRepository.count());
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
        return Map.of("gamesSynced", nbaSyncService.syncGames(dates).gamesSynced());
    }
}