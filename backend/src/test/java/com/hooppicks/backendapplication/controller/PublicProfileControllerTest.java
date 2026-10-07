package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.badge.BadgeService;
import com.hooppicks.backendapplication.dto.PublicProfileDto;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.favorite.FavoriteTeamService;
import com.hooppicks.backendapplication.leaderboard.LeaderboardService;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.LeagueMembershipRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import com.hooppicks.backendapplication.security.SessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PublicProfileControllerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final BetRepository betRepository = mock(BetRepository.class);
    private final SessionStore sessionStore = mock(SessionStore.class);
    private final FavoriteTeamService favoriteTeamService = mock(FavoriteTeamService.class);
    private final LeaderboardService leaderboardService = mock(LeaderboardService.class);
    private final LeagueMembershipRepository membershipRepository = mock(LeagueMembershipRepository.class);
    private PublicProfileController controller;
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @BeforeEach
    void setUp() {
        controller = new PublicProfileController(userRepository, betRepository, new BadgeService(), sessionStore,
                favoriteTeamService, leaderboardService, membershipRepository);
        when(favoriteTeamService.badgeFacts(any(), any())).thenReturn(FavoriteTeamService.BadgeFacts.NONE);
        when(leaderboardService.standing(any())).thenReturn(new LeaderboardService.Standing(300, 2, 10, 50, 1, 4));
        when(betRepository.getUserStats(any())).thenReturn(List.of());
    }

    private Bet bet(BetStatus status, int stake, int payout) {
        Bet b = new Bet();
        b.setId(status + "-" + stake + "-" + payout);
        b.setStatus(status);
        b.setStake(stake);
        b.setPotentialPayout(payout);
        return b;
    }

    private LeagueMembership membership(String leagueId, String name) {
        League l = new League();
        l.setId(leagueId);
        l.setName(name);
        LeagueMembership m = new LeagueMembership();
        m.setLeague(l);
        return m;
    }

    @Test
    void les_tickets_en_attente_restent_prives_et_le_meilleur_ticket_se_juge_au_benefice() {
        User user = new User();
        user.setId("u2");
        user.setUsername("Meee");
        when(sessionStore.getUserIdFromRequest(request)).thenReturn("u1");
        when(userRepository.findByUsername("Meee")).thenReturn(Optional.of(user));
        when(betRepository.findByUserIdOrderByPlacedAtDesc("u2")).thenReturn(List.of(
                bet(BetStatus.PENDING, 100, 900),
                bet(BetStatus.WON, 500, 600),   // +100
                bet(BetStatus.WON, 50, 400),    // +350 : le meilleur
                bet(BetStatus.LOST, 20, 60)));
        when(membershipRepository.findByUserId("u1")).thenReturn(List.of(membership("l1", "Les potes"), membership("l3", "Bureau")));
        when(membershipRepository.findByUserId("u2")).thenReturn(List.of(membership("l1", "Les potes"), membership("l2", "Famille")));

        ResponseEntity<?> response = controller.getPublicProfile("Meee", request);
        PublicProfileDto dto = (PublicProfileDto) response.getBody();

        assertThat(dto.recentTickets()).hasSize(3).noneMatch(t -> t.status().equals("pending"));
        assertThat(dto.bestTicket().potentialPayout()).isEqualTo(400);
        assertThat(dto.commonLeagues()).containsExactly("Les potes");
        assertThat(dto.isMe()).isFalse();
        assertThat(dto.seasonRank()).isEqualTo(2);
        assertThat(dto.bestStreak()).isEqualTo(2);
    }
}
