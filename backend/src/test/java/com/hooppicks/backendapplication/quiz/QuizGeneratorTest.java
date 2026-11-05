package com.hooppicks.backendapplication.quiz;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuizGeneratorTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 6);
    private static final QuizGenerator.Facts EMPTY = new QuizGenerator.Facts(List.of(), List.of(), List.of(), List.of());

    private static QuizGenerator.Facts rich() {
        List<QuizGenerator.PlayerFact> players = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            players.add(new QuizGenerator.PlayerFact("Joueur " + i, "Équipe " + (i % 8), String.valueOf(i), "https://photo/" + i, 30.0, 10, 10.0 + i, 3.0 + i, 2.0 + i));
        }
        List<QuizGenerator.TeamFact> teams = new ArrayList<>();
        for (int i = 0; i < 8; i++) teams.add(new QuizGenerator.TeamFact("Équipe " + i, i % 2 == 0 ? "Est" : "Ouest", i, 8 - i));
        List<QuizGenerator.NightLine> lines = new ArrayList<>();
        for (int i = 0; i < 20; i++) lines.add(new QuizGenerator.NightLine("Joueur " + i, 40 - i));
        return new QuizGenerator.Facts(players, teams, List.of(new QuizGenerator.GameFact("BKN", "CLE", 85, 88)), lines);
    }

    private static void wellFormed(List<QuizGenerator.Question> questions) {
        assertThat(questions).hasSize(QuizGenerator.QUESTIONS);
        assertThat(questions).allSatisfy(q -> {
            assertThat(q.choices()).hasSize(4).doesNotHaveDuplicates();
            assertThat(q.correct()).isBetween(0, 3);
            assertThat(q.text().length()).isLessThanOrEqualTo(200);
            assertThat(String.join("\n", q.choices()).length()).isLessThanOrEqualTo(400);
        });
    }

    @Test
    void sans_aucune_donnee_le_quiz_reste_jouable_avec_la_liste_d_histoire() {
        wellFormed(QuizGenerator.generate(DAY, EMPTY));
    }

    @Test
    void des_questions_bien_formees_sur_toute_une_saison() {
        QuizGenerator.Facts facts = rich();
        for (int i = 0; i < 200; i++) wellFormed(QuizGenerator.generate(DAY.plusDays(i), facts));
    }

    @Test
    void la_bonne_reponse_n_a_pas_de_place_preferee() {
        int[] places = new int[4];
        int total = 0;
        QuizGenerator.Facts facts = rich();
        for (int i = 0; i < 400; i++) {
            for (QuizGenerator.Question q : QuizGenerator.generate(DAY.plusDays(i), facts)) {
                places[q.correct()]++;
                total++;
            }
        }
        for (int place : places) assertThat((double) place / total).as(java.util.Arrays.toString(places)).isBetween(0.20, 0.30);
    }

    @Test
    void la_bonne_reponse_change_de_place_d_un_jour_a_l_autre_meme_sans_donnees() {
        int[] places = new int[4];
        for (int i = 0; i < 400; i++) {
            for (QuizGenerator.Question q : QuizGenerator.generate(DAY.plusDays(i), EMPTY)) places[q.correct()]++;
        }
        for (int place : places) assertThat((double) place / 2000).as(java.util.Arrays.toString(places)).isBetween(0.20, 0.30);
    }

    @Test
    void meme_jour_memes_donnees_memes_questions() {
        assertThat(QuizGenerator.generate(DAY, rich())).isEqualTo(QuizGenerator.generate(DAY, rich()));
    }

    @Test
    void la_nuit_derniere_est_toujours_au_programme_quand_il_y_a_eu_des_matchs() {
        for (int i = 0; i < 30; i++) {
            assertThat(QuizGenerator.generate(DAY.plusDays(i), rich())).anyMatch(q -> q.text().contains("nuit dernière"));
        }
    }

    @Test
    void le_meilleur_marqueur_est_bien_la_bonne_reponse() {
        for (int i = 0; i < 60; i++) {
            for (QuizGenerator.Question q : QuizGenerator.generate(DAY.plusDays(i), rich())) {
                if (q.text().startsWith("Qui a marqué")) assertThat(q.choices().get(q.correct())).isEqualTo("Joueur 0");
            }
        }
    }

    @Test
    void egalite_en_tete_de_la_nuit_pas_de_question_sur_le_meilleur_marqueur() {
        List<QuizGenerator.NightLine> tied = List.of(new QuizGenerator.NightLine("A", 40), new QuizGenerator.NightLine("B", 40),
                new QuizGenerator.NightLine("C", 30), new QuizGenerator.NightLine("D", 20));
        QuizGenerator.Facts facts = new QuizGenerator.Facts(List.of(), List.of(), List.of(), tied);
        for (int i = 0; i < 30; i++) {
            assertThat(QuizGenerator.generate(DAY.plusDays(i), facts)).noneMatch(q -> q.text().startsWith("Qui a marqué"));
        }
    }

    @Test
    void pas_de_question_sur_un_joueur_hors_rotation() {
        List<QuizGenerator.PlayerFact> bench = new ArrayList<>();
        for (int i = 0; i < 40; i++) bench.add(new QuizGenerator.PlayerFact("Remplaçant " + i, "Équipe " + (i % 8), "1", "https://p/" + i, 8.0, 10, 3.0, 1.0, 1.0));
        QuizGenerator.Facts facts = new QuizGenerator.Facts(bench, List.of(), List.of(), List.of());
        for (int i = 0; i < 30; i++) {
            assertThat(QuizGenerator.generate(DAY.plusDays(i), facts)).noneMatch(q -> q.text().contains("Remplaçant") || String.join(" ", q.choices()).contains("Remplaçant"));
        }
    }

    @Test
    void la_liste_d_histoire_ne_se_repete_pas_avant_un_mois() {
        HashSet<String> seen = new HashSet<>();
        for (int i = 0; i < 28; i++) {
            assertThat(seen.add(QuizBank.of(DAY.plusDays(i), 0).text())).isTrue();
            assertThat(seen.add(QuizBank.of(DAY.plusDays(i), 1).text())).isTrue();
        }
        assertThat(QuizBank.ITEMS).allSatisfy(item -> assertThat(item.choices()).hasSize(4).doesNotHaveDuplicates());
    }
}
