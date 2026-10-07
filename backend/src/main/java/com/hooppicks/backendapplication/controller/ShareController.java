package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.badge.BadgeService;
import com.hooppicks.backendapplication.dto.BetSelectionDto;
import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.entity.BetStatus;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.leaderboard.LeaderboardService;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Données des liens de partage (aperçu WhatsApp, page publique) : SANS
 * session, puisque c'est l'aperçu d'un lien envoyé à quelqu'un qui n'a pas
 * forcément de compte. N'expose que le strict nécessaire :
 * - un ticket déjà réglé (jamais en attente : on ne doit pas pouvoir copier
 *   un pari en cours), repéré par son id UUID impossible à deviner ;
 * - la carte d'un joueur : ce que le classement, déjà public, montre de lui.
 * Jamais l'email ni l'historique complet.
 */
@RestController
@RequestMapping("/share")
public class ShareController {

    private final BetRepository betRepository;
    private final UserRepository userRepository;
    private final LeaderboardService leaderboardService;
    private final BadgeService badgeService;

    public ShareController(BetRepository betRepository, UserRepository userRepository,
                           LeaderboardService leaderboardService, BadgeService badgeService) {
        this.betRepository = betRepository;
        this.userRepository = userRepository;
        this.leaderboardService = leaderboardService;
        this.badgeService = badgeService;
    }

    public record SharedTicket(String username, int avatarNumber, String avatarPosition, String avatarColorway,
                               String avatarIcon, String status, int stake, double totalOdds, int potentialPayout,
                               List<BetSelectionDto> selections, String resolvedAt) {}

    public record SharedPlayer(String username, int avatarNumber, String avatarPosition, String avatarColorway,
                               String avatarIcon, String favoriteTeam, long seasonPoints, Integer seasonRank,
                               int seasonPlayers, int winRate, int settledTickets, int bestStreak,
                               long weekPoints, Integer weekRank, int weekPlayers, int currentStreak) {}

    @GetMapping("/tickets/{id}")
    public ResponseEntity<?> ticket(@PathVariable String id) {
        Bet bet = betRepository.findById(id).orElse(null);
        if (bet == null || bet.getStatus() == BetStatus.PENDING) return ResponseEntity.notFound().build();
        User u = bet.getUser();
        return ResponseEntity.ok(new SharedTicket(u.getUsername(), u.getAvatarNumber(), u.getAvatarPosition(),
                u.getAvatarColorway(), u.getAvatarIcon(), bet.getStatus().name().toLowerCase(), bet.getStake(),
                bet.getTotalOdds(), bet.getPotentialPayout(),
                bet.getSelections().stream().map(BetSelectionDto::from).toList(),
                bet.getResolvedAt() == null ? null : bet.getResolvedAt().toString()));
    }

    @GetMapping("/players/{username}")
    public ResponseEntity<?> player(@PathVariable String username) {
        User u = userRepository.findByUsername(username).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();
        List<Object[]> stats = betRepository.getUserStats(u.getId());
        long total = 0, won = 0;
        if (!stats.isEmpty() && stats.get(0)[0] != null) {
            total = (Long) stats.get(0)[0];
            won = stats.get(0)[1] != null ? (Long) stats.get(0)[1] : 0;
        }
        LeaderboardService.Standing s = leaderboardService.standing(u.getId());
        List<Bet> bets = betRepository.findByUserIdOrderByPlacedAtDesc(u.getId());
        return ResponseEntity.ok(new SharedPlayer(u.getUsername(), u.getAvatarNumber(), u.getAvatarPosition(),
                u.getAvatarColorway(), u.getAvatarIcon(), u.getFavoriteTeam(), s.seasonPoints(), s.seasonRank(),
                s.seasonPlayers(), total == 0 ? 0 : (int) Math.round(won * 100.0 / total), (int) total,
                badgeService.computeBestWinStreak(bets), s.weekPoints(), s.weekRank(), s.weekPlayers(),
                badgeService.computeWinStreak(bets)));
    }
}
