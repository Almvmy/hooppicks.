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
    void fetchScoreboard_lit_equipes_score_statut_et_phase() throws Exception {
        mockResponse("""
            {
              "events": [
                {
                  "id": "401869999",
                  "date": "2026-04-21T23:30Z",
                  "season": { "type": 3 },
                  "competitions": [
                    {
                      "type": { "abbreviation": "RD16" },
                      "status": { "type": { "state": "post", "completed": true } },
                      "notes": [ { "headline": "East 1st Round - Game 2" } ],
                      "series": { "summary": "NY leads series 2-0" },
                      "competitors": [
                        { "team": { "abbreviation": "NY" }, "homeAway": "home", "score": "112" },
                        { "team": { "abbreviation": "ATL" }, "homeAway": "away", "score": "104" }
                      ]
                    }
                  ]
                }
              ]
            }
        """);

        List<EspnGameRow> rows = client.fetchScoreboard(LocalDate.of(2026, 4, 21)).orElseThrow();

        assertThat(rows).hasSize(1);
        EspnGameRow row = rows.get(0);
        assertThat(row.eventId()).isEqualTo("401869999");
        assertThat(row.date()).isEqualTo(java.time.Instant.parse("2026-04-21T23:30:00Z"));
        // Sigles ESPN reconvertis aux nôtres (NY -> NYK).
        assertThat(row.homeAbbreviation()).isEqualTo("NYK");
        assertThat(row.awayAbbreviation()).isEqualTo("ATL");
        assertThat(row.homeScore()).isEqualTo(112);
        assertThat(row.matchStatus()).isEqualTo(com.hooppicks.backendapplication.entity.MatchStatus.FINISHED);
        assertThat(row.seasonType()).isEqualTo(3);
        assertThat(row.note()).isEqualTo("East 1st Round - Game 2");
        assertThat(row.seriesSummary()).isEqualTo("NY leads series 2-0");
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

    @Test
    void fetchNews_extrait_photo_equipes_aux_sigles_de_notre_base_et_type_video() throws Exception {
        mockResponse("""
            {
              "articles": [
                {
                  "type": "HeadlineNews",
                  "headline": "Knicks' Towns not expecting extension soon",
                  "description": "Karl-Anthony Towns...",
                  "published": "2026-10-05T17:11:40Z",
                  "links": { "web": { "href": "https://www.espn.com/nba/story/_/id/1" } },
                  "images": [
                    { "type": "Media", "url": "https://img/media.jpg" },
                    { "type": "header", "url": "https://img/header.jpg" }
                  ],
                  "categories": [
                    { "type": "league", "description": "NBA" },
                    { "type": "team", "team": { "abbreviation": "NY" } },
                    { "type": "team", "team": { "abbreviation": "BOS" } },
                    { "type": "athlete", "description": "Karl-Anthony Towns" }
                  ]
                },
                {
                  "type": "Media",
                  "headline": "Highlights",
                  "published": "2026-10-05T16:00:00Z",
                  "links": { "web": { "href": "https://www.espn.com/video/2" } },
                  "images": [ { "type": "Media", "url": "https://img/video.jpg" } ],
                  "categories": [ { "type": "team", "team": { "abbreviation": "GS" } } ]
                },
                { "headline": "Sans lien : ignoré" }
              ]
            }
            """);

        List<EspnNewsRow> rows = client.fetchNews(30);

        assertThat(rows).hasSize(2);
        EspnNewsRow story = rows.get(0);
        assertThat(story.imageUrl()).isEqualTo("https://img/header.jpg"); // la photo "header" passe devant
        assertThat(story.teamAbbreviations()).containsExactly("NYK", "BOS"); // NY (ESPN) -> NYK (balldontlie)
        assertThat(story.video()).isFalse();
        assertThat(story.publishedAt()).isEqualTo(java.time.Instant.parse("2026-10-05T17:11:40Z"));

        EspnNewsRow video = rows.get(1);
        assertThat(video.video()).isTrue();
        assertThat(video.imageUrl()).isEqualTo("https://img/video.jpg");
        assertThat(video.teamAbbreviations()).containsExactly("GSW");
    }

    @Test
    void fromEspnAbbreviation_est_l_inverse_de_toEspnAbbreviation() {
        for (String ours : List.of("GSW", "NOP", "NYK", "SAS", "UTA", "WAS", "LAL", "BOS")) {
            assertThat(EspnStatsClient.fromEspnAbbreviation(EspnStatsClient.toEspnAbbreviation(ours))).isEqualTo(ours);
        }
    }
}
