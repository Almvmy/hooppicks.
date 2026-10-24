package com.hooppicks.backendapplication.bet;

import com.hooppicks.backendapplication.entity.BetSelection;
import com.hooppicks.backendapplication.entity.Match;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LegEvaluatorAltLinesTest {

    private static Match finished(int home, int away) {
        Match m = new Match();
        m.setHomeScore(home);
        m.setAwayScore(away);
        // Lignes principales différentes : la ligne figée sur la sélection seule compte.
        m.setSpreadValue(-4.5);
        m.setTotalValue(226.5);
        return m;
    }

    private static BetSelection alt(String market, String outcome, double line) {
        BetSelection s = new BetSelection();
        s.setMarket(market);
        s.setOutcome(outcome);
        s.setPropLine(line);
        return s;
    }

    @Test
    void handicap_alternatif_regle_sur_sa_propre_ligne() {
        // Domicile gagne de 9 : couvre −7,5 mais pas −10,5.
        Match m = finished(118, 109);
        assertThat(LegEvaluator.evaluate(alt("alt_spread", "home", -7.5), m)).isEqualTo(LegEvaluator.LegResult.WIN);
        assertThat(LegEvaluator.evaluate(alt("alt_spread", "home", -10.5), m)).isEqualTo(LegEvaluator.LegResult.LOSE);
        assertThat(LegEvaluator.evaluate(alt("alt_spread", "away", -10.5), m)).isEqualTo(LegEvaluator.LegResult.WIN);
    }

    @Test
    void total_alternatif_regle_sur_sa_propre_ligne() {
        Match m = finished(118, 109); // 227
        assertThat(LegEvaluator.evaluate(alt("alt_total", "over", 221.5), m)).isEqualTo(LegEvaluator.LegResult.WIN);
        assertThat(LegEvaluator.evaluate(alt("alt_total", "over", 231.5), m)).isEqualTo(LegEvaluator.LegResult.LOSE);
        assertThat(LegEvaluator.evaluate(alt("alt_total", "under", 231.5), m)).isEqualTo(LegEvaluator.LegResult.WIN);
    }
}
