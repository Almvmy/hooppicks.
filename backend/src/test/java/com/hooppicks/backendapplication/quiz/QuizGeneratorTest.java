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
                if (q.text().equals("Qui a marqué le plus de points la nuit dernière ?")) assertThat(q.choices().get(q.correct())).isEqualTo("Joueur 0");
            }
        }
    }

    @Test
    void egalite_en_tete_de_la_nuit_pas_de_question_sur_le_meilleur_marqueur() {
        List<QuizGenerator.NightLine> tied = List.of(new QuizGenerator.NightLine("A", 40), new QuizGenerator.NightLine("B", 40),
                new QuizGenerator.NightLine("C", 30), new QuizGenerator.NightLine("D", 20));
        QuizGenerator.Facts facts = new QuizGenerator.Facts(List.of(), List.of(), List.of(), tied);
        for (int i = 0; i < 30; i++) {
            assertThat(QuizGenerator.generate(DAY.plusDays(i), facts)).noneMatch(q -> q.text().equals("Qui a marqué le plus de points la nuit dernière ?"));
        }
    }

    @Test
    void seulement_les_120_meilleurs_marqueurs() {
        List<QuizGenerator.PlayerFact> players = new ArrayList<>();
        for (int i = 0; i < 120; i++) players.add(new QuizGenerator.PlayerFact("Connu " + i, "Équipe " + (i % 8), "1", "https://p/" + i, 30.0, 10, 30.0 - i * 0.1, 5.0, 5.0));
        for (int i = 0; i < 40; i++) players.add(new QuizGenerator.PlayerFact("Inconnu " + i, "Équipe " + (i % 8), "1", "https://q/" + i, 22.0, 10, 5.0, 3.0, 1.0));
        QuizGenerator.Facts facts = new QuizGenerator.Facts(players, List.of(), List.of(), List.of());
        for (int i = 0; i < 60; i++) {
            assertThat(QuizGenerator.generate(DAY.plusDays(i), facts)).noneMatch(q -> q.text().contains("Inconnu") || String.join(" ", q.choices()).contains("Inconnu"));
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
    void la_liste_d_histoire_est_assez_longue_et_bien_formee() {
        assertThat(QuizBank.ITEMS.size()).isGreaterThanOrEqualTo(200);
        assertThat(QuizBank.ITEMS).allSatisfy(item -> {
            assertThat(item.choices()).hasSize(4).doesNotHaveDuplicates();
            assertThat(item.text().length()).isLessThanOrEqualTo(200);
        });
        assertThat(QuizBank.ITEMS.stream().map(QuizBank.Item::text).toList()).doesNotHaveDuplicates();
    }

    @Test
    void aucune_question_d_histoire_ne_revient_tant_qu_il_en_reste() {
        HashSet<String> used = new HashSet<>();
        int perDay = 2;
        for (int i = 0; i < QuizBank.ITEMS.size() / perDay - 15; i++) {
            for (QuizBank.Item item : QuizBank.pick(DAY.plusDays(i), perDay, used)) {
                assertThat(used.add(item.text())).as(item.text()).isTrue();
            }
        }
    }

    @Test
    void pas_deux_questions_du_meme_theme_le_meme_jour() {
        for (int i = 0; i < 100; i++) {
            List<String> themes = QuizBank.pick(DAY.plusDays(i), 5, java.util.Set.of()).stream().map(QuizBank.Item::theme).toList();
            assertThat(themes).hasSize(5).doesNotHaveDuplicates();
        }
    }

    @Test
    void liste_epuisee_le_quiz_reste_complet() {
        java.util.Set<String> all = QuizBank.ITEMS.stream().map(QuizBank.Item::text).collect(java.util.stream.Collectors.toSet());
        assertThat(QuizBank.pick(DAY, 2, all)).hasSize(2);
    }

    @Test
    void le_meme_modele_ne_revient_pas_le_lendemain_quand_il_y_a_le_choix() {
        QuizGenerator.Facts facts = fullNight();
        java.util.Set<String> recent = new HashSet<>();
        for (int i = 0; i < 40; i++) {
            List<QuizGenerator.Question> questions = QuizGenerator.generate(DAY.plusDays(i), facts, new QuizGenerator.History(java.util.Set.of(), java.util.Set.copyOf(recent)));
            List<String> dataKinds = questions.stream().map(QuizGenerator.Question::kind).filter(k -> !k.startsWith("histoire-")).toList();
            assertThat(dataKinds).doesNotHaveDuplicates().hasSize(3).noneMatch(recent::contains);
            recent = new HashSet<>(dataKinds);
        }
    }

    @Test
    void chaque_modele_de_la_nuit_donne_la_bonne_reponse() {
        QuizGenerator.Facts facts = fullNight();
        java.util.Map<String, String> expected = new java.util.HashMap<>();
        expected.put("Qui a marqué le plus de points la nuit dernière ?", "Joueur 0");
        expected.put("Qui a pris le plus de rebonds la nuit dernière ?", "Rebondeur");
        expected.put("Qui a fait le plus de passes la nuit dernière ?", "Passeur");
        expected.put("Qui a réussi le plus de tirs à 3 points la nuit dernière ?", "Sniper");
        expected.put("Qui a réussi un triple-double la nuit dernière ?", "Polyvalent");
        expected.put("Quel match a fini avec le plus gros écart la nuit dernière ?", "A @ B");
        java.util.Set<String> seen = new HashSet<>();
        for (int i = 0; i < 300; i++) {
            for (QuizGenerator.Question q : QuizGenerator.generate(DAY.plusDays(i), facts)) {
                if (expected.containsKey(q.text()) && q.choices().contains(expected.get(q.text()))) {
                    assertThat(q.choices().get(q.correct())).as(q.text()).isEqualTo(expected.get(q.text()));
                    seen.add(q.text());
                }
            }
        }
        assertThat(seen).hasSize(expected.size());
    }

    @Test
    void qui_mene_la_conference_et_plus_longue_serie() {
        List<QuizGenerator.TeamFact> teams = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            teams.add(new QuizGenerator.TeamFact("Est " + i, "Est", 20 - i, i, i, "W" + (16 - i)));
            teams.add(new QuizGenerator.TeamFact("Ouest " + i, "Ouest", 20 - i, i, i, "L" + i));
        }
        QuizGenerator.Facts facts = new QuizGenerator.Facts(List.of(), teams, List.of(), List.of());
        int checked = 0;
        for (int i = 0; i < 300; i++) {
            for (QuizGenerator.Question q : QuizGenerator.generate(DAY.plusDays(i), facts)) {
                if (q.text().startsWith("Qui mène la conférence Est")) {
                    assertThat(q.choices().get(q.correct())).isEqualTo("Est 1");
                    checked++;
                }
                if (q.text().startsWith("Laquelle de ces équipes a la plus longue série")) {
                    int best = q.choices().stream().filter(c -> c.startsWith("Est")).mapToInt(c -> 16 - Integer.parseInt(c.substring(4))).max().orElse(0);
                    assertThat(q.choices().get(q.correct())).isEqualTo("Est " + (16 - best));
                    checked++;
                }
            }
        }
        assertThat(checked).isGreaterThan(0);
    }

    private static QuizGenerator.Facts fullNight() {
        List<QuizGenerator.NightLine> lines = new ArrayList<>();
        for (int i = 0; i < 16; i++) lines.add(new QuizGenerator.NightLine("Joueur " + i, 40 - i, 3, 2, 1));
        lines.add(new QuizGenerator.NightLine("Rebondeur", 8, 19, 1, 0));
        lines.add(new QuizGenerator.NightLine("Passeur", 7, 2, 17, 0));
        lines.add(new QuizGenerator.NightLine("Sniper", 9, 1, 1, 9));
        lines.add(new QuizGenerator.NightLine("Polyvalent", 14, 11, 12, 0));
        List<QuizGenerator.GameFact> games = List.of(
                new QuizGenerator.GameFact("A", "B", 90, 130),
                new QuizGenerator.GameFact("C", "D", 100, 104),
                new QuizGenerator.GameFact("E", "F", 110, 101),
                new QuizGenerator.GameFact("G", "H", 99, 98));
        QuizGenerator.Facts base = rich();
        return new QuizGenerator.Facts(base.players(), base.teams(), games, lines);
    }
}
