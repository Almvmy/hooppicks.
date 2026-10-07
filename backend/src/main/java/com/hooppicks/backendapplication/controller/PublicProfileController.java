package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.badge.BadgeService;
import com.hooppicks.backendapplication.dto.PublicProfileDto;
import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.dto.PlacedBetDto;
import com.hooppicks.backendapplication.entity.BetStatus;
import com.hooppicks.backendapplication.entity.LeagueMembership;
import com.hooppicks.backendapplication.favorite.FavoriteTeamService;
import com.hooppicks.backendapplication.leaderboard.LeaderboardService;
import com.hooppicks.backendapplication.repository.LeagueMembershipRepository;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Profil consultable par n'importe quel utilisateur connecté (classement,
 * membres de ligue, fil d'activité) : jamais anonyme, contrairement à
 * /matches ou /leaderboard. Ne renvoie que le sous-ensemble public de User,
 * voir PublicProfileDto.
 */
@RestController
@RequestMapping("/users")
public class PublicProfileController {

    private final UserRepository userRepository;
    private final BetRepository betRepository;
    private final BadgeService badgeService;
    private final SessionStore sessionStore;
    private final FavoriteTeamService favoriteTeamService;
    private final LeaderboardService leaderboardService;
    private final LeagueMembershipRepository membershipRepository;

    private static final int RECENT_TICKETS = 10;

    public PublicProfileController(UserRepository userRepository, BetRepository betRepository,
                                    BadgeService badgeService, SessionStore sessionStore,
                                    FavoriteTeamService favoriteTeamService, LeaderboardService leaderboardService,
                                    LeagueMembershipRepository membershipRepository) {
        this.leaderboardService = leaderboardService;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.betRepository = betRepository;
        this.badgeService = badgeService;
        this.sessionStore = sessionStore;
        this.favoriteTeamService = favoriteTeamService;
    }

    @GetMapping("/{username}")
    public ResponseEntity<?> getPublicProfile(@PathVariable String username, HttpServletRequest request) {
        String viewerId = sessionStore.getUserIdFromRequest(request);
        if (viewerId == null) return ResponseEntity.status(401).build();

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        List<Bet> bets = betRepository.findByUserIdOrderByPlacedAtDesc(user.getId());
        List<Object[]> stats = betRepository.getUserStats(user.getId());
        long totalBets = 0;
        long wonBets = 0;
        if (!stats.isEmpty() && stats.get(0)[0] != null) {
            totalBets = (Long) stats.get(0)[0];
            wonBets = stats.get(0)[1] != null ? (Long) stats.get(0)[1] : 0;
        }
        int winRate = totalBets == 0 ? 0 : (int) Math.round((wonBets * 100.0) / totalBets);

        List<Bet> settled = bets.stream().filter(b -> b.getStatus() != BetStatus.PENDING).toList();
        // Meilleur ticket = plus gros bénéfice, comme au classement.
        PlacedBetDto best = settled.stream()
                .filter(b -> b.getStatus() == BetStatus.WON)
                .max(java.util.Comparator.comparingInt(b -> b.getPotentialPayout() - b.getStake()))
                .map(PlacedBetDto::from).orElse(null);

        boolean isMe = viewerId.equals(user.getId());
        java.util.Set<String> viewerLeagues = isMe ? java.util.Set.of() : membershipRepository.findByUserId(viewerId).stream()
                .map(m -> m.getLeague().getId()).collect(java.util.stream.Collectors.toSet());
        List<String> common = membershipRepository.findByUserId(user.getId()).stream()
                .map(LeagueMembership::getLeague)
                .filter(l -> viewerLeagues.contains(l.getId()))
                .map(l -> l.getName()).sorted().toList();

        LeaderboardService.Standing standing = leaderboardService.standing(user.getId());
        PublicProfileDto.Extras extras = new PublicProfileDto.Extras(
                user.getCreatedAt() == null ? null : user.getCreatedAt().toString(),
                standing.seasonPoints(), standing.seasonRank(), standing.seasonPlayers(),
                standing.weekPoints(), standing.weekRank(), standing.weekPlayers(),
                badgeService.computeWinStreak(bets), badgeService.computeBestWinStreak(bets),
                settled.stream().limit(RECENT_TICKETS).map(PlacedBetDto::from).toList(),
                best, common, isMe);

        return ResponseEntity.ok(PublicProfileDto.from(user, winRate, (int) totalBets, badgeService.computeBadges(bets,
                favoriteTeamService.badgeFacts(user.getFavoriteTeam(), bets)), extras));
    }
}
