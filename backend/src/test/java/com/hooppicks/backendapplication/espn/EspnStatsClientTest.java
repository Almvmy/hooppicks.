package com.hooppicks.backendapplication.espn;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EspnStatsClientTest {

    @Mock
    private RestTemplate restTemplate;

    private EspnStatsClient client;
    private final JsonMapper mapper = new JsonMapper();

    @BeforeEach
    void setUp() {
        client = new EspnStatsClient(restTemplate);
    }

    private void mockResponse(String json) throws Exception {
        JsonNode node = mapper.readTree(json);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(node));
    }

    @Test
    void toEspnAbbreviation_traduit_les_6_sigles_qui_different() {
        assertThat(EspnStatsClient.toEspnAbbreviation("GSW")).isEqualTo("GS");
        assertThat(EspnStatsClient.toEspnAbbreviation("NOP")).isEqualTo("NO");
        assertThat(EspnStatsClient.toEspnAbbreviation("NYK")).isEqualTo("NY");
        assertThat(EspnStatsClient.toEspnAbbreviation("SAS")).isEqualTo("SA");
        assertThat(EspnStatsClient.toEspnAbbreviation("UTA")).isEqualTo("UTAH");
        assertThat(EspnStatsClient.toEspnAbbreviation("WAS")).isEqualTo("WSH");
    }

    @Test
    void toEspnAbbreviation_laisse_les_autres_sigles_inchanges() {
        assertThat(EspnStatsClient.toEspnAbbreviation("LAL")).isEqualTo("LAL");
        assertThat(EspnStatsClient.toEspnAbbreviation("BOS")).isEqualTo("BOS");
    }

    @Test
    void findEventId_trouve_le_match_correspondant_par_date_et_equipes() throws Exception {
        mockResponse("""
            {
              "events": [
                {
                  "id": "400878160",
                  "competitions": [
                    { "competitors": [
                        { "team": { "abbreviation": "GS" }, "homeAway": "home" },
                        { "team": { "abbreviation": "CLE" }, "homeAway": "away" }
                    ] }
                  ]
                }
              ]
            }
        """);

        Optional<String> result = client.findEventId(LocalDate.of(2016, 6, 19), "GSW", "CLE");

        assertThat(result).contains("400878160");
    }

    @Test
    void findEventId_renvoie_vide_si_aucun_match_ne_correspond() throws Exception {
        mockResponse("""
            {
              "events": [
                {
                  "id": "999",
                  "competitions": [
                    { "competitors": [
                        { "team": { "abbreviation": "BOS" }, "homeAway": "home" },
                        { "team": { "abbreviation": "MIA" }, "homeAway": "away" }
                    ] }
                  ]
                }
              ]
            }
        """);

        Optional<String> result = client.findEventId(LocalDate.of(2016, 6, 19), "GSW", "CLE");

        assertThat(result).isEmpty();
    }

    @Test
    void fetchBoxScore_parse_les_lignes_de_joueurs_et_ignore_les_dnp() throws Exception {
        mockResponse("""
            {
              "boxscore": {
                "players": [
                  {
                    "team": { "abbreviation": "CLE" },
                    "statistics": [
                      {
                        "labels": ["MIN","PTS","FG","3PT","FT","REB","AST","TO","STL","BLK","OREB","DREB","PF","+/-"],
                        "athletes": [
                          {
                            "athlete": { "displayName": "LeBron James" },
                            "starter": true,
                            "didNotPlay": false,
                            "stats": ["47","27","9-24","1-5","8-10","11","11","5","2","3","1","10","1","+4"]
                          },
                          {
                            "athlete": { "displayName": "Benched Guy" },
                            "starter": false,
                            "didNotPlay": true,
                            "stats": []
                          }
                        ]
                      }
                    ]
                  }
                ]
              }
            }
        """);

        List<PlayerBoxScoreRow> rows = client.fetchBoxScore("400878160");

        assertThat(rows).hasSize(1);
        PlayerBoxScoreRow row = rows.get(0);
        assertThat(row.playerName()).isEqualTo("LeBron James");
        assertThat(row.teamAbbreviation()).isEqualTo("CLE");
        assertThat(row.starter()).isTrue();
        assertThat(row.points()).isEqualTo(27);
        assertThat(row.rebounds()).isEqualTo(11);
        assertThat(row.assists()).isEqualTo(11);
        assertThat(row.plusMinus()).isEqualTo(4);
        assertThat(row.fieldGoals()).isEqualTo(new PlayerBoxScoreRow.ShotSplit(9, 24));
        assertThat(row.threePoints()).isEqualTo(new PlayerBoxScoreRow.ShotSplit(1, 5));
        assertThat(row.freeThrows()).isEqualTo(new PlayerBoxScoreRow.ShotSplit(8, 10));
    }

    @Test
    void fetchBoxScore_gere_un_moins_en_plusMinus() throws Exception {
        mockResponse("""
            {
              "boxscore": {
                "players": [
                  {
                    "team": { "abbreviation": "CLE" },
                    "statistics": [
                      {
                        "labels": ["MIN","PTS","FG","3PT","FT","REB","AST","TO","STL","BLK","OREB","DREB","PF","+/-"],
                        "athletes": [
                          {
                            "athlete": { "displayName": "Bench Player" },
                            "starter": false,
                            "didNotPlay": false,
                            "stats": ["10","4","2-5","0-1","0-0","2","1","0","0","0","1","1","2","-6"]
                          }
                        ]
                      }
                    ]
                  }
                ]
              }
            }
        """);

        List<PlayerBoxScoreRow> rows = client.fetchBoxScore("400878160");

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).plusMinus()).isEqualTo(-6);
    }
}
