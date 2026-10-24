package com.hooppicks.backendapplication.espn;

import com.hooppicks.backendapplication.entity.RosterPlayer;
import com.hooppicks.backendapplication.repository.PlayerMatchStatRepository;
import com.hooppicks.backendapplication.repository.RosterPlayerRepository;
import com.hooppicks.backendapplication.season.SeasonCalendar;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SeasonAveragesServiceTest {

    private static RosterPlayer lastSeason(String first, String last) {
        RosterPlayer p = new RosterPlayer();
        p.setId("id-" + last);
        p.setFirstName(first);
        p.setLastName(last);
        p.setStatsSeasonLabel("2025-26");
        p.setGamesPlayed(68);
        p.setPointsPerGame(31.1);
        p.setReboundsPerGame(5.0);
        p.setAssistsPerGame(6.6);
        p.setMinutesPerGame(33.2);
        return p;
    }

    // nom, matchs, titularisations, minutes, pts, reb, pd, int, ctr, bp, tirs, 3 pts, lancers
    private static Object[] row(String name, int games, int points) {
        return new Object[]{name, games, games, 70, points, 10, 12, 3, 2, 5, 19, 46, 4, 11, 20, 22};
    }

    @Test
    void premier_match_de_la_saison_la_saison_passee_devient_la_precedente() {
        RosterPlayer sga = lastSeason("Shai", "Gilgeous-Alexander");

        boolean changed = SeasonAveragesService.apply(sga,
                SeasonAveragesService.totalsOf(List.of(2, 2, 71, 64, 10, 12, 3, 2, 5, 19, 46, 4, 11, 20, 22)), "2026-27");

        assertThat(changed).isTrue();
        assertThat(sga.getStatsSeasonLabel()).isEqualTo("2026-27");
        assertThat(sga.getGamesPlayed()).isEqualTo(2);
        assertThat(sga.getPointsPerGame()).isEqualTo(32.0);
        assertThat(sga.getMinutesPerGame()).isEqualTo(35.5);
        assertThat(sga.getFieldGoalPct()).isEqualTo(41.3);
        assertThat(sga.getThreePointersMadePerGame()).isEqualTo(2.0);
        assertThat(sga.getPrevSeasonLabel()).isEqualTo("2025-26");
        assertThat(sga.getPrevGamesPlayed()).isEqualTo(68);
        assertThat(sga.getPrevPointsPerGame()).isEqualTo(31.1);
        assertThat(sga.getPrevMinutesPerGame()).isEqualTo(33.2);
    }

    @Test
    void match_suivant_la_saison_precedente_ne_bouge_plus_et_rien_ne_se_reecrit_pour_rien() {
        RosterPlayer sga = lastSeason("Shai", "Gilgeous-Alexander");
        SeasonAveragesService.apply(sga, SeasonAveragesService.totalsOf(List.of(2, 2, 71, 64, 10, 12, 3, 2, 5, 19, 46, 4, 11, 20, 22)), "2026-27");

        SeasonAveragesService.apply(sga, SeasonAveragesService.totalsOf(List.of(3, 3, 105, 90, 15, 18, 4, 3, 7, 29, 66, 6, 16, 26, 29)), "2026-27");
        assertThat(sga.getGamesPlayed()).isEqualTo(3);
        assertThat(sga.getPointsPerGame()).isEqualTo(30.0);
        assertThat(sga.getPrevSeasonLabel()).isEqualTo("2025-26");
        assertThat(sga.getPrevPointsPerGame()).isEqualTo(31.1);

        boolean again = SeasonAveragesService.apply(sga,
                SeasonAveragesService.totalsOf(List.of(3, 3, 105, 90, 15, 18, 4, 3, 7, 29, 66, 6, 16, 26, 29)), "2026-27");
        assertThat(again).isFalse();
    }

    @Test
    void refresh_relie_les_feuilles_aux_joueurs_par_nom_normalise() {
        PlayerMatchStatRepository stats = mock(PlayerMatchStatRepository.class);
        RosterPlayerRepository roster = mock(RosterPlayerRepository.class);
        SeasonCalendar calendar = mock(SeasonCalendar.class);
        when(calendar.currentSeason()).thenReturn("2026-27");
        RosterPlayer luka = lastSeason("Luka", "Dončić");
        RosterPlayer injured = lastSeason("Kyrie", "Irving");
        when(roster.findAll()).thenReturn(List.of(luka, injured));
        // Feuille ESPN sans accent, et un joueur échangé apparaît sous deux lignes.
        when(stats.seasonTotals(any())).thenReturn(List.of(row("Luka Doncic", 2, 70), row("Luka Doncic ", 1, 30)));

        int updated = new SeasonAveragesService(stats, roster, calendar).refresh();

        assertThat(updated).isEqualTo(1);
        assertThat(luka.getStatsSeasonLabel()).isEqualTo("2026-27");
        assertThat(luka.getGamesPlayed()).isEqualTo(3);
        assertThat(luka.getPointsPerGame()).isEqualTo(33.3);
        // Pas encore joué cette saison : intact.
        assertThat(injured.getStatsSeasonLabel()).isEqualTo("2025-26");
        assertThat(injured.getGamesPlayed()).isEqualTo(68);
    }

    @Test
    void la_synchro_espn_n_apporte_qu_une_saison_plus_recente() {
        assertThat(EspnPlayerStatsService.isNewer("2026-27", "2025-26")).isTrue();
        assertThat(EspnPlayerStatsService.isNewer("2026-27", null)).isTrue();
        assertThat(EspnPlayerStatsService.isNewer("2026-27", "2026-27")).isFalse();
        assertThat(EspnPlayerStatsService.isNewer("2025-26", "2026-27")).isFalse();
        assertThat(EspnPlayerStatsService.isNewer(null, "2025-26")).isFalse();
    }
}
