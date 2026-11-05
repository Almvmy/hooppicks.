package com.hooppicks.backendapplication.news;

import com.hooppicks.backendapplication.espn.EspnStatsClient;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NewsTranslationTest {

    private final DeepLService deepL = mock(DeepLService.class);
    private final NewsService service = new NewsService(new RestTemplate(), deepL, new EspnStatsClient(new RestTemplate()));

    private static NewsItemDto article(String title, String description) {
        return NewsItemDto.fromRss(title, "https://espn.test/" + title, description, Instant.now());
    }

    @Test
    void ne_renvoie_a_deepl_que_les_textes_jamais_traduits() {
        when(deepL.translateToFrench(List.of("Lakers win", "Big night")))
                .thenReturn(new DeepLService.Result(Map.of("Lakers win", "Victoire des Lakers", "Big night", "Grosse soirée"), null));
        service.translate(List.of(article("Lakers win", "Big night")));

        when(deepL.translateToFrench(List.of("Celtics lose")))
                .thenReturn(new DeepLService.Result(Map.of("Celtics lose", "Défaite des Celtics"), null));
        List<NewsItemDto> result = service.translate(List.of(article("Lakers win", "Big night"), article("Celtics lose", null)));

        verify(deepL).translateToFrench(List.of("Celtics lose"));
        assertThat(result).extracting(NewsItemDto::title).containsExactly("Victoire des Lakers", "Défaite des Celtics");
        assertThat(result.get(0).description()).isEqualTo("Grosse soirée");
        assertThat(result.get(1).description()).isNull();
    }

    @Test
    void garde_l_anglais_si_deepl_ne_repond_pas() {
        when(deepL.translateToFrench(anyList())).thenReturn(new DeepLService.Result(Map.of(), "Quota DeepL dépassé (456)"));
        List<NewsItemDto> result = service.translate(List.of(article("Lakers win", "Big night")));
        assertThat(result.get(0).title()).isEqualTo("Lakers win");
    }

    // Les textes vus pendant une panne restaient en anglais jusqu'au
    // redémarrage suivant, même une fois le quota revenu.
    @Test
    void retente_apres_une_panne_de_deepl() {
        when(deepL.translateToFrench(anyList())).thenReturn(new DeepLService.Result(Map.of(), "Quota DeepL dépassé (456)"));
        service.translate(List.of(article("Lakers win", null)));

        when(deepL.translateToFrench(List.of("Lakers win")))
                .thenReturn(new DeepLService.Result(Map.of("Lakers win", "Victoire des Lakers"), null));
        assertThat(service.translate(List.of(article("Lakers win", null))).get(0).title()).isEqualTo("Victoire des Lakers");
    }
}
