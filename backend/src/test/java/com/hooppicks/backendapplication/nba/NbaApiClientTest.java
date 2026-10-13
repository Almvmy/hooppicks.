package com.hooppicks.backendapplication.nba;

import com.hooppicks.backendapplication.nba.dto.NbaGameDto;
import com.hooppicks.backendapplication.nba.dto.NbaGameListResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NbaApiClientTest {

    private RestTemplate restTemplate;
    private NbaApiClient client;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        client = new NbaApiClient(restTemplate);
        ReflectionTestUtils.setField(client, "apiKey", "cle");
        ReflectionTestUtils.setField(client, "baseUrl", "https://api.test");
    }

    @Test
    void suit_le_curseur_quand_la_fenetre_depasse_une_page() {
        NbaGameDto first = mock(NbaGameDto.class);
        NbaGameDto second = mock(NbaGameDto.class);
        when(restTemplate.exchange(argThat((String url) -> url != null && !url.contains("cursor=")),
                eq(HttpMethod.GET), any(HttpEntity.class), eq(NbaGameListResponse.class)))
                .thenReturn(ResponseEntity.ok(new NbaGameListResponse(List.of(first), new NbaGameListResponse.Meta(42L))));
        when(restTemplate.exchange(argThat((String url) -> url != null && url.endsWith("&cursor=42")),
                eq(HttpMethod.GET), any(HttpEntity.class), eq(NbaGameListResponse.class)))
                .thenReturn(ResponseEntity.ok(new NbaGameListResponse(List.of(second), new NbaGameListResponse.Meta(null))));

        List<NbaGameDto> games = client.fetchGamesForDates(List.of(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 21)));

        assertThat(games).containsExactly(first, second);
        verify(restTemplate, times(2)).exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(NbaGameListResponse.class));
    }

    @Test
    void s_arrete_apres_trois_pages_pour_preserver_le_quota() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(NbaGameListResponse.class)))
                .thenReturn(ResponseEntity.ok(new NbaGameListResponse(List.of(mock(NbaGameDto.class)), new NbaGameListResponse.Meta(7L))));

        assertThat(client.fetchGamesForDates(List.of(LocalDate.of(2026, 10, 20)))).hasSize(3);
        verify(restTemplate, times(3)).exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(NbaGameListResponse.class));
    }
}
