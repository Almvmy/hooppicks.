package com.hooppicks.backendapplication.bet;

import com.hooppicks.backendapplication.bet.LegEvaluator.LegResult;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.repository.PlayerMatchStatRepository;
import com.hooppicks.backendapplication.repository.RosterPlayerRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlayerPropsServiceTest {

    private final RosterPlayerRepository rosterRepository = mock(RosterPlayerRepository.class);
    private final PlayerMatchStatRepository statRepository = mock(PlayerMatchStatRepository.class);
    private final PlayerPropsService service = new PlayerPropsService(rosterRepository, statRepository);

    private static final Instant TIP_OFF = Instant.parse("2026-10-22T00:00:00Z");

    private Match match() {
        Team home = new Team();
        home.setId("nyk");
        home.setAbbreviation("NYK");
        Team away = new Team();
        away.setId("bos");
        away.setAbbreviation("BOS");
        Match m = new Match();
        m.setId("m1");
        m.setHomeTeam(home);
        m.setAwayTeam(away);
        m.setType(MatchType.REGULAR);
        m.setDate(TIP_OFF);
        return m;
    }

    private RosterPlayer player(String id, String first, String last, double ppg, int games, String injury) {
        RosterPlayer p = new RosterPlayer();
        p.setId(id);
        p.setFirstName(first);
        p.setLastName(last);
        p.setPointsPerGame(ppg);
        p.setGamesPlayed(games);
        p.setInjuryStatus(injury);
        return p;
    }

    private BetSelection over(String name, double line) {
        BetSelection s = new BetSelection();
        s.setMarket(PlayerPropsService.MARKET);
        s.setOutcome("over");
        s.setPlayerName(name);
        s.setPropLine(line);
        return s;
    }

    private PlayerMatchStat stat(String name, int points, String minutes) {
        PlayerMatchStat s = new PlayerMatchStat();
        s.setPlayerName(name);
        s.setPoints(points);
        s.setMinutes(minutes);
        return s;
    }

    @Test
    void la_ligne_est_toujours_un_demi_point() {
        assertThat(PlayerPropsService.line(26.8)).isEqualTo(26.5);
        assertThat(PlayerPropsService.line(26.2)).isEqualTo(26.5);
        assertThat(PlayerPropsService.line(27.0)).isEqualTo(27.5);
    }

    @Test
    void deux_meilleurs_marqueurs_par_equipe_hors_blesses_et_petits_echantillons() {
        when(rosterRepository.findByTeamIdOrderByLastNameAsc("nyk")).thenReturn(List.of(
                player("1", "Jalen", "Brunson", 26.8, 40, null),
                player("2", "Karl-Anthony", "Towns", 24.1, 40, "Out"),
                player("3", "Mikal", "Bridges", 17.9, 40, null),
                player("4", "Rookie", "Chaud", 30.0, 3, null),
                player("5", "OG", "Anunoby", 16.0, 40, "Day-To-Day")));
        when(rosterRepository.findByTeamIdOrderByLastNameAsc("bos")).thenReturn(List.of());

        assertThat(service.propsFor(match()).stream().filter(p -> p.market().equals("player_points")))
                .extracting(PlayerPropsService.PlayerProp::playerName)
                .containsExactly("Jalen Brunson", "Mikal Bridges");
    }

    @Test
    void rebonds_et_passes_seulement_pour_les_joueurs_dont_c_est_le_role() {
        RosterPlayer brunson = player("1", "Jalen", "Brunson", 26.8, 40, null);
        brunson.setReboundsPerGame(3.4);
        brunson.setAssistsPerGame(7.3);
        RosterPlayer towns = player("2", "Karl-Anthony", "Towns", 24.1, 40, null);
        towns.setReboundsPerGame(12.8);
        towns.setAssistsPerGame(3.1);
        when(rosterRepository.findByTeamIdOrderByLastNameAsc("nyk")).thenReturn(List.of(brunson, towns));
        when(rosterRepository.findByTeamIdOrderByLastNameAsc("bos")).thenReturn(List.of());

        List<PlayerPropsService.PlayerProp> props = service.propsFor(match());
        assertThat(props).filteredOn(p -> p.market().equals("player_rebounds"))
                .extracting(PlayerPropsService.PlayerProp::playerName).containsExactly("Karl-Anthony Towns");
        assertThat(props).filteredOn(p -> p.market().equals("player_assists"))
                .extracting(PlayerPropsService.PlayerProp::playerName).containsExactly("Jalen Brunson");
        // Moyenne de tirs à 3 points pas encore importée : pas de ligne plutôt qu'une ligne fausse.
        assertThat(props).filteredOn(p -> p.market().equals("player_threes")).isEmpty();
        // 26,8 + 3,4 + 7,3 = 37,5 → ligne 37,5
        assertThat(props).filteredOn(p -> p.market().equals("player_pra") && p.playerId().equals("1"))
                .extracting(PlayerPropsService.PlayerProp::line).containsExactly(37.5);
    }

    @Test
    void chaque_marche_est_regle_sur_sa_statistique() {
        PlayerMatchStat line = stat("Nikola Jokic", 25, "35");
        line.setRebounds(13);
        line.setAssists(9);
        when(statRepository.findByMatchIdOrderByPointsDesc("m1")).thenReturn(List.of(line));
        Instant after = TIP_OFF.plus(Duration.ofHours(3));

        BetSelection rebounds = over("Nikola Jokic", 12.5);
        rebounds.setMarket("player_rebounds");
        assertThat(service.evaluate(rebounds, match(), after)).contains(LegResult.WIN);

        BetSelection assists = over("Nikola Jokic", 9.5);
        assists.setMarket("player_assists");
        assertThat(service.evaluate(assists, match(), after)).contains(LegResult.LOSE);

        BetSelection threes = over("Nikola Jokic", 1.5);
        threes.setMarket("player_threes");
        line.setThreePointsMade(1);
        assertThat(service.evaluate(threes, match(), after)).contains(LegResult.LOSE);

        BetSelection pra = over("Nikola Jokic", 46.5); // 25 + 13 + 9 = 47
        pra.setMarket("player_pra");
        assertThat(service.evaluate(pra, match(), after)).contains(LegResult.WIN);
    }

    @Test
    void pas_de_paris_joueurs_en_presaison() {
        Match m = match();
        m.setType(MatchType.PRESEASON);
        assertThat(service.propsFor(m)).isEmpty();
    }

    @Test
    void resultat_lu_sur_la_feuille_de_match_accents_compris() {
        when(statRepository.findByMatchIdOrderByPointsDesc("m1")).thenReturn(List.of(stat("Luka Dončić", 31, "36")));
        assertThat(service.evaluate(over("Luka Doncic", 29.5), match(), TIP_OFF.plus(Duration.ofHours(3))))
                .contains(LegResult.WIN);
        assertThat(service.evaluate(over("Luka Doncic", 31.5), match(), TIP_OFF.plus(Duration.ofHours(3))))
                .contains(LegResult.LOSE);
    }

    @Test
    void joueur_qui_ne_joue_pas_rembourse() {
        when(statRepository.findByMatchIdOrderByPointsDesc("m1")).thenReturn(List.of(stat("Jalen Brunson", 0, "--")));
        assertThat(service.evaluate(over("Jalen Brunson", 26.5), match(), TIP_OFF)).contains(LegResult.PUSH);
        assertThat(service.evaluate(over("Mikal Bridges", 17.5), match(), TIP_OFF)).contains(LegResult.PUSH);
    }

    @Test
    void sans_feuille_on_attend_puis_on_rembourse() {
        when(statRepository.findByMatchIdOrderByPointsDesc("m1")).thenReturn(List.of());
        assertThat(service.evaluate(over("Jalen Brunson", 26.5), match(), TIP_OFF.plus(Duration.ofHours(4)))).isEmpty();
        assertThat(service.evaluate(over("Jalen Brunson", 26.5), match(), TIP_OFF.plus(Duration.ofHours(19))))
                .isEqualTo(Optional.of(LegResult.PUSH));
    }
}
