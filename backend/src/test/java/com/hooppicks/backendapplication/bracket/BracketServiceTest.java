package com.hooppicks.backendapplication.bracket;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.push.PushService;
import com.hooppicks.backendapplication.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BracketServiceTest {

    private static final Instant NOW = Instant.parse("2026-12-01T12:00:00Z");
    private static final Instant LOCK = Instant.parse("2026-12-05T00:00:00Z");
    private static final List<String> TEAMS = List.of("NYK", "BOS", "CLE", "MIL", "OKC", "SAS", "LAL", "DEN");
    // Quarts : NYK, CLE, OKC, LAL ; demies : NYK, OKC ; finale : OKC.
    private static final List<String> PICKS = List.of("NYK", "CLE", "OKC", "LAL", "NYK", "OKC", "OKC");

    private final BracketRepository brackets = mock(BracketRepository.class);
    private final BracketPickRepository picks = mock(BracketPickRepository.class);
    private final TeamRepository teams = mock(TeamRepository.class);
    private final MatchRepository matches = mock(MatchRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final PhaseTitleRepository titles = mock(PhaseTitleRepository.class);
    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final PushService push = mock(PushService.class);

    private BracketService at(Instant now) {
        return new BracketService(brackets, picks, teams, matches, users, titles, notifications, push, Clock.fixed(now, BankrollService.ZONE));
    }

    private final Bracket bracket = new Bracket();

    private static User user(String id) {
        User u = new User();
        u.setId(id);
        u.setUsername(id);
        return u;
    }

    private static Team team(String abbreviation) {
        Team t = new Team();
        t.setAbbreviation(abbreviation);
        return t;
    }

    private static BracketPick pick(String userId, List<String> values) {
        BracketPick p = new BracketPick();
        p.setId(userId + ":cup-2026");
        p.setUserId(userId);
        p.setBracketId("cup-2026");
        p.setPicks(String.join(",", values));
        return p;
    }

    private static Match game(Team home, Team away, Instant date, int homeScore, int awayScore, MatchType type) {
        Match m = new Match();
        m.setHomeTeam(home);
        m.setAwayTeam(away);
        m.setDate(date);
        m.setHomeScore(homeScore);
        m.setAwayScore(awayScore);
        m.setType(type);
        return m;
    }

    private static String nones() {
        return ",,,,,,";
    }

    @BeforeEach
    void setUp() {
        bracket.setId("cup-2026");
        bracket.setLabel("Coupe NBA 2026");
        bracket.setTeams(String.join(",", TEAMS));
        bracket.setWinners(nones());
        bracket.setLocksAt(LOCK);
        when(brackets.findTopByOrderByLocksAtDesc()).thenReturn(Optional.of(bracket));
        when(picks.findById(any())).thenReturn(Optional.empty());
        when(picks.findByBracketId(any())).thenReturn(List.of());
        when(teams.findAll()).thenReturn(TEAMS.stream().map(BracketServiceTest::team).toList());
        when(users.findAll()).thenReturn(List.of(user("a"), user("b")));
        when(matches.findByDateBetween(any(), any())).thenReturn(List.of());
        when(matches.findFinishedMeetings(any(), any())).thenReturn(List.of());
    }

    // --- Forme du tableau ---

    @Test
    void chaque_tour_vaut_le_double_du_precedent() {
        assertThat(BracketTree.points(8, 0)).isEqualTo(10);
        assertThat(BracketTree.points(8, 3)).isEqualTo(10);
        assertThat(BracketTree.points(8, 4)).isEqualTo(20);
        assertThat(BracketTree.points(8, 6)).isEqualTo(40);
    }

    @Test
    void la_meme_forme_marche_pour_les_16_equipes_des_playoffs() {
        assertThat(BracketTree.games(16)).isEqualTo(15);
        assertThat(BracketTree.round(16, 7)).isZero();
        assertThat(BracketTree.round(16, 8)).isEqualTo(1);
        assertThat(BracketTree.round(16, 12)).isEqualTo(2);
        assertThat(BracketTree.round(16, 14)).isEqualTo(3);
    }

    @Test
    void les_demies_opposent_les_vainqueurs_des_quarts() {
        assertThat(BracketTree.participants(TEAMS, PICKS, 1)).containsExactly("CLE", "MIL");
        assertThat(BracketTree.participants(TEAMS, PICKS, 4)).containsExactly("NYK", "CLE");
        assertThat(BracketTree.participants(TEAMS, PICKS, 5)).containsExactly("OKC", "LAL");
        assertThat(BracketTree.participants(TEAMS, PICKS, 6)).containsExactly("NYK", "OKC");
    }

    @Test
    void tableau_incoherent_refuse() {
        assertThat(BracketTree.isValid(TEAMS, PICKS)).isTrue();
        // BOS en demie alors qu'on a fait gagner NYK en quart.
        assertThat(BracketTree.isValid(TEAMS, List.of("NYK", "CLE", "OKC", "LAL", "BOS", "OKC", "OKC"))).isFalse();
        // Finale avec une équipe éliminée en demie.
        assertThat(BracketTree.isValid(TEAMS, List.of("NYK", "CLE", "OKC", "LAL", "NYK", "OKC", "LAL"))).isFalse();
        assertThat(BracketTree.isValid(TEAMS, PICKS.subList(0, 6))).isFalse();
        assertThat(BracketTree.isValid(TEAMS, Arrays.asList("NYK", "CLE", "OKC", "LAL", "NYK", "OKC", null))).isFalse();
    }

    @Test
    void corriger_un_vainqueur_efface_ce_qui_en_dependait() {
        List<String> updated = BracketTree.withWinner(TEAMS, PICKS, 0, "BOS");
        assertThat(updated.get(0)).isEqualTo("BOS");
        assertThat(updated.get(4)).isNull();
        assertThat(updated.get(5)).isEqualTo("OKC");
        assertThat(updated.get(6)).isNull();
    }

    @Test
    void le_score_additionne_les_bons_choix_selon_leur_tour() {
        List<String> winners = new ArrayList<>(Arrays.asList("NYK", "MIL", "OKC", "DEN", "NYK", "OKC", "OKC"));
        // 2 quarts sur 4 (20) + 2 demies (40) + finale (40).
        assertThat(BracketTree.score(TEAMS, winners, PICKS)).isEqualTo(100);
    }

    // --- Remplir ---

    @Test
    void remplir_son_tableau_avant_le_verrou() {
        at(NOW).savePicks("a", PICKS);

        ArgumentCaptor<BracketPick> saved = ArgumentCaptor.forClass(BracketPick.class);
        verify(picks).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo("a:cup-2026");
        assertThat(saved.getValue().getPicks()).isEqualTo(String.join(",", PICKS));
    }

    @Test
    void plus_rien_apres_le_verrou() {
        assertThatThrownBy(() -> at(LOCK).savePicks("a", PICKS)).isInstanceOf(BracketService.BracketException.class);
        verify(picks, never()).save(any());
    }

    @Test
    void tableau_incoherent_refuse_par_le_serveur() {
        assertThatThrownBy(() -> at(NOW).savePicks("a", List.of("NYK"))).isInstanceOf(BracketService.BracketException.class);
        verify(picks, never()).save(any());
    }

    @Test
    void les_choix_des_autres_restent_caches_avant_le_verrou() {
        when(picks.findByBracketId("cup-2026")).thenReturn(List.of(pick("b", PICKS)));

        BracketService.Overview o = at(NOW).current("a").orElseThrow();

        assertThat(o.locked()).isFalse();
        assertThat(o.ranking()).isNull();
        assertThat(o.champions()).isNull();
        assertThat(o.players()).isEqualTo(1);
    }

    @Test
    void apres_le_verrou_classement_et_repartition_des_champions() {
        when(picks.findByBracketId("cup-2026")).thenReturn(List.of(pick("a", PICKS), pick("b", PICKS)));
        when(users.findAllById(any())).thenReturn(List.of(user("a"), user("b")));
        bracket.setWinners("NYK,,,,,,");

        BracketService.Overview o = at(LOCK).current("a").orElseThrow();

        assertThat(o.locked()).isTrue();
        assertThat(o.ranking()).hasSize(2);
        assertThat(o.ranking()).allSatisfy(r -> assertThat(r.rank()).isEqualTo(1)); // Ex æquo.
        assertThat(o.ranking().get(0).points()).isEqualTo(10);
        assertThat(o.ranking().get(0).champion()).isEqualTo("OKC");
        assertThat(o.champions()).containsExactly(new BracketService.ChampionShare("OKC", 2));
    }

    // --- Console ---

    @Test
    void publier_la_premiere_fois_previent_tout_le_monde() {
        when(brackets.findById("cup-2026")).thenReturn(Optional.empty());

        at(NOW).publish("cup-2026", "Coupe NBA 2026", TEAMS, LOCK);

        verify(brackets).save(any());
        verify(notifications, times(2)).save(any());
    }

    @Test
    void corriger_l_heure_ne_previent_personne_une_seconde_fois() {
        when(brackets.findById("cup-2026")).thenReturn(Optional.of(bracket));

        at(NOW).publish("cup-2026", "Coupe NBA 2026", TEAMS, LOCK.plusSeconds(3600));

        assertThat(bracket.getLocksAt()).isEqualTo(LOCK.plusSeconds(3600));
        verify(notifications, never()).save(any());
    }

    @Test
    void les_equipes_ne_changent_plus_une_fois_des_tableaux_remplis() {
        when(brackets.findById("cup-2026")).thenReturn(Optional.of(bracket));
        when(picks.countByBracketId("cup-2026")).thenReturn(1L);
        List<String> other = List.of("BOS", "NYK", "CLE", "MIL", "OKC", "SAS", "LAL", "DEN");

        assertThatThrownBy(() -> at(NOW).publish("cup-2026", "Coupe NBA 2026", other, LOCK)).isInstanceOf(BracketService.BracketException.class);
    }

    @Test
    void equipes_invalides_refusees() {
        assertThatThrownBy(() -> at(NOW).publish("x", "X", TEAMS.subList(0, 7), LOCK)).isInstanceOf(BracketService.BracketException.class);
        assertThatThrownBy(() -> at(NOW).publish("x", "X", List.of("NYK", "NYK", "CLE", "MIL", "OKC", "SAS", "LAL", "DEN"), LOCK)).isInstanceOf(BracketService.BracketException.class);
        assertThatThrownBy(() -> at(NOW).publish("x", "X", List.of("XXX", "BOS", "CLE", "MIL", "OKC", "SAS", "LAL", "DEN"), LOCK)).isInstanceOf(BracketService.BracketException.class);
    }

    @Test
    void un_admin_ne_saisit_qu_une_equipe_du_match() {
        assertThatThrownBy(() -> at(LOCK).setWinner(0, "CLE")).isInstanceOf(BracketService.BracketException.class);

        at(LOCK).setWinner(0, "NYK");

        assertThat(bracket.getWinners()).isEqualTo("NYK,,,,,,");
    }

    // --- Chaque minute ---

    @Test
    void le_premier_vrai_match_avance_le_verrou() {
        Instant earlier = LOCK.minus(Duration.ofHours(5));
        when(matches.findByDateBetween(any(), any())).thenReturn(List.of(game(team("BOS"), team("NYK"), earlier, 0, 0, MatchType.NBA_CUP)));

        at(NOW).tick();

        assertThat(bracket.getLocksAt()).isEqualTo(earlier);
    }

    @Test
    void un_match_de_saison_entre_les_memes_equipes_ne_verrouille_rien() {
        when(matches.findByDateBetween(any(), any())).thenReturn(List.of(game(team("BOS"), team("NYK"), LOCK.minus(Duration.ofHours(5)), 0, 0, MatchType.REGULAR)));

        at(NOW).tick();

        assertThat(bracket.getLocksAt()).isEqualTo(LOCK);
    }

    @Test
    void rappel_24_h_avant_seulement_a_ceux_qui_n_ont_pas_rempli_et_une_fois() {
        when(picks.findByBracketId("cup-2026")).thenReturn(List.of(pick("a", PICKS)));
        Instant dayBefore = LOCK.minus(Duration.ofHours(20));

        at(NOW).tick();
        verify(notifications, never()).save(any());

        at(dayBefore).tick();
        ArgumentCaptor<AppNotification> sent = ArgumentCaptor.forClass(AppNotification.class);
        verify(notifications).save(sent.capture());
        assertThat(sent.getValue().getUser().getId()).isEqualTo("b");

        clearInvocations(notifications);
        at(dayBefore).tick();
        verify(notifications, never()).save(any());
    }

    @Test
    void les_vainqueurs_se_lisent_dans_les_matchs_de_coupe_apres_le_verrou() {
        Instant played = LOCK.plus(Duration.ofHours(3));
        when(matches.findFinishedMeetings(any(), any())).thenAnswer(i -> {
            Team a = i.getArgument(0);
            Team b = i.getArgument(1);
            if (a.getAbbreviation().equals("NYK") && b.getAbbreviation().equals("BOS")) {
                return List.of(game(team("BOS"), team("NYK"), played, 99, 101, MatchType.NBA_CUP));
            }
            return List.of();
        });

        at(played.plusSeconds(60)).tick();

        assertThat(bracket.getWinners()).isEqualTo("NYK,,,,,,");
    }

    @Test
    void un_match_de_poule_entre_les_memes_equipes_ne_compte_pas() {
        when(matches.findFinishedMeetings(any(), any())).thenAnswer(i -> {
            Team a = i.getArgument(0);
            return a.getAbbreviation().equals("NYK")
                    ? List.of(game(team("BOS"), team("NYK"), LOCK.minus(Duration.ofDays(20)), 120, 90, MatchType.NBA_CUP))
                    : List.of();
        });

        at(LOCK.plusSeconds(60)).tick();

        assertThat(bracket.getWinners()).isEqualTo(nones());
    }

    @Test
    void finale_connue_le_meilleur_tableau_gagne_un_titre_une_seule_fois() {
        bracket.setWinners("NYK,CLE,OKC,LAL,NYK,OKC,OKC");
        when(picks.findByBracketId("cup-2026")).thenReturn(List.of(pick("a", PICKS), pick("b", List.of("BOS", "CLE", "OKC", "LAL", "BOS", "OKC", "BOS"))));
        when(users.findById("a")).thenReturn(Optional.of(user("a")));

        at(LOCK.plus(Duration.ofDays(7))).tick();

        ArgumentCaptor<PhaseTitle> saved = ArgumentCaptor.forClass(PhaseTitle.class);
        verify(titles).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo("a");
        assertThat(saved.getValue().getPhase()).isEqualTo("BRACKET");
        assertThat(saved.getValue().getSeason()).isEqualTo("Coupe NBA 2026");
        assertThat(saved.getValue().getPoints()).isEqualTo(120);

        clearInvocations(titles);
        at(LOCK.plus(Duration.ofDays(7))).tick();
        verify(titles, never()).save(any());
    }

    @Test
    void ex_aequo_tous_ont_le_titre() {
        bracket.setWinners("NYK,CLE,OKC,LAL,NYK,OKC,OKC");
        when(picks.findByBracketId("cup-2026")).thenReturn(List.of(pick("a", PICKS), pick("b", PICKS)));

        at(LOCK.plus(Duration.ofDays(7))).tick();

        verify(titles, times(2)).save(any());
    }
}
