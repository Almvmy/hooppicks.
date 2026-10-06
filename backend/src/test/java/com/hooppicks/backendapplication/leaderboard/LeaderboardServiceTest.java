package com.hooppicks.backendapplication.leaderboard;

import com.hooppicks.backendapplication.dto.LeaderboardEntryDto;
import com.hooppicks.backendapplication.entity.RankSnapshot;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.RankSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock private BetRepository betRepository;
    @Mock private RankSnapshotRepository rankSnapshotRepository;

    private LeaderboardService service;

    // Mercredi 7 octobre 2026, 10h à Paris (8h UTC).
    private static final Instant NOW = Instant.parse("2026-10-07T08:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 7);

    @BeforeEach
    void setUp() {
        service = new LeaderboardService(betRepository, rankSnapshotRepository,
                Clock.fixed(NOW, LeaderboardService.ZONE));
    }

    private static Object[] row(String user, long points) {
        return new Object[]{user, "pseudo-" + user, points, 4L, 2L, 7, "PG", "orange", "dunk", null};
    }

    private static Object[] result(String user, String status) {
        return new Object[]{user, status};
    }

    private static RankSnapshot snapshot(String user, int rank) {
        RankSnapshot s = new RankSnapshot();
        s.setUserId(user);
        s.setRank(rank);
        return s;
    }

    @Test
    void semaine_commence_lundi_midi_heure_de_paris() {
        when(betRepository.getLeaderboardRawSince(any())).thenReturn(List.of());

        service.leaderboard(LeaderboardPeriod.WEEK);

        // Semaine de jeu : lundi 5 octobre, 12h00 à Paris = 10h00 UTC (heure d'été).
        verify(betRepository).getLeaderboardRawSince(Instant.parse("2026-10-05T10:00:00Z"));
        verify(betRepository, never()).getLeaderboardRaw();
    }

    @Test
    void mois_commence_le_premier_a_minuit_heure_de_paris() {
        when(betRepository.getLeaderboardRawSince(any())).thenReturn(List.of());

        service.leaderboard(LeaderboardPeriod.MONTH);

        verify(betRepository).getLeaderboardRawSince(Instant.parse("2026-09-30T22:00:00Z"));
    }

    @Test
    void evolution_du_rang_depuis_la_derniere_photo_d_un_jour_precedent() {
        when(betRepository.getLeaderboardRaw()).thenReturn(List.of(row("a", 900), row("b", 500), row("c", 100)));
        when(rankSnapshotRepository.findLatestDateBefore(TODAY)).thenReturn(TODAY.minusDays(1));
        // Hier : b 1er, a 3e ; c n'était pas encore classé.
        when(rankSnapshotRepository.findBySnapshotDate(TODAY.minusDays(1)))
                .thenReturn(List.of(snapshot("b", 1), snapshot("a", 3)));

        List<LeaderboardEntryDto> entries = service.leaderboard(LeaderboardPeriod.SEASON);

        assertThat(entries).extracting(LeaderboardEntryDto::rankChange).containsExactly(2, -1, null);
        assertThat(entries).extracting(LeaderboardEntryDto::newcomer).containsExactly(false, false, true);
    }

    @Test
    void sans_historique_aucune_evolution_ni_nouveau() {
        when(betRepository.getLeaderboardRaw()).thenReturn(List.<Object[]>of(row("a", 900)));
        when(rankSnapshotRepository.findLatestDateBefore(TODAY)).thenReturn(null);

        LeaderboardEntryDto entry = service.leaderboard(LeaderboardPeriod.SEASON).get(0);

        assertThat(entry.rankChange()).isNull();
        assertThat(entry.newcomer()).isFalse();
    }

    @Test
    void pas_d_evolution_sur_la_semaine_ni_le_mois() {
        when(betRepository.getLeaderboardRawSince(any())).thenReturn(List.<Object[]>of(row("a", 900)));

        LeaderboardEntryDto entry = service.leaderboard(LeaderboardPeriod.WEEK).get(0);

        assertThat(entry.rankChange()).isNull();
        verifyNoInteractions(rankSnapshotRepository);
    }

    @Test
    void forme_limitee_aux_5_derniers_et_serie_en_cours() {
        when(betRepository.getLeaderboardRaw()).thenReturn(List.of(row("a", 900), row("b", 500)));
        when(betRepository.getRecentResultsPerUser()).thenReturn(List.of(
                result("a", "WON"), result("a", "WON"), result("a", "WON"), result("a", "LOST"),
                result("a", "WON"), result("a", "LOST"), result("a", "LOST"),
                result("b", "LOST"), result("b", "LOST"), result("b", "WON")
        ));

        List<LeaderboardEntryDto> entries = service.leaderboard(LeaderboardPeriod.SEASON);

        assertThat(entries.get(0).recentForm()).containsExactly("W", "W", "W", "L", "W");
        assertThat(entries.get(0).streak()).isEqualTo(3);
        assertThat(entries.get(1).recentForm()).containsExactly("L", "L", "W");
        assertThat(entries.get(1).streak()).isEqualTo(-2);
    }

    @Test
    void joueur_sans_ticket_recent_forme_vide() {
        when(betRepository.getLeaderboardRaw()).thenReturn(List.<Object[]>of(row("a", 0)));

        LeaderboardEntryDto entry = service.leaderboard(LeaderboardPeriod.SEASON).get(0);

        assertThat(entry.recentForm()).isEmpty();
        assertThat(entry.streak()).isZero();
    }

    @Test
    @SuppressWarnings("unchecked")
    void photo_remplace_celle_du_jour_avec_les_rangs_a_egalite() {
        when(betRepository.getLeaderboardRaw()).thenReturn(List.of(row("a", 900), row("b", 500), row("c", 500)));

        service.takeSnapshot(TODAY);

        verify(rankSnapshotRepository).deleteBySnapshotDate(TODAY);
        ArgumentCaptor<List<RankSnapshot>> saved = ArgumentCaptor.forClass(List.class);
        verify(rankSnapshotRepository).saveAll(saved.capture());
        List<RankSnapshot> snapshots = new ArrayList<>(saved.getValue());
        snapshots.sort((x, y) -> x.getUserId().compareTo(y.getUserId()));
        assertThat(snapshots).extracting(RankSnapshot::getRank).containsExactly(1, 2, 2);
        assertThat(snapshots).allMatch(s -> TODAY.equals(s.getSnapshotDate()));
        verify(rankSnapshotRepository).deleteOlderThan(TODAY.minusDays(30));
    }

    @Test
    void periode_inconnue_refusee() {
        assertThat(LeaderboardPeriod.parse(null)).isEqualTo(LeaderboardPeriod.SEASON);
        assertThat(LeaderboardPeriod.parse("WEEK")).isEqualTo(LeaderboardPeriod.WEEK);
        assertThatThrownBy(() -> LeaderboardPeriod.parse("year")).isInstanceOf(IllegalArgumentException.class);
    }
}
