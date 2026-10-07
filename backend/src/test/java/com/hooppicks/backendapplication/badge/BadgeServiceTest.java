package com.hooppicks.backendapplication.badge;

import com.hooppicks.backendapplication.dto.BadgeDto;
import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.entity.BetStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BadgeServiceTest {

    private final BadgeService badgeService = new BadgeService();

    private Bet bet(BetStatus status, int stake, int payout) {
        Bet b = new Bet();
        b.setStatus(status);
        b.setStake(stake);
        b.setPotentialPayout(payout);
        return b;
    }

    private boolean unlocked(List<BadgeDto> badges, String id) {
        return badges.stream().filter(b -> b.id().equals(id)).findFirst().orElseThrow().unlocked();
    }

    @Test
    void main_chaude_reste_debloquee_apres_une_defaite() {
        // Du plus récent au plus ancien : une défaite, puis 3 victoires avant.
        List<Bet> bets = List.of(
                bet(BetStatus.LOST, 10, 20),
                bet(BetStatus.WON, 10, 20),
                bet(BetStatus.VOID, 10, 20),
                bet(BetStatus.WON, 10, 20),
                bet(BetStatus.WON, 10, 20));

        List<BadgeDto> badges = badgeService.computeBadges(bets);

        assertThat(unlocked(badges, "hot_streak_3")).isTrue();
        assertThat(unlocked(badges, "hot_streak_5")).isFalse();
        assertThat(badgeService.computeWinStreak(bets)).isZero(); // la série en cours, elle, est cassée
    }

    @Test
    void gros_coup_se_juge_au_benefice_pas_au_gain_brut() {
        assertThat(unlocked(badgeService.computeBadges(List.of(bet(BetStatus.WON, 500, 515))), "big_win")).isFalse();
        assertThat(unlocked(badgeService.computeBadges(List.of(bet(BetStatus.WON, 100, 600))), "big_win")).isTrue();
    }
}
