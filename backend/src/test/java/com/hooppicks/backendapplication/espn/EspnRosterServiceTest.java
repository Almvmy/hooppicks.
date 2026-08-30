package com.hooppicks.backendapplication.espn;

import com.hooppicks.backendapplication.entity.RosterPlayer;
import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.repository.RosterPlayerRepository;
import com.hooppicks.backendapplication.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EspnRosterServiceTest {

    @Mock
    private EspnStatsClient espnStatsClient;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private RosterPlayerRepository rosterPlayerRepository;

    private EspnRosterService service;
    private Team team;

    @BeforeEach
    void setUp() {
        service = new EspnRosterService(espnStatsClient, teamRepository, rosterPlayerRepository);
        team = new Team();
        team.setId("1");
        team.setAbbreviation("ATL");
    }

    private EspnRosterRow row(String espnId, String injuryStatus) {
        return new EspnRosterRow(espnId, "Jane", "Doe", "G", "7", "6' 0\"", "180 lbs",
                "https://example.com/headshot.png", injuryStatus, null);
    }

    @Test
    void cree_les_nouveaux_joueurs_et_met_a_jour_leurs_champs() {
        when(espnStatsClient.fetchRoster("ATL")).thenReturn(List.of(row("100", null)));
        when(rosterPlayerRepository.findByTeamIdOrderByLastNameAsc("1")).thenReturn(List.of());
        when(rosterPlayerRepository.findById("100")).thenReturn(Optional.empty());

        service.syncTeamRoster(team);

        var captor = org.mockito.ArgumentCaptor.forClass(RosterPlayer.class);
        verify(rosterPlayerRepository).save(captor.capture());
        RosterPlayer saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo("100");
        assertThat(saved.getFirstName()).isEqualTo("Jane");
        assertThat(saved.getTeam()).isEqualTo(team);
    }

    @Test
    void supprime_les_joueurs_qui_ne_sont_plus_dans_la_reponse_espn() {
        RosterPlayer traded = new RosterPlayer();
        traded.setId("999");

        when(espnStatsClient.fetchRoster("ATL")).thenReturn(List.of(row("100", null)));
        when(rosterPlayerRepository.findByTeamIdOrderByLastNameAsc("1")).thenReturn(List.of(traded));
        when(rosterPlayerRepository.findById("100")).thenReturn(Optional.empty());

        service.syncTeamRoster(team);

        verify(rosterPlayerRepository).delete(traded);
    }

    @Test
    void garde_un_joueur_toujours_present_plutot_que_de_le_recreer() {
        RosterPlayer existing = new RosterPlayer();
        existing.setId("100");
        existing.setStatsUpdatedAt(java.time.Instant.parse("2026-01-01T00:00:00Z"));

        when(espnStatsClient.fetchRoster("ATL")).thenReturn(List.of(row("100", null)));
        when(rosterPlayerRepository.findByTeamIdOrderByLastNameAsc("1")).thenReturn(List.of(existing));
        when(rosterPlayerRepository.findById("100")).thenReturn(Optional.of(existing));

        service.syncTeamRoster(team);

        verify(rosterPlayerRepository, never()).delete(any());
        // Le même objet est mis à jour et resauvegardé, pas recréé : les
        // moyennes saison déjà présentes (statsUpdatedAt) survivent.
        assertThat(existing.getStatsUpdatedAt()).isNotNull();
    }

    @Test
    void reponse_vide_ne_touche_pas_au_roster_existant() {
        when(espnStatsClient.fetchRoster("ATL")).thenReturn(List.of());

        service.syncTeamRoster(team);

        verifyNoInteractions(rosterPlayerRepository);
        verify(teamRepository, never()).save(any());
    }

    @Test
    void compte_uniquement_le_statut_out_pour_outPlayersCount() {
        when(espnStatsClient.fetchRoster("ATL")).thenReturn(List.of(
                row("100", "Out"),
                row("101", "Day-To-Day"),
                row("102", null)
        ));
        when(rosterPlayerRepository.findByTeamIdOrderByLastNameAsc("1")).thenReturn(List.of());
        when(rosterPlayerRepository.findById(any())).thenReturn(Optional.empty());

        service.syncTeamRoster(team);

        assertThat(team.getOutPlayersCount()).isEqualTo(1);
    }

    @Test
    void un_echec_reseau_ne_leve_pas_d_exception() {
        when(espnStatsClient.fetchRoster("ATL")).thenThrow(new RuntimeException("boom"));

        service.syncTeamRoster(team); // ne doit pas propager
    }
}
