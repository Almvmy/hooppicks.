package com.hooppicks.backendapplication.espn;

import com.hooppicks.backendapplication.entity.MatchType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Intitulés ESPN relevés en direct sur la saison 2025-26.
class EspnMatchStageTest {

    private static EspnGameRow row(int seasonType, String competitionType, String note, String series) {
        return new EspnGameRow("1", null, "NYK", "ATL", null, null, "pre", false,
                seasonType, competitionType, note, series);
    }

    @Test
    void saison_reguliere_sans_intitule_ni_serie() {
        // "series" en saison régulière = bilan des confrontations, à ignorer.
        EspnMatchStage stage = EspnMatchStage.of(row(2, "STD", "", "SA wins series 4-3"));
        assertThat(stage).isEqualTo(new EspnMatchStage(MatchType.REGULAR, null, null));
    }

    @Test
    void presaison_avec_ou_sans_note() {
        assertThat(EspnMatchStage.of(row(1, "STD", "", null)))
                .isEqualTo(new EspnMatchStage(MatchType.PRESEASON, "Présaison", null));
        assertThat(EspnMatchStage.of(row(1, "STD", "NBA Abu Dhabi Game", "NY wins series 4-0")))
                .isEqualTo(new EspnMatchStage(MatchType.PRESEASON, "Présaison · NBA Abu Dhabi Game", null));
    }

    @Test
    void coupe_nba_par_tour() {
        assertThat(EspnMatchStage.of(row(2, "STD", "NBA Cup - Group Play", null)).label())
                .isEqualTo("Coupe NBA · Phase de groupes");
        assertThat(EspnMatchStage.of(row(2, "STD", "NBA Cup - Quarterfinals", null)).label())
                .isEqualTo("Coupe NBA · Quarts de finale");
        assertThat(EspnMatchStage.of(row(2, "STD", "NBA Cup - Semifinals", null)).label())
                .isEqualTo("Coupe NBA · Demi-finales");
        EspnMatchStage finale = EspnMatchStage.of(row(2, "CC", "NBA Cup Championship", null));
        assertThat(finale.type()).isEqualTo(MatchType.NBA_CUP);
        assertThat(finale.label()).isEqualTo("Coupe NBA · Finale");
    }

    @Test
    void all_star_reconnu_par_le_type_de_competition() {
        EspnMatchStage stage = EspnMatchStage.of(row(2, "ALLSTAR", "NBA All-Star - Round Robin", null));
        assertThat(stage.type()).isEqualTo(MatchType.ALL_STAR);
        assertThat(stage.label()).isEqualTo("All-Star Game");
    }

    @Test
    void play_in_avec_conference_et_places() {
        EspnMatchStage stage = EspnMatchStage.of(row(5, "STD", "NBA Play-In - West - 9th Place vs 10th Place", null));
        assertThat(stage.type()).isEqualTo(MatchType.PLAY_IN);
        assertThat(stage.label()).isEqualTo("Play-in Ouest · 9e contre 10e");
    }

    @Test
    void playoffs_tour_match_et_serie() {
        assertThat(EspnMatchStage.of(row(3, "RD16", "East 1st Round - Game 2", "NY leads series 2-0")))
                .isEqualTo(new EspnMatchStage(MatchType.PLAYOFFS, "1er tour Est · Match 2", "NYK mène 2-0"));
        assertThat(EspnMatchStage.of(row(3, "QTR", "West Semifinals - Game 1", "Series tied 1-1")))
                .isEqualTo(new EspnMatchStage(MatchType.PLAYOFFS, "Demi-finale Ouest · Match 1", "Égalité 1-1"));
        assertThat(EspnMatchStage.of(row(3, "SEMI", "East Finals - Game 4", "CLE wins series 4-0")))
                .isEqualTo(new EspnMatchStage(MatchType.PLAYOFFS, "Finale Est · Match 4", "CLE remporte la série 4-0"));
        assertThat(EspnMatchStage.of(row(3, "FINAL", "NBA Finals - Game 2", "SA leads series 2-0")).label())
                .isEqualTo("Finales NBA · Match 2");
    }

    @Test
    void intitule_inconnu_retombe_sur_un_libelle_generique() {
        assertThat(EspnMatchStage.of(row(3, "RD16", "", null)).label()).isEqualTo("Playoffs");
        assertThat(EspnMatchStage.of(row(5, "STD", "Something new", null)).label()).isEqualTo("Play-in");
    }
}
