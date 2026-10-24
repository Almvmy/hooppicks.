package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.dto.PlayerLeadersDto;
import com.hooppicks.backendapplication.dto.RosterPlayerDto;
import com.hooppicks.backendapplication.entity.RosterPlayer;
import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.espn.EspnStatsClient;
import com.hooppicks.backendapplication.nba.NbaSyncService;
import com.hooppicks.backendapplication.repository.RosterPlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerControllerTest {

    @Mock
    private RosterPlayerRepository rosterPlayerRepository;
    @Mock
    private NbaSyncService nbaSyncService;
    @Mock
    private EspnStatsClient espnStatsClient;

    private PlayerController controller;

    @BeforeEach
    void setUp() {
        controller = new PlayerController(rosterPlayerRepository, nbaSyncService, espnStatsClient,
                org.mockito.Mockito.mock(com.hooppicks.backendapplication.espn.StatLeadersService.class));
    }

    private static Team team(String name) {
        Team t = new Team();
        t.setName(name);
        t.setAbbreviation(name.substring(0, 3).toUpperCase());
        return t;
    }

    private static RosterPlayer player(String lastName, Team team, String injuryStatus) {
        RosterPlayer p = new RosterPlayer();
        p.setId("id-" + lastName);
        p.setLastName(lastName);
        p.setTeam(team);
        p.setInjuryStatus(injuryStatus);
        return p;
    }

    @Test
    void blesses_tries_par_equipe_puis_out_avant_les_incertains() {
        Team celtics = team("Celtics");
        Team bucks = team("Bucks");
        // Le dépôt renvoie déjà par nom : l'ordre d'équipe et la priorité "Out" viennent du contrôleur.
        when(rosterPlayerRepository.findByInjuryStatusIsNotNullOrderByLastNameAsc()).thenReturn(List.of(
                player("Brown", celtics, "Day-To-Day"),
                player("Holiday", bucks, "Out"),
                player("Tatum", celtics, "Out"),
                player("Lillard", bucks, "Day-To-Day")
        ));

        List<RosterPlayerDto> injured = controller.injuries();

        assertThat(injured).extracting(RosterPlayerDto::lastName)
                .containsExactly("Holiday", "Lillard", "Tatum", "Brown");
    }

}
