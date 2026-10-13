package com.hooppicks.backendapplication.nba;

import com.hooppicks.backendapplication.nba.dto.NbaGameListResponse;
import com.hooppicks.backendapplication.nba.dto.NbaPlayerListResponse;
import com.hooppicks.backendapplication.nba.dto.NbaTeamDto;
import com.hooppicks.backendapplication.nba.dto.NbaTeamListResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class NbaApiClient {

    private final RestTemplate restTemplate;

    @Value("${balldontlie.api-key}")
    private String apiKey;

    @Value("${balldontlie.base-url}")
    private String baseUrl;

    public NbaApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private HttpEntity<Void> authEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey);
        return new HttpEntity<>(headers);
    }

    public List<NbaTeamDto> fetchAllTeams() {
        ResponseEntity<NbaTeamListResponse> response = restTemplate.exchange(
                baseUrl + "/nba/v1/teams", HttpMethod.GET, authEntity(), NbaTeamListResponse.class);
        return response.getBody() != null ? response.getBody().data() : List.of();
    }

    public List<com.hooppicks.backendapplication.nba.dto.NbaPlayerDto> searchPlayers(String query, String teamId) {
        StringBuilder url = new StringBuilder(baseUrl + "/nba/v1/players?per_page=25&search=")
                .append(java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8));
        if (teamId != null && !teamId.isBlank()) {
            url.append("&team_ids[]=").append(teamId);
        }
        ResponseEntity<NbaPlayerListResponse> response = restTemplate.exchange(
                url.toString(), HttpMethod.GET, authEntity(), NbaPlayerListResponse.class);
        return response.getBody() != null ? response.getBody().data() : List.of();
    }

    public List<com.hooppicks.backendapplication.nba.dto.NbaPlayerDto> fetchPlayersForTeam(String teamExternalId) {
        String url = baseUrl + "/nba/v1/players?per_page=100&team_ids[]=" + teamExternalId;
        ResponseEntity<NbaPlayerListResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, authEntity(), NbaPlayerListResponse.class);
        return response.getBody() != null ? response.getBody().data() : List.of();
    }

    // Une semaine chargée dépasse les 100 matchs d'une page : on suit le
    // curseur, borné pour ne jamais vider le quota gratuit (5 req/min).
    private static final int MAX_GAME_PAGES = 3;

    public List<com.hooppicks.backendapplication.nba.dto.NbaGameDto> fetchGamesForDates(List<LocalDate> dates) {
        StringBuilder base = new StringBuilder(baseUrl + "/nba/v1/games?per_page=100");
        for (LocalDate date : dates) {
            base.append("&dates[]=").append(date);
        }
        List<com.hooppicks.backendapplication.nba.dto.NbaGameDto> games = new ArrayList<>();
        Long cursor = null;
        for (int page = 0; page < MAX_GAME_PAGES; page++) {
            String url = cursor == null ? base.toString() : base + "&cursor=" + cursor;
            ResponseEntity<NbaGameListResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, authEntity(), NbaGameListResponse.class);
            NbaGameListResponse body = response.getBody();
            if (body == null || body.data() == null) break;
            games.addAll(body.data());
            cursor = body.meta() != null ? body.meta().nextCursor() : null;
            if (cursor == null) break;
        }
        return games;
    }
}