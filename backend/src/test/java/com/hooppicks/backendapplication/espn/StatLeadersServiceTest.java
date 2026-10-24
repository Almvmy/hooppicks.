package com.hooppicks.backendapplication.espn;

import com.hooppicks.backendapplication.dto.PlayerLeadersDto;
import com.hooppicks.backendapplication.dto.RosterPlayerDto;
import com.hooppicks.backendapplication.dto.TopPerformanceDto;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.PlayerMatchStat;
import com.hooppicks.backendapplication.entity.RosterPlayer;
import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.repository.PlayerMatchStatRepository;
import com.hooppicks.backendapplication.repository.RosterPlayerRepository;
import com.hooppicks.backendapplication.season.SeasonCalendar;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StatLeadersServiceTest {

    private final RosterPlayerRepository roster = mock(RosterPlayerRepository.class);
    private final PlayerMatchStatRepository stats = mock(PlayerMatchStatRepository.class);
    private final SeasonCalendar calendar = mock(SeasonCalendar.class);
    private final StatLeadersService service = new StatLeadersService(roster, stats, calendar);

    private static RosterPlayer player(String last, String season, int games, double pts) {
        RosterPlayer p = new RosterPlayer();
        p.setId(last);
        p.setFirstName("X");
        p.setLastName(last);
        p.setStatsSeasonLabel(season);
        p.setGamesPlayed(games);
        p.setPointsPerGame(pts);
        p.setFieldGoalPct(50.0 + pts / 10);
        p.setStealsPerGame(1.0);
        p.setBlocksPerGame(1.0);
        return p;
    }

    @Test
    void saison_en_cours_seulement_et_minimum_de_matchs() {
        when(calendar.currentSeason()).thenReturn("2026-27");
        when(roster.findAll()).thenReturn(List.of(
                player("AnciennneSaison", "2025-26", 70, 40.0),
                player("UnSeulMatch", "2026-27", 1, 45.0),
                player("Regulier", "2026-27", 4, 30.0),
                player("Banc", "2026-27", 4, 6.0)));

        PlayerLeadersDto leaders = service.leaders();

        assertThat(leaders.season()).isEqualTo("2026-27");
        assertThat(leaders.minGames()).isEqualTo(2);
        assertThat(leaders.points()).extracting(RosterPlayerDto::lastName).containsExactly("Regulier", "Banc");
        // % aux tirs : 10 points de moyenne au moins.
        assertThat(leaders.fieldGoalPct()).extracting(RosterPlayerDto::lastName).containsExactly("Regulier");
        assertThat(leaders.steals()).hasSize(2);
    }

    @Test
    void avant_le_premier_match_la_derniere_saison_connue() {
        when(calendar.currentSeason()).thenReturn("2026-27");
        when(roster.findAll()).thenReturn(List.of(player("A", "2025-26", 70, 30.0), player("B", "2024-25", 60, 35.0)));

        PlayerLeadersDto leaders = service.leaders();

        assertThat(leaders.season()).isEqualTo("2025-26");
        assertThat(leaders.points()).extracting(RosterPlayerDto::lastName).containsExactly("A");
    }

    @Test
    void top_25_d_une_categorie_par_son_nom_d_api() {
        when(calendar.currentSeason()).thenReturn("2026-27");
        when(roster.findAll()).thenReturn(java.util.stream.IntStream.range(0, 30)
                .mapToObj(i -> player("P" + i, "2026-27", 3, 10.0 + i)).toList());

        assertThat(service.leaders("points", 25).orElseThrow().players()).hasSize(25)
                .first().extracting(RosterPlayerDto::lastName).isEqualTo("P29");
        assertThat(service.leaders("fieldGoalPct", 100).orElseThrow().players()).hasSize(25);
        assertThat(service.leaders("dunks", 25)).isEmpty();
    }

    @Test
    void minimum_de_matchs_la_moitie_du_plus_assidu() {
        assertThat(StatLeadersService.minGames(0)).isEqualTo(1);
        assertThat(StatLeadersService.minGames(3)).isEqualTo(2);
        assertThat(StatLeadersService.minGames(82)).isEqualTo(41);
    }

    private static PlayerMatchStat line(Match m, String name, String team, String minutes, int pts, int fgm, int fga) {
        PlayerMatchStat s = new PlayerMatchStat();
        s.setMatch(m);
        s.setPlayerName(name);
        s.setTeamAbbreviation(team);
        s.setMinutes(minutes);
        s.setPoints(pts);
        s.setFieldGoalsMade(fgm);
        s.setFieldGoalsAttempted(fga);
        s.setRebounds(5);
        s.setAssists(5);
        return s;
    }

    private static Match match(String id, String date, String home, String away) {
        Match m = new Match();
        m.setId(id);
        m.setDate(Instant.parse(date));
        Team h = new Team();
        h.setAbbreviation(home);
        Team a = new Team();
        a.setAbbreviation(away);
        m.setHomeTeam(h);
        m.setAwayTeam(a);
        return m;
    }

    @Test
    void perfs_de_la_derniere_soiree_classees_par_game_score() {
        when(calendar.now()).thenReturn(Instant.parse("2026-10-24T10:00:00Z"));
        Match tonight = match("m1", "2026-10-24T00:00:00Z", "SAS", "OKC");
        Match before = match("m0", "2026-10-22T23:00:00Z", "NYK", "PHI");
        RosterPlayer sga = player("Gilgeous-Alexander", "2026-27", 3, 32.0);
        sga.setFirstName("Shai");
        when(roster.findAll()).thenReturn(List.of(sga));
        when(stats.findFinishedBetween(any(), any())).thenReturn(List.of(
                line(tonight, "Shai Gilgeous-Alexander", "OKC", "36", 34, 12, 20),
                line(tonight, "Victor Wembanyama", "SA", "34", 30, 10, 25),
                line(tonight, "Bench Guy", "SA", "--", 0, 0, 0),
                line(before, "Joel Embiid", "PHI", "33", 50, 18, 25)));

        List<TopPerformanceDto> top = service.lastNight();

        // La soirée précédente (Embiid) ne compte pas ; le joueur sans minutes non plus.
        assertThat(top).extracting(TopPerformanceDto::playerName).containsExactly("Shai Gilgeous-Alexander", "Victor Wembanyama");
        assertThat(top.get(0).opponentAbbreviation()).isEqualTo("SAS");
        assertThat(top.get(0).player()).isNotNull();
        // Sigle ESPN « SA » converti au nôtre.
        assertThat(top.get(1).teamAbbreviation()).isEqualTo("SAS");
        assertThat(top.get(1).opponentAbbreviation()).isEqualTo("OKC");
    }
}
