package com.hooppicks.backendapplication.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LeaderboardEntryDtoTest {

    // Même forme que BetRepository.getLeaderboardRaw : id, pseudo, points,
    // paris résolus, paris gagnés, avatar (4 champs), équipe favorite.
    private static Object[] row(String username, long points, long total, long won, String favoriteTeam) {
        return new Object[]{"id-" + username, username, points, total, won, 7, "PG", "orange", "dunk", favoriteTeam};
    }

    @Test
    void egalite_de_points_partage_le_rang_et_le_suivant_saute() {
        List<LeaderboardEntryDto> entries = LeaderboardEntryDto.fromRows(List.of(
                row("a", 500, 4, 3, ""),
                row("b", 300, 4, 2, ""),
                row("c", 300, 2, 1, ""),
                row("d", 100, 5, 1, "")
        ));

        assertThat(entries).extracting(LeaderboardEntryDto::rank).containsExactly(1, 2, 2, 4);
    }

    @Test
    void calcule_le_taux_de_reussite_et_reprend_l_equipe_favorite() {
        LeaderboardEntryDto entry = LeaderboardEntryDto.fromRows(List.<Object[]>of(row("a", 500, 3, 2, "Boston Celtics"))).get(0);

        assertThat(entry.winRate()).isEqualTo(67);
        assertThat(entry.favoriteTeam()).isEqualTo("Boston Celtics");
        assertThat(entry.username()).isEqualTo("a");
    }

    @Test
    void equipe_favorite_vide_devient_null() {
        LeaderboardEntryDto entry = LeaderboardEntryDto.fromRows(List.<Object[]>of(row("a", 0, 0, 0, ""))).get(0);

        assertThat(entry.favoriteTeam()).isNull();
        assertThat(entry.winRate()).isZero();
    }
}
