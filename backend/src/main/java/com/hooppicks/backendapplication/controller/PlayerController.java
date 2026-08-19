package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.dto.PlayerDto;
import com.hooppicks.backendapplication.entity.Player;
import com.hooppicks.backendapplication.nba.NbaSyncService;
import com.hooppicks.backendapplication.repository.PlayerRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint public (comme /matches et /leaderboard) : pas besoin de session
 * pour consulter des infos joueurs, ce n'est pas une donnée sensible.
 *
 * Contrairement à une version précédente, cet endpoint n'expose plus jamais
 * la table Player en bloc : balldontlie en free tier ne distingue pas les
 * joueurs actifs des retraités, donc un "SELECT *" mélangerait des milliers
 * de joueurs historiques avec l'effectif actuel. Une recherche par nom est
 * désormais obligatoire ; on regarde d'abord le cache local, et on ne va
 * chercher en direct chez balldontlie que si rien n'y est encore.
 */
@RestController
@RequestMapping("/players")
public class PlayerController {

    private static final int MIN_SEARCH_LENGTH = 2;

    private final PlayerRepository playerRepository;
    private final NbaSyncService nbaSyncService;

    public PlayerController(PlayerRepository playerRepository, NbaSyncService nbaSyncService) {
        this.playerRepository = playerRepository;
        this.nbaSyncService = nbaSyncService;
    }

    @GetMapping
    public List<PlayerDto> searchPlayers(
            @RequestParam String search,
            @RequestParam(required = false) String teamId
    ) {
        if (search == null || search.trim().length() < MIN_SEARCH_LENGTH) {
            return List.of();
        }

        List<Player> local = playerRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(search, search);

        if (teamId != null && !teamId.isBlank()) {
            local = local.stream()
                    .filter(p -> p.getTeam() != null && teamId.equals(p.getTeam().getId()))
                    .toList();
        }

        if (!local.isEmpty()) {
            return local.stream().map(PlayerDto::from).toList();
        }

        // Rien en cache local : on tente un appel live (protégé par le
        // rate limiter interne) et on met en cache pour la prochaine fois.
        List<Player> fresh = nbaSyncService.searchAndCachePlayers(search, teamId);
        return fresh.stream().map(PlayerDto::from).toList();
    }
}