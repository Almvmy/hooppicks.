package com.hooppicks.backendapplication.favorite;

import com.hooppicks.backendapplication.badge.BadgeService;
import com.hooppicks.backendapplication.dto.BadgeDto;
import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.entity.BetSelection;
import com.hooppicks.backendapplication.entity.BetStatus;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.favorite.FavoriteTeamService.Side;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FavoriteTeamServiceTest {

    @Mock private TeamRepository teamRepository;
    @Mock private MatchRepository matchRepository;

    private FavoriteTeamService service;

    private final Team knicks = team("NYK", "Knicks");
    private final Team sixers = team("PHI", "76ers");
    private final Team nets = team("BKN", "Nets");
    private final Team hornets = team("CHA", "Hornets");

    @BeforeEach
    void setUp() {
        service = new FavoriteTeamService(teamRepository, matchRepository);
        when(teamRepository.findAll()).thenReturn(List.of(knicks, sixers, nets, hornets));
    }

    private static Team team(String abbr, String name) {
        Team t = new Team();
        t.setId(abbr);
        t.setAbbreviation(abbr);
        t.setName(name);
        return t;
    }

    private static Match match(String id, Team home, Team away, Integer homeScore, Integer awayScore) {
        Match m = new Match();
        m.setId(id);
        m.setHomeTeam(home);
        m.setAwayTeam(away);
        m.setHomeScore(homeScore);
        m.setAwayScore(awayScore);
        m.setStatus(homeScore == null ? MatchStatus.SCHEDULED : MatchStatus.FINISHED);
        m.setSpreadValue(-4.5);
        m.setTotalValue(220.5);
        return m;
    }

    private static BetSelection selection(String matchId, String market, String outcome) {
        BetSelection s = new BetSelection();
        s.setMatchId(matchId);
        s.setMarket(market);
        s.setOutcome(outcome);
        s.setOdds(1.9);
        return s;
    }

    private static Bet bet(BetStatus status, BetSelection... selections) {
        Bet b = new Bet();
        b.setStatus(status);
        b.setSelections(new ArrayList<>(List.of(selections)));
        return b;
    }

    @Test
    void hornets_ne_se_confond_pas_avec_nets() {
        assertThat(service.resolve("Charlotte Hornets")).contains(hornets);
        assertThat(service.resolve("Brooklyn Nets")).contains(nets);
        assertThat(service.resolve("")).isEmpty();
        assertThat(service.resolve("Équipe inventée")).isEmpty();
    }

    @Test
    void pour_contre_ou_neutre_selon_le_cote_choisi() {
        Match m = match("m1", knicks, sixers, null, null);

        assertThat(FavoriteTeamService.side(selection("m1", "moneyline", "home"), m, "NYK")).isEqualTo(Side.FOR);
        assertThat(FavoriteTeamService.side(selection("m1", "spread", "away"), m, "NYK")).isEqualTo(Side.AGAINST);
        // Les Knicks à l'extérieur : parier "away", c'est parier pour eux.
        Match away = match("m2", sixers, knicks, null, null);
        assertThat(FavoriteTeamService.side(selection("m2", "moneyline", "away"), away, "NYK")).isEqualTo(Side.FOR);
        // Un total ne prend parti pour personne, un match sans l'équipe non plus.
        assertThat(FavoriteTeamService.side(selection("m1", "total", "over"), m, "NYK")).isEqualTo(Side.NEUTRAL);
        assertThat(FavoriteTeamService.side(selection("m1", "moneyline", "home"), m, "BKN")).isEqualTo(Side.NEUTRAL);
    }

    @Test
    void stats_par_selection_avec_resultat_des_matchs_termines() {
        Match won = match("m1", knicks, sixers, 110, 100);      // Knicks gagnent
        Match lost = match("m2", sixers, knicks, 120, 100);     // Knicks perdent
        Match upcoming = match("m3", knicks, nets, null, null);
        when(matchRepository.findAllById(any())).thenReturn(List.of(won, lost, upcoming));

        List<Bet> bets = List.of(
                bet(BetStatus.WON, selection("m1", "moneyline", "home")),          // pour, gagné
                bet(BetStatus.LOST, selection("m2", "moneyline", "away")),         // pour, perdu
                bet(BetStatus.WON, selection("m2", "moneyline", "home")),          // contre, gagné
                bet(BetStatus.PENDING, selection("m3", "moneyline", "home"),       // pour, en attente
                        selection("m3", "total", "over")));                        // neutre

        FavoriteTeamService.Stats stats = service.stats("New York Knicks", bets);

        assertThat(stats.teamAbbreviation()).isEqualTo("NYK");
        assertThat(stats.forLegs()).isEqualTo(3);
        assertThat(stats.forWon()).isEqualTo(1);
        assertThat(stats.forLost()).isEqualTo(1);
        assertThat(stats.againstLegs()).isEqualTo(1);
        assertThat(stats.againstWon()).isEqualTo(1);
        assertThat(stats.againstLost()).isZero();
    }

    @Test
    void sans_equipe_favorite_stats_vides() {
        FavoriteTeamService.Stats stats = service.stats("", List.of());
        assertThat(stats.teamAbbreviation()).isNull();
        assertThat(stats.forLegs()).isZero();
    }

    @Test
    void badges_fidele_et_lucide() {
        Match m = match("m1", knicks, sixers, 110, 100);
        when(matchRepository.findAllById(any())).thenReturn(List.of(m));
        List<Bet> bets = new ArrayList<>();
        for (int i = 0; i < 10; i++) bets.add(bet(BetStatus.LOST, selection("m1", "moneyline", "home")));

        FavoriteTeamService.BadgeFacts tenFor = service.badgeFacts("New York Knicks", bets);
        assertThat(tenFor.forSelections()).isEqualTo(10);
        assertThat(tenFor.wonTicketAgainst()).isFalse();

        bets.add(bet(BetStatus.WON, selection("m1", "moneyline", "away")));
        FavoriteTeamService.BadgeFacts withAgainstWin = service.badgeFacts("New York Knicks", bets);
        assertThat(withAgainstWin.wonTicketAgainst()).isTrue();

        List<BadgeDto> badges = new BadgeService().computeBadges(bets, withAgainstWin);
        assertThat(badges).filteredOn(b -> b.id().equals("loyal_fan")).extracting(BadgeDto::unlocked).containsExactly(true);
        assertThat(badges).filteredOn(b -> b.id().equals("clear_eyed")).extracting(BadgeDto::unlocked).containsExactly(true);
        // Sans équipe favorite, jamais débloqués.
        assertThat(new BadgeService().computeBadges(bets)).filteredOn(b -> b.id().startsWith("loyal") || b.id().startsWith("clear"))
                .extracting(BadgeDto::unlocked).containsOnly(false);
    }
}
