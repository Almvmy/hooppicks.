package com.hooppicks.backendapplication.espn;

import com.hooppicks.backendapplication.bet.BetResolutionService;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.entity.MatchType;
import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.nba.NbaSyncService;
import com.hooppicks.backendapplication.nba.OddsService;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EspnScheduleServiceTest {

    @Mock private EspnStatsClient espnStatsClient;
    @Mock private MatchRepository matchRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private BetRepository betRepository;
    @Mock private OddsService oddsService;
    @Mock private NbaSyncService nbaSyncService;
    @Mock private BetResolutionService betResolutionService;
    @Mock private PlatformTransactionManager transactionManager;

    private EspnScheduleService service;

    private final Team knicks = team("1", "NYK");
    private final Team sixers = team("2", "PHI");
    private static final Instant TIP_OFF = Instant.parse("2026-10-05T23:00:00Z");

    @BeforeEach
    void setUp() {
        service = new EspnScheduleService(espnStatsClient, matchRepository, teamRepository, betRepository,
                oddsService, nbaSyncService, betResolutionService, transactionManager);
    }

    private static Team team(String id, String abbreviation) {
        Team t = new Team();
        t.setId(id);
        t.setAbbreviation(abbreviation);
        return t;
    }

    private static EspnGameRow row(String home, String away, int seasonType, String state, boolean completed,
                                   Integer homeScore, Integer awayScore) {
        return new EspnGameRow("401914101", TIP_OFF, home, away, homeScore, awayScore, state, completed,
                seasonType, "STD", "", null);
    }

    private void givenTeams() {
        when(teamRepository.findAll()).thenReturn(List.of(knicks, sixers));
    }

    private void givenNoExistingMatch() {
        when(matchRepository.findFirstByEspnEventId(any())).thenReturn(Optional.empty());
        when(matchRepository.findFirstByHomeTeamAndAwayTeamAndDateBetween(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
    }

    @Test
    void cree_un_match_de_presaison_absent_de_balldontlie_avec_ses_cotes() {
        givenTeams();
        givenNoExistingMatch();

        int updated = service.apply(List.of(row("PHI", "NYK", 1, "pre", false, 0, 0)));

        ArgumentCaptor<Match> saved = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository).save(saved.capture());
        Match match = saved.getValue();
        assertThat(updated).isEqualTo(1);
        assertThat(match.getExternalId()).isNull();
        assertThat(match.getEspnEventId()).isEqualTo("401914101");
        assertThat(match.getType()).isEqualTo(MatchType.PRESEASON);
        assertThat(match.getStatus()).isEqualTo(MatchStatus.SCHEDULED);
        assertThat(match.getDate()).isEqualTo(TIP_OFF);
        // Les "0-0" d'ESPN avant le coup d'envoi ne sont pas un score.
        assertThat(match.getHomeScore()).isNull();
        verify(oddsService).applyOdds(match, sixers, knicks);
    }

    @Test
    void ne_cree_jamais_un_match_de_saison_reguliere_laisse_a_balldontlie() {
        givenTeams();
        givenNoExistingMatch();

        int updated = service.apply(List.of(row("PHI", "NYK", 2, "pre", false, 0, 0)));

        assertThat(updated).isZero();
        verify(matchRepository, never()).save(any());
    }

    @Test
    void ignore_les_clubs_hors_nba() {
        givenTeams();

        int updated = service.apply(List.of(row("BKN", "HAPOEL", 1, "pre", false, 0, 0)));

        assertThat(updated).isZero();
        verify(matchRepository, never()).save(any());
    }

    @Test
    void match_balldontlie_relie_et_classe_mais_statut_et_score_jamais_touches() {
        givenTeams();
        Match existing = new Match();
        existing.setId("m1");
        existing.setExternalId(42L);
        existing.setHomeTeam(sixers);
        existing.setAwayTeam(knicks);
        existing.setStatus(MatchStatus.LIVE);
        existing.setHomeScore(50);
        when(matchRepository.findFirstByEspnEventId(any())).thenReturn(Optional.empty());
        when(matchRepository.findFirstByHomeTeamAndAwayTeamAndDateBetween(any(), any(), any(), any()))
                .thenReturn(Optional.of(existing));

        // ESPN dit "terminé 120-100" : c'est balldontlie qui fait foi pour ce match.
        service.apply(List.of(new EspnGameRow("401909089", TIP_OFF, "PHI", "NYK", 120, 100, "post", true,
                3, "RD16", "East 1st Round - Game 2", "NY leads series 2-0")));

        assertThat(existing.getEspnEventId()).isEqualTo("401909089");
        assertThat(existing.getType()).isEqualTo(MatchType.PLAYOFFS);
        assertThat(existing.getStageLabel()).isEqualTo("1er tour Est · Match 2");
        assertThat(existing.getSeriesSummary()).isEqualTo("NYK mène 2-0");
        assertThat(existing.getStatus()).isEqualTo(MatchStatus.LIVE);
        assertThat(existing.getHomeScore()).isEqualTo(50);
        verifyNoInteractions(oddsService, betResolutionService);
    }

    @Test
    void match_de_presaison_termine_met_le_score_et_resout_les_paris_sans_elo() {
        givenTeams();
        Match existing = new Match();
        existing.setId("m2");
        existing.setEspnEventId("401914101");
        existing.setHomeTeam(sixers);
        existing.setAwayTeam(knicks);
        existing.setStatus(MatchStatus.LIVE);
        when(matchRepository.findFirstByEspnEventId("401914101")).thenReturn(Optional.of(existing));

        service.apply(List.of(row("PHI", "NYK", 1, "post", true, 98, 105)));

        assertThat(existing.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(existing.getHomeScore()).isEqualTo(98);
        assertThat(existing.getAwayScore()).isEqualTo(105);
        verify(betResolutionService).resolvePendingBets();
        verifyNoInteractions(oddsService);
    }

    @Test
    void presaison_qui_demarre_previent_les_parieurs() {
        givenTeams();
        Match existing = new Match();
        existing.setId("m3");
        existing.setEspnEventId("401914101");
        existing.setHomeTeam(sixers);
        existing.setAwayTeam(knicks);
        existing.setStatus(MatchStatus.SCHEDULED);
        when(matchRepository.findFirstByEspnEventId("401914101")).thenReturn(Optional.of(existing));

        service.apply(List.of(row("PHI", "NYK", 1, "in", false, 10, 8)));

        assertThat(existing.getStatus()).isEqualTo(MatchStatus.LIVE);
        verify(nbaSyncService).notifyMatchStarting(existing);
    }

    @Test
    void espn_injoignable_ne_touche_a_rien() {
        when(espnStatsClient.fetchScoreboard(any())).thenReturn(Optional.empty());

        int updated = service.syncWindow(List.of(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 6)));

        assertThat(updated).isZero();
        // Arrêt dès le premier jour injoignable, sans prendre le verrou de synchro.
        verify(espnStatsClient, times(1)).fetchScoreboard(any());
        verifyNoInteractions(nbaSyncService, matchRepository);
    }

    @Test
    void match_de_presaison_verrouille_par_un_admin_jamais_ecrase() {
        givenTeams();
        Match locked = new Match();
        locked.setId("m4");
        locked.setEspnEventId("401914101");
        locked.setHomeTeam(sixers);
        locked.setAwayTeam(knicks);
        locked.setStatus(MatchStatus.FINISHED);
        locked.setHomeScore(100);
        locked.setAwayScore(90);
        locked.setAdminLocked(true);
        when(matchRepository.findFirstByEspnEventId("401914101")).thenReturn(Optional.of(locked));

        service.apply(List.of(row("PHI", "NYK", 1, "post", true, 98, 105)));

        assertThat(locked.getHomeScore()).isEqualTo(100);
        assertThat(locked.getAwayScore()).isEqualTo(90);
        verifyNoInteractions(betResolutionService, oddsService);
    }
}
