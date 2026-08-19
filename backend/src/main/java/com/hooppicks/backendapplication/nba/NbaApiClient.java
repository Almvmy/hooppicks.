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

    public List<com.hooppicks.backendapplication.nba.dto.NbaGameDto> fetchGamesForDates(List<LocalDate> dates) {
        StringBuilder url = new StringBuilder(baseUrl + "/nba/v1/games?per_page=100");
        for (LocalDate date : dates) {
            url.append("&dates[]=").append(date);
        }
        ResponseEntity<NbaGameListResponse> response = restTemplate.exchange(
                url.toString(), HttpMethod.GET, authEntity(), NbaGameListResponse.class);
        return response.getBody() != null ? response.getBody().data() : List.of();
    }

    public List<com.hooppicks.backendapplication.nba.dto.NbaPlayerDto> fetchPlayersForTeam(String teamExternalId) {
        String url = baseUrl + "/nba/v1/players?per_page=100&team_ids[]=" + teamExternalId;
        ResponseEntity<NbaPlayerListResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, authEntity(), NbaPlayerListResponse.class);
        return response.getBody() != null ? response.getBody().data() : List.of();
    }
}