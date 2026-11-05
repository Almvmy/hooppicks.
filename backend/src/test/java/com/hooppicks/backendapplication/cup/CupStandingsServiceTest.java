package com.hooppicks.backendapplication.cup;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.entity.MatchType;
import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CupStandingsServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-07T12:00:00Z");
    private final MatchRepository matches = mock(MatchRepository.class);
    private final TeamRepository teams = mock(TeamRepository.class);
    private final List<Match> played = new ArrayList<>();

    private static Team team(String abbreviation) {
        Team t = new Team();
        t.setAbbreviation(abbreviation);
        t.setName("Nom " + abbreviation);
        return t;
    }

    private Match game(String home, int homeScore, String away, int awayScore, String stage) {
        Match m = new Match();
        m.setHomeTeam(team(home));
        m.setAwayTeam(team(away));
        m.setHomeScore(homeScore);
        m.setAwayScore(awayScore);
        m.setStageLabel(stage);
        m.setType(MatchType.NBA_CUP);
        played.add(m);
        return m;
    }

    private void poule(String home, int homeScore, String away, int awayScore) {
        game(home, homeScore, away, awayScore, "Coupe NBA · Phase de groupes");
    }

    private CupStandingsService.Standings standings(Instant now) {
        return new CupStandingsService(matches, teams, Clock.fixed(now, BankrollService.ZONE)).standings();
    }

    private CupStandingsService.Group group(CupStandingsService.Standings s, String conference, String name) {
        return s.groups().stream().filter(g -> g.conference().equals(conference) && g.name().equals(name)).findFirst().orElseThrow();
    }

    @BeforeEach
    void setUp() {
        when(matches.findByTypeAndStatusAndDateAfter(eq(MatchType.NBA_CUP), eq(MatchStatus.FINISHED), any())).thenReturn(played);
        when(teams.findAll()).thenReturn(List.of(team("NYK"), team("CLE"), team("PHI"), team("MIA"), team("IND")));
    }

    @Test
    void six_poules_de_cinq_les_30_equipes_une_seule_fois() {
        CupStandingsService.Standings s = standings(NOW);

        assertThat(s.groups()).hasSize(6);
        assertThat(s.groups()).allSatisfy(g -> assertThat(g.teams()).hasSize(5));
        assertThat(new HashSet<>(s.groups().stream().flatMap(g -> g.teams().stream()).map(CupStandingsService.TeamRow::abbreviation).toList())).hasSize(30);
    }

    @Test
    void victoires_defaites_ecart_et_points_marques() {
        poule("NYK", 110, "CLE", 100);
        poule("MIA", 95, "NYK", 101);

        CupStandingsService.TeamRow knicks = group(standings(NOW), "Est", "B").teams().get(0);

        assertThat(knicks.abbreviation()).isEqualTo("NYK");
        assertThat(knicks.wins()).isEqualTo(2);
        assertThat(knicks.losses()).isZero();
        assertThat(knicks.diff()).isEqualTo(16);
        assertThat(knicks.pointsFor()).isEqualTo(211);
        assertThat(knicks.name()).isEqualTo("Nom NYK");
    }

    @Test
    void a_egalite_de_bilan_la_confrontation_directe_passe_avant_l_ecart() {
        // CLE a un meilleur écart, mais NYK l'a battu.
        poule("NYK", 101, "CLE", 100);
        poule("CLE", 140, "MIA", 90);
        poule("IND", 110, "NYK", 100);
        poule("PHI", 100, "CLE", 105);
        poule("NYK", 102, "PHI", 100);
        poule("IND", 100, "CLE", 90);

        List<String> order = group(standings(NOW), "Est", "B").teams().stream().map(CupStandingsService.TeamRow::abbreviation).toList();

        assertThat(order.indexOf("NYK")).isLessThan(order.indexOf("CLE"));
    }

    @Test
    void les_matchs_a_elimination_directe_ne_comptent_pas_dans_les_poules() {
        game("NYK", 120, "CLE", 80, "Coupe NBA · Quarts de finale");

        assertThat(group(standings(NOW), "Est", "B").teams()).allSatisfy(t -> assertThat(t.wins() + t.losses()).isZero());
    }

    @Test
    void personne_n_est_declare_qualifie_tant_que_la_conference_n_a_pas_fini() {
        poule("NYK", 110, "CLE", 100);

        assertThat(standings(NOW).groups()).allSatisfy(g -> assertThat(g.teams()).allSatisfy(t -> assertThat(t.status()).isNull()));
    }

    private void toutesLesRencontres(List<String> poule, Map<String, Integer> forces) {
        for (int i = 0; i < poule.size(); i++) {
            for (int j = i + 1; j < poule.size(); j++) {
                poule(poule.get(i), 100 + forces.get(poule.get(i)), poule.get(j), 100 + forces.get(poule.get(j)));
            }
        }
    }

    @Test
    void poules_finies_premiers_qualifies_et_meilleur_deuxieme_repeche() {
        // Le deuxième de la poule B a le meilleur écart des trois deuxièmes.
        toutesLesRencontres(List.of("DET", "TOR", "ORL", "MIL", "BKN"), Map.of("DET", 5, "TOR", 4, "ORL", 3, "MIL", 2, "BKN", 1));
        toutesLesRencontres(List.of("NYK", "CLE", "PHI", "MIA", "IND"), Map.of("NYK", 50, "CLE", 40, "PHI", 3, "MIA", 2, "IND", 1));
        toutesLesRencontres(List.of("BOS", "ATL", "CHA", "CHI", "WAS"), Map.of("BOS", 5, "ATL", 4, "CHA", 3, "CHI", 2, "WAS", 1));

        CupStandingsService.Standings s = standings(NOW);

        assertThat(group(s, "Est", "B").teams().get(0).status()).isEqualTo("qualified");
        assertThat(group(s, "Est", "B").teams().get(1).status()).isEqualTo("wildcard");
        assertThat(group(s, "Est", "A").teams().get(0).status()).isEqualTo("qualified");
        assertThat(group(s, "Est", "A").teams().get(1).status()).isEqualTo("eliminated");
        assertThat(group(s, "Est", "C").teams().get(4).status()).isEqualTo("eliminated");
        // L'Ouest n'a pas joué : rien de marqué chez lui.
        assertThat(group(s, "Ouest", "A").teams().get(0).status()).isNull();
    }

    @Test
    void sans_poules_connues_pour_la_saison_rien_a_afficher() {
        assertThat(standings(Instant.parse("2027-11-07T12:00:00Z")).groups()).isEmpty();
    }
}
