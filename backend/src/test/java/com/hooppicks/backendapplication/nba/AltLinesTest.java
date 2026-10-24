package com.hooppicks.backendapplication.nba;

import com.hooppicks.backendapplication.entity.Match;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class AltLinesTest {

    private static Match match(double spread, double total) {
        Match m = new Match();
        m.setSpreadValue(spread);
        m.setTotalValue(total);
        return m;
    }

    @Test
    void loi_normale() {
        assertThat(AltLines.normalCdf(0)).isCloseTo(0.5, within(1e-6));
        assertThat(AltLines.normalCdf(1.96)).isCloseTo(0.975, within(1e-3));
        assertThat(AltLines.normalCdf(-1)).isCloseTo(0.1587, within(1e-3));
    }

    @Test
    void la_ligne_principale_reste_a_1_96() {
        Match m = match(-4.5, 226.5);
        assertThat(AltLines.odds(m, AltLines.ALT_SPREAD, "home", -4.5)).isEqualTo(1.96);
        assertThat(AltLines.odds(m, AltLines.ALT_TOTAL, "over", 226.5)).isEqualTo(1.96);
    }

    @Test
    void handicaps_autour_de_la_ligne_cote_qui_suit_le_risque() {
        Match m = match(-4.5, 226.5);
        List<AltLines.Line> lines = AltLines.of(m);

        assertThat(lines).filteredOn(l -> l.market().equals(AltLines.ALT_SPREAD))
                .extracting(AltLines.Line::line).containsExactly(-10.5, -7.5, -1.5, 1.5);
        AltLines.Line hard = lines.get(0);
        // Domicile −10,5 : bien plus dur que −4,5, donc mieux payé ; l'extérieur +10,5 l'inverse.
        assertThat(hard.firstOdds()).isGreaterThan(2.5);
        assertThat(hard.secondOdds()).isLessThan(1.6);
        AltLines.Line easy = lines.get(3);
        assertThat(easy.firstOdds()).isLessThan(1.6);

        assertThat(lines).filteredOn(l -> l.market().equals(AltLines.ALT_TOTAL))
                .extracting(AltLines.Line::line).containsExactly(216.5, 221.5, 231.5, 236.5);
        assertThat(lines).allSatisfy(l -> {
            assertThat(l.firstOdds()).isBetween(1.03, 15.0);
            assertThat(l.secondOdds()).isBetween(1.03, 15.0);
        });
    }

    @Test
    void ligne_entiere_jamais_d_egalite() {
        assertThat(AltLines.of(match(-4.0, 226.5))).filteredOn(l -> l.market().equals(AltLines.ALT_SPREAD))
                .extracting(AltLines.Line::line).containsExactly(-10.5, -7.5, -1.5, 1.5);
    }

    @Test
    void seule_une_ligne_proposee_se_retrouve() {
        Match m = match(-4.5, 226.5);
        assertThat(AltLines.find(m, AltLines.ALT_TOTAL, "under", 221.5)).isPresent();
        assertThat(AltLines.find(m, AltLines.ALT_TOTAL, "under", 222.5)).isEmpty();
        assertThat(AltLines.find(m, AltLines.ALT_SPREAD, "away", null)).isEmpty();
    }
}
