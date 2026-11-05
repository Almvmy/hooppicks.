package com.hooppicks.backendapplication.news;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DeepLServiceTest {

    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final DeepLService service = new DeepLService(restTemplate);

    DeepLServiceTest() {
        ReflectionTestUtils.setField(service, "apiKey", "cle");
        ReflectionTestUtils.setField(service, "apiUrl", "https://deepl.test");
    }

    private static DeepLResponse response(int size) {
        return new DeepLResponse(IntStream.range(0, size).mapToObj(i -> new DeepLResponse.Translation("fr" + i)).toList());
    }

    private static List<String> texts(int size) {
        return IntStream.range(0, size).mapToObj(i -> "en" + i).toList();
    }

    private static HttpClientErrorException quota() {
        return HttpClientErrorException.create(HttpStatusCode.valueOf(456), "Quota", new HttpHeaders(), new byte[0], null);
    }

    @Test
    void trente_articles_partent_en_deux_lots() {
        when(restTemplate.postForObject(anyString(), any(), eq(DeepLResponse.class))).thenReturn(response(50), response(10));

        DeepLService.Result result = service.translateToFrench(texts(60));

        verify(restTemplate, times(2)).postForObject(anyString(), any(), eq(DeepLResponse.class));
        assertThat(result.translations()).hasSize(60);
        assertThat(result.error()).isNull();
    }

    @Test
    void quota_depasse_sans_nouvelle_tentative_ni_faux_texte_traduit() {
        when(restTemplate.postForObject(anyString(), any(), eq(DeepLResponse.class))).thenThrow(quota());

        DeepLService.Result result = service.translateToFrench(texts(2));

        verify(restTemplate, times(1)).postForObject(anyString(), any(), eq(DeepLResponse.class));
        assertThat(result.translations()).isEmpty();
        assertThat(result.error()).contains("456");
    }

    @Test
    void un_lot_refuse_garde_les_traductions_du_lot_precedent() {
        when(restTemplate.postForObject(anyString(), any(), eq(DeepLResponse.class))).thenReturn(response(50)).thenThrow(quota());

        DeepLService.Result result = service.translateToFrench(texts(60));

        assertThat(result.translations()).hasSize(50);
        assertThat(result.error()).isNotNull();
    }
}
