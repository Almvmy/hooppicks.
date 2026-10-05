package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.dto.PublicStatsDto;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.LeagueRepository;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chiffres de la communauté affichés sur la page d'accueil publique, donc
 * accessible sans session comme /leaderboard. Uniquement des totaux : aucune
 * donnée qui permette de remonter à un utilisateur.
 */
@RestController
@RequestMapping("/stats")
public class PublicStatsController {

    private final UserRepository userRepository;
    private final BetRepository betRepository;
    private final LeagueRepository leagueRepository;
    private final MatchRepository matchRepository;

    public PublicStatsController(UserRepository userRepository, BetRepository betRepository,
                                 LeagueRepository leagueRepository, MatchRepository matchRepository) {
        this.userRepository = userRepository;
        this.betRepository = betRepository;
        this.leagueRepository = leagueRepository;
        this.matchRepository = matchRepository;
    }

    @GetMapping("/public")
    public PublicStatsDto getPublicStats() {
        return new PublicStatsDto(
                userRepository.count(),
                betRepository.count(),
                leagueRepository.count(),
                matchRepository.countByStatus(MatchStatus.FINISHED)
        );
    }
}
