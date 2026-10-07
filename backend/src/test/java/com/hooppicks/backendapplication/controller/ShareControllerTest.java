package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.badge.BadgeService;
import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.entity.BetStatus;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.leaderboard.LeaderboardService;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShareControllerTest {

    private final BetRepository betRepository = mock(BetRepository.class);
    private final ShareController controller = new ShareController(betRepository, mock(UserRepository.class),
            mock(LeaderboardService.class), new BadgeService());

    private Bet bet(BetStatus status) {
        User u = new User();
        u.setUsername("Meee");
        Bet b = new Bet();
        b.setUser(u);
        b.setStatus(status);
        b.setStake(50);
        b.setPotentialPayout(400);
        return b;
    }

    @Test
    void un_ticket_en_attente_ne_se_partage_pas() {
        when(betRepository.findById("t1")).thenReturn(Optional.of(bet(BetStatus.PENDING)));
        assertThat(controller.ticket("t1").getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void un_ticket_regle_se_partage_sans_l_email() {
        when(betRepository.findById("t1")).thenReturn(Optional.of(bet(BetStatus.WON)));
        var body = (ShareController.SharedTicket) controller.ticket("t1").getBody();
        assertThat(body.username()).isEqualTo("Meee");
        assertThat(body.status()).isEqualTo("won");
    }
}
