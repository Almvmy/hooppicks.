package com.hooppicks.backendapplication.palmares;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.leaderboard.LeaderboardPeriod;
import com.hooppicks.backendapplication.push.PushService;
import com.hooppicks.backendapplication.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PhaseChampionServiceTest {

    // Mercredi 16 décembre 2026, 20h GMT : finale de la Coupe jouée la veille.
    private static final Instant NOW = Instant.parse("2026-12-16T20:00:00Z");
    private static final Instant SEASON_START = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant FINAL = Instant.parse("2026-12-16T01:30:00Z");

    private final BetRepository betRepository = mock(BetRepository.class);
    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final PhaseTitleRepository titleRepository = mock(PhaseTitleRepository.class);
    private final PalmaresPhaseRepository phaseRepository = mock(PalmaresPhaseRepository.class);
    private final LeagueRepository leagueRepository = mock(LeagueRepository.class);
    private final LeagueMembershipRepository membershipRepository = mock(LeagueMembershipRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);

    private PhaseChampionService service(Instant now) {
        return new PhaseChampionService(betRepository, matchRepository, titleRepository, phaseRepository,
                leagueRepository, membershipRepository, userRepository, notificationRepository,
                mock(PushService.class), Clock.fixed(now, BankrollService.ZONE));
    }

    private PhaseChampionService service;

    @BeforeEach
    void setUp() {
        service = service(NOW);
        when(userRepository.findById(any())).thenAnswer(inv -> {
            User u = new User();
            u.setId(inv.getArgument(0));
            return Optional.of(u);
        });
        when(matchRepository.findByTypeAndStatusAndDateAfter(any(), any(), any())).thenReturn(List.of());
    }

    private static Match match(MatchType type, String label, String series, Instant date) {
        Match m = new Match();
        m.setType(type);
        m.setStatus(MatchStatus.FINISHED);
        m.setStageLabel(label);
        m.setSeriesSummary(series);
        m.setDate(date);
        return m;
    }

    private static Object[] row(String user, long points) {
        return new Object[]{user, "pseudo-" + user, points, 3L, 2L, 7, "PG", "orange", "dunk", null};
    }

    private void cupFinalPlayed() {
        when(matchRepository.findByTypeAndStatusAndDateAfter(MatchType.NBA_CUP, MatchStatus.FINISHED, SEASON_START))
                .thenReturn(List.of(match(MatchType.NBA_CUP, "Coupe NBA · Demi-finales", null, FINAL.minusSeconds(4 * 86400)),
                        match(MatchType.NBA_CUP, "Coupe NBA · Finale", null, FINAL)));
    }

    @Test
    void phases_ouvertes_une_fois_leur_premier_match_commence() {
        when(matchRepository.existsByTypeInAndDateBetween(List.of(MatchType.NBA_CUP), SEASON_START, NOW)).thenReturn(true);

        PhaseChampionService.OpenPhases open = service.openPhases();

        assertThat(open.cup()).isTrue();
        assertThat(open.playoffs()).isFalse();
    }

    @Test
    void rien_tant_que_la_finale_n_est_pas_jouee() {
        when(matchRepository.findByTypeAndStatusAndDateAfter(MatchType.NBA_CUP, MatchStatus.FINISHED, SEASON_START))
                .thenReturn(List.of(match(MatchType.NBA_CUP, "Coupe NBA · Demi-finales", null, FINAL)));

        assertThat(service.recordEndedPhases()).isZero();
        verify(titleRepository, never()).save(any());
    }

    @Test
    void attend_les_tickets_de_coupe_en_attente_24h_au_plus() {
        cupFinalPlayed();
        when(betRepository.countPendingInPhaseSince(SEASON_START, "CUP")).thenReturn(1L);

        assertThat(service.recordEndedPhases()).isZero();

        when(betRepository.getLeaderboardRawSince(SEASON_START, "CUP")).thenReturn(List.<Object[]>of(row("a", 300)));
        assertThat(service(FINAL.plus(Duration.ofHours(25))).recordEndedPhases()).isEqualTo(1);
    }

    @Test
    void champion_de_la_coupe_ex_aequo_et_dans_les_ligues() {
        cupFinalPlayed();
        when(betRepository.getLeaderboardRawSince(SEASON_START, "CUP"))
                .thenReturn(List.of(row("a", 400), row("b", 400), row("c", 120)));
        League league = new League();
        league.setId("l1");
        league.setName("Les potes");
        when(leagueRepository.findAll()).thenReturn(List.of(league));
        when(membershipRepository.findByLeagueId("l1")).thenReturn(List.of(member("b"), member("c")));

        assertThat(service.recordEndedPhases()).isEqualTo(1);

        ArgumentCaptor<PhaseTitle> saved = ArgumentCaptor.forClass(PhaseTitle.class);
        verify(titleRepository, times(3)).save(saved.capture());
        assertThat(saved.getAllValues()).allMatch(t -> "2026-27".equals(t.getSeason()) && "CUP".equals(t.getPhase()));
        assertThat(saved.getAllValues()).filteredOn(t -> t.getLeagueId() == null)
                .extracting(PhaseTitle::getUserId).containsExactlyInAnyOrder("a", "b");
        assertThat(saved.getAllValues()).filteredOn(t -> "l1".equals(t.getLeagueId()))
                .extracting(PhaseTitle::getUserId).containsExactly("b");
        verify(notificationRepository, times(3)).save(any());

        ArgumentCaptor<PalmaresPhase> marker = ArgumentCaptor.forClass(PalmaresPhase.class);
        verify(phaseRepository).save(marker.capture());
        assertThat(marker.getValue().getId()).isEqualTo("2026-27:CUP");
    }

    @Test
    void jamais_deux_fois() {
        cupFinalPlayed();
        when(phaseRepository.existsById("2026-27:CUP")).thenReturn(true);

        assertThat(service.recordEndedPhases()).isZero();
        verify(betRepository, never()).getLeaderboardRawSince(any(), eq("CUP"));
    }

    @Test
    void phase_sans_point_positif_sans_champion_mais_close() {
        cupFinalPlayed();
        when(betRepository.getLeaderboardRawSince(SEASON_START, "CUP")).thenReturn(List.<Object[]>of(row("a", 0)));

        assertThat(service.recordEndedPhases()).isEqualTo(1);
        verify(titleRepository, never()).save(any());
        verify(phaseRepository).save(any());
    }

    @Test
    void playoffs_finis_quand_les_finales_sont_remportees() {
        Instant june = Instant.parse("2027-06-18T01:00:00Z");
        PhaseChampionService later = service(june.plus(Duration.ofDays(2)));
        when(matchRepository.findByTypeAndStatusAndDateAfter(MatchType.PLAYOFFS, MatchStatus.FINISHED, SEASON_START))
                .thenReturn(List.of(match(MatchType.PLAYOFFS, "Finales NBA · Match 5", "OKC mène 3-2", june)));
        when(betRepository.getLeaderboardRawSince(SEASON_START, "PLAYOFFS")).thenReturn(List.<Object[]>of(row("a", 250)));

        assertThat(later.recordEndedPhases()).isZero();

        when(matchRepository.findByTypeAndStatusAndDateAfter(MatchType.PLAYOFFS, MatchStatus.FINISHED, SEASON_START))
                .thenReturn(List.of(match(MatchType.PLAYOFFS, "Finales NBA · Match 6", "OKC remporte la série 4-2", june)));

        // Playoffs et classement général, clos ensemble.
        assertThat(later.recordEndedPhases()).isEqualTo(2);
        ArgumentCaptor<PhaseTitle> saved = ArgumentCaptor.forClass(PhaseTitle.class);
        verify(titleRepository).save(saved.capture());
        assertThat(saved.getValue().getPhase()).isEqualTo("PLAYOFFS");
        assertThat(saved.getValue().getSeason()).isEqualTo("2026-27");
    }

    @Test
    void champions_de_la_phase_de_la_saison() {
        PhaseTitle t = new PhaseTitle();
        t.setUserId("a");
        t.setPoints(400);
        when(titleRepository.findBySeasonAndPhaseAndLeagueIdIsNull("2026-27", "CUP")).thenReturn(List.of(t));
        User a = new User();
        a.setId("a");
        a.setUsername("alpha");
        when(userRepository.findAllById(any())).thenReturn(List.of(a));

        PhaseChampionService.PhaseChampions champions = service.champions(LeaderboardPeriod.CUP, null);

        assertThat(champions.phase()).isEqualTo("cup");
        assertThat(champions.champions()).extracting(PalmaresService.Champion::username).containsExactly("alpha");
        assertThat(champions.champions().get(0).points()).isEqualTo(400);
    }

    @Test
    void champion_hooppicks_de_la_saison_avec_les_playoffs() {
        Instant june = Instant.parse("2027-06-18T01:00:00Z");
        when(matchRepository.findByTypeAndStatusAndDateAfter(MatchType.PLAYOFFS, MatchStatus.FINISHED, SEASON_START))
                .thenReturn(List.of(match(MatchType.PLAYOFFS, "Finales NBA · Match 6", "OKC remporte la série 4-2", june)));
        when(betRepository.getLeaderboardRawSince(SEASON_START, "GENERAL")).thenReturn(List.of(row("a", 5200), row("b", 4100)));

        service(june.plus(Duration.ofDays(2))).recordEndedPhases();

        ArgumentCaptor<PhaseTitle> saved = ArgumentCaptor.forClass(PhaseTitle.class);
        verify(titleRepository).save(saved.capture());
        assertThat(saved.getValue().getPhase()).isEqualTo("GENERAL");
        assertThat(saved.getValue().getUserId()).isEqualTo("a");
        assertThat(PhaseChampionService.title("GENERAL", "2026-27")).isEqualTo("Champion HoopPicks 2026-27");
    }

    @Test
    void champion_du_mois_une_fois_ses_semaines_finies() {
        // Lundi 2 novembre 2026 à 14h GMT : les semaines d'octobre (lundis 12, 19, 26) sont finies.
        Instant monday = Instant.parse("2026-11-02T14:00:00Z");
        LocalDateTime from = LocalDateTime.parse("2026-10-12T00:00"); // semaines d'essai exclues
        LocalDateTime to = LocalDateTime.parse("2026-11-02T00:00");
        when(betRepository.getWeeklyScoreLeaderboardBetween(from, to, "ALL")).thenReturn(List.of(row("a", 900), row("b", 300)));

        assertThat(service(monday).recordEndedMonths()).isEqualTo(1);

        ArgumentCaptor<PhaseTitle> saved = ArgumentCaptor.forClass(PhaseTitle.class);
        verify(titleRepository).save(saved.capture());
        assertThat(saved.getValue().getSeason()).isEqualTo("2026-10");
        assertThat(saved.getValue().getPhase()).isEqualTo("MONTH");
        assertThat(saved.getValue().getUserId()).isEqualTo("a");
        verify(notificationRepository).save(any());
        ArgumentCaptor<PalmaresPhase> marker = ArgumentCaptor.forClass(PalmaresPhase.class);
        verify(phaseRepository).save(marker.capture());
        assertThat(marker.getValue().getId()).isEqualTo("2026-10:MONTH");
    }

    @Test
    void mois_pas_fini_tant_que_sa_derniere_semaine_court() {
        // Samedi 31 octobre : la semaine du lundi 26 octobre n'est pas finie.
        assertThat(service(Instant.parse("2026-10-31T20:00:00Z")).recordEndedMonths()).isZero();
        // Dimanche 1er novembre : toujours la semaine du 26 octobre.
        assertThat(service(Instant.parse("2026-11-01T20:00:00Z")).recordEndedMonths()).isZero();
        verify(phaseRepository, never()).save(any());
    }

    @Test
    void champions_du_mois_qui_vient_de_finir() {
        when(titleRepository.findBySeasonAndPhaseAndLeagueIdIsNull("2026-11", "MONTH")).thenReturn(List.of());

        PhaseChampionService.PhaseChampions champions =
                service(Instant.parse("2026-12-16T20:00:00Z")).champions(LeaderboardPeriod.MONTH, null);

        assertThat(champions.season()).isEqualTo("2026-11");
        assertThat(champions.phase()).isEqualTo("month");
    }

    private static LeagueMembership member(String userId) {
        User u = new User();
        u.setId(userId);
        LeagueMembership m = new LeagueMembership();
        m.setUser(u);
        return m;
    }
}
