package com.hooppicks.backendapplication.chat;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DebateNewsTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 6);

    private static DebateNews.Performance perf(String name, int points) {
        return new DebateNews.Performance(name, points, 3, 3);
    }

    @Test
    void une_nuit_calme_ne_donne_pas_de_debat() {
        assertThat(DebateNews.pick(DAY, List.of(perf("Joueur", 28)), List.of(new DebateNews.TeamForm("Knicks", "W2")), Set.of(), List.of())).isEmpty();
    }

    @Test
    void le_gros_exploit_passe_avant_tout() {
        Optional<DebateNews.News> news = DebateNews.pick(DAY, List.of(perf("Curry", 42), perf("Edwards", 61)), List.of(new DebateNews.TeamForm("Knicks", "W9")), Set.of(), List.of());

        assertThat(news.orElseThrow().kind()).isEqualTo("exploit");
        assertThat(news.get().question()).startsWith("Edwards à 61 points");
    }

    @Test
    void triple_double_serie_et_crise() {
        DebateNews.Performance jokic = new DebateNews.Performance("Jokic", 30, 12, 11);
        assertThat(DebateNews.pick(DAY, List.of(jokic), List.of(), Set.of(), List.of()).orElseThrow().question()).contains("Triple-double de Jokic (30-12-11)");
        assertThat(DebateNews.pick(DAY, List.of(), List.of(new DebateNews.TeamForm("Thunder", "W8")), Set.of(), List.of()).orElseThrow().question()).isEqualTo("Les Thunder enchaînent 8 victoires : candidats au titre ?");
        assertThat(DebateNews.pick(DAY, List.of(), List.of(new DebateNews.TeamForm("Wizards", "L7")), Set.of(), List.of()).orElseThrow().kind()).isEqualTo("crise");
    }

    @Test
    void pas_le_meme_type_de_debat_ces_derniers_jours() {
        Optional<DebateNews.News> news = DebateNews.pick(DAY, List.of(perf("Curry", 42)), List.of(new DebateNews.TeamForm("Knicks", "W9")), Set.of("gros-match"), List.of());

        assertThat(news.orElseThrow().kind()).isEqualTo("serie");
    }

    @Test
    void on_ne_reparle_pas_tout_de_suite_du_meme_joueur_ou_de_la_meme_equipe() {
        assertThat(DebateNews.pick(DAY, List.of(), List.of(new DebateNews.TeamForm("Thunder", "W8")), Set.of(), List.of("Les Thunder enchaînent 7 victoires : candidats au titre ?"))).isEmpty();
    }

    @Test
    void toutes_les_questions_tiennent_dans_la_limite() {
        String longName = "Giannis Antetokounmpo-Johnson";
        for (int i = 0; i < 3; i++) {
            DebateNews.News news = DebateNews.pick(DAY.plusDays(i), List.of(perf(longName, 45)), List.of(), Set.of(), List.of()).orElseThrow();
            assertThat(news.question().length()).isLessThanOrEqualTo(DebateService.MAX_QUESTION);
        }
    }
}
