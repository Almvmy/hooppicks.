package com.hooppicks.backendapplication.power;

import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.entity.TeamEloSnapshot;
import com.hooppicks.backendapplication.repository.TeamEloSnapshotRepository;
import com.hooppicks.backendapplication.repository.TeamRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PowerRankingServiceTest {

    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final TeamEloSnapshotRepository snapshotRepository = mock(TeamEloSnapshotRepository.class);
    private final PowerRankingService service = new PowerRankingService(teamRepository, snapshotRepository,
            Clock.fixed(Instant.parse("2026-11-10T15:00:00Z"), ZoneOffset.UTC));

    private static Team team(String id, String abbr, double elo) {
        Team t = new Team();
        t.setId(id);
        t.setAbbreviation(abbr);
        t.setEloRating(elo);
        return t;
    }

    private static TeamEloSnapshot snap(String teamId, double elo) {
        TeamEloSnapshot s = new TeamEloSnapshot();
        s.setTeamId(teamId);
        s.setElo(elo);
        return s;
    }

    @Test
    void rang_actuel_et_evolution_sur_sept_jours() {
        when(teamRepository.findAll()).thenReturn(List.of(team("1", "BOS", 1600), team("2", "OKC", 1650), team("3", "NYK", 1500)));
        LocalDate weekAgo = LocalDate.of(2026, 11, 3);
        when(snapshotRepository.findLatestDateOnOrBefore(weekAgo)).thenReturn(Optional.of(weekAgo));
        when(snapshotRepository.findByDate(weekAgo)).thenReturn(List.of(snap("1", 1620), snap("2", 1600), snap("3", 1510)));

        List<PowerRankingService.Entry> r = service.rankings();

        assertThat(r).extracting(PowerRankingService.Entry::abbreviation).containsExactly("OKC", "BOS", "NYK");
        assertThat(r.get(0).previousRank()).isEqualTo(2);
        assertThat(r.get(0).eloChange()).isEqualTo(50);
        assertThat(r.get(1).previousRank()).isEqualTo(1);
    }

    @Test
    void sans_photo_assez_ancienne_pas_d_evolution() {
        when(teamRepository.findAll()).thenReturn(List.of(team("1", "BOS", 1600)));
        when(snapshotRepository.findLatestDateOnOrBefore(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty());

        assertThat(service.rankings()).singleElement().satisfies(e -> {
            assertThat(e.previousRank()).isNull();
            assertThat(e.eloChange()).isNull();
        });
    }
}
