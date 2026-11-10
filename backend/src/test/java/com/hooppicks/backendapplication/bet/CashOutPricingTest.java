package com.hooppicks.backendapplication.bet;

import com.hooppicks.backendapplication.bet.CashOutPricing.Leg;
import com.hooppicks.backendapplication.bet.CashOutPricing.Live;
import com.hooppicks.backendapplication.bet.CashOutPricing.Quote;
import com.hooppicks.backendapplication.bet.LegEvaluator.LegResult;
import com.hooppicks.backendapplication.entity.BetSelection;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CashOutPricingTest {

    private static Match match(MatchStatus status) {
        Match m = new Match();
        m.setId("m");
        m.setStatus(status);
        m.setMoneylineHome(2.0);
        m.setMoneylineAway(1.9);
        m.setSpreadValue(-5.5);
        m.setSpreadOddsHome(1.96);
        m.setSpreadOddsAway(1.96);
        m.setTotalValue(220.5);
        m.setTotalOddsOver(1.96);
        m.setTotalOddsUnder(1.96);
        return m;
    }

    private static Match finished(int home, int away) {
        Match m = match(MatchStatus.FINISHED);
        m.setHomeScore(home);
        m.setAwayScore(away);
        return m;
    }

    private static BetSelection sel(String market, String outcome, double odds) {
        BetSelection s = new BetSelection();
        s.setMatchId("m");
        s.setMarket(market);
        s.setOutcome(outcome);
        s.setOdds(odds);
        return s;
    }

    private static Live live(int period, String clock, int home, int away, Double homeWin) {
        return new Live("in", period, clock, home, away, homeWin, Duration.ofSeconds(10));
    }

    private static Leg leg(BetSelection s, Match m, Live live) {
        return new Leg(s, m, live, Optional.empty());
    }

    @Test
    void encaisser_juste_apres_la_pose_rend_un_peu_moins_que_la_mise() {
        // 100 pts sur le vainqueur à 2,00 : 49 % de chances, moins 5 %.
        Quote q = CashOutPricing.price(200, List.of(leg(sel("moneyline", "home", 2.0), match(MatchStatus.SCHEDULED), null))).orElseThrow();
        assertThat(q.value()).isEqualTo(93);
        assertThat(q.available()).isTrue();
    }

    @Test
    void en_direct_le_vainqueur_suit_la_probabilite_d_espn() {
        Quote q = CashOutPricing.price(200, List.of(leg(sel("moneyline", "home", 2.0), match(MatchStatus.LIVE),
                live(4, "8:00", 105, 90, 0.9)))).orElseThrow();
        assertThat(q.value()).isEqualTo(171);
    }

    @Test
    void une_cote_boostee_ne_change_pas_la_probabilite_seulement_le_gain() {
        // Le ticket boosté a un gain plus gros ; la probabilité vient de la cote du match, pas de celle du ticket.
        Quote boosted = CashOutPricing.price(230, List.of(leg(sel("moneyline", "home", 2.3), match(MatchStatus.SCHEDULED), null))).orElseThrow();
        assertThat(boosted.value()).isEqualTo(107);
    }

    @Test
    void une_selection_perdue_et_plus_rien_a_encaisser() {
        List<Leg> legs = List.of(
                leg(sel("moneyline", "away", 1.9), finished(110, 100), null),
                leg(sel("moneyline", "home", 2.0), match(MatchStatus.SCHEDULED), null));
        assertThat(CashOutPricing.price(400, legs)).isEmpty();
    }

    @Test
    void tout_est_joue_le_ticket_va_etre_regle() {
        assertThat(CashOutPricing.price(200, List.of(leg(sel("moneyline", "home", 2.0), finished(110, 100), null)))).isEmpty();
    }

    @Test
    void une_selection_gagnee_compte_pour_un_et_une_remboursee_retire_sa_cote() {
        Match second = match(MatchStatus.SCHEDULED);
        second.setId("m2");
        BetSelection upcoming = sel("moneyline", "home", 2.0);
        upcoming.setMatchId("m2");
        Quote won = CashOutPricing.price(400, List.of(leg(sel("moneyline", "home", 2.0), finished(110, 100), null), leg(upcoming, second, null))).orElseThrow();
        assertThat(won.value()).isEqualTo(186); // 400 × 0,49 × 0,95

        BetSelection pushed = sel("total", "over", 2.0);
        Match exact = finished(110, 110);
        exact.setTotalValue(220);
        Quote push = CashOutPricing.price(400, List.of(leg(pushed, exact, null), leg(upcoming, second, null))).orElseThrow();
        assertThat(push.value()).isEqualTo(93); // la sélection remboursée ne compte plus : 200 × 0,49 × 0,95
    }

    @Test
    void fin_de_match_serree_suspend_l_encaissement() {
        Quote q = CashOutPricing.price(200, List.of(leg(sel("moneyline", "home", 2.0), match(MatchStatus.LIVE),
                live(4, "1:30", 102, 99, 0.7)))).orElseThrow();
        assertThat(q.available()).isFalse();
        assertThat(q.suspended()).isEqualTo(CashOutPricing.SUSPENDED_CLUTCH);

        Quote blowout = CashOutPricing.price(200, List.of(leg(sel("moneyline", "home", 2.0), match(MatchStatus.LIVE),
                live(4, "1:30", 120, 99, 0.999)))).orElseThrow();
        assertThat(blowout.available()).isTrue();
    }

    @Test
    void direct_trop_vieux_ou_absent_suspend() {
        Live old = new Live("in", 2, "5:00", 50, 48, 0.55, Duration.ofSeconds(90));
        assertThat(CashOutPricing.price(200, List.of(leg(sel("moneyline", "home", 2.0), match(MatchStatus.LIVE), old))).orElseThrow().suspended())
                .isEqualTo(CashOutPricing.SUSPENDED_LIVE);
        assertThat(CashOutPricing.price(200, List.of(leg(sel("moneyline", "home", 2.0), match(MatchStatus.LIVE), null))).orElseThrow().suspended())
                .isEqualTo(CashOutPricing.SUSPENDED_LIVE);
    }

    @Test
    void espn_dit_fini_mais_pas_encore_notre_synchro() {
        Live post = new Live("post", 4, "0:00", 110, 100, 1.0, Duration.ofSeconds(5));
        assertThat(CashOutPricing.price(200, List.of(leg(sel("moneyline", "home", 2.0), match(MatchStatus.LIVE), post))).orElseThrow().suspended())
                .isEqualTo(CashOutPricing.SUSPENDED_RESULT);
    }

    @Test
    void pari_joueur_avant_pendant_et_apres_son_match() {
        BetSelection prop = sel("player_points", "over", 1.96);
        prop.setPropLine(25.5);
        Quote before = CashOutPricing.price(196, List.of(leg(prop, match(MatchStatus.SCHEDULED), null))).orElseThrow();
        assertThat(before.value()).isEqualTo(93);

        Quote during = CashOutPricing.price(196, List.of(leg(prop, match(MatchStatus.LIVE), live(2, "4:00", 50, 50, 0.5)))).orElseThrow();
        assertThat(during.suspended()).isEqualTo(CashOutPricing.SUSPENDED_PROP);

        Match second = match(MatchStatus.SCHEDULED);
        second.setId("m2");
        BetSelection upcoming = sel("moneyline", "home", 2.0);
        upcoming.setMatchId("m2");
        Quote sheetMissing = CashOutPricing.price(392, List.of(new Leg(prop, finished(100, 90), null, Optional.empty()), leg(upcoming, second, null))).orElseThrow();
        assertThat(sheetMissing.suspended()).isEqualTo(CashOutPricing.SUSPENDED_RESULT);

        Quote passed = CashOutPricing.price(392, List.of(new Leg(prop, finished(100, 90), null, Optional.of(LegResult.WIN)), leg(upcoming, second, null))).orElseThrow();
        assertThat(passed.available()).isTrue();
        assertThat(passed.value()).isEqualTo(182); // 392 × 0,49 × 0,95

        assertThat(CashOutPricing.price(392, List.of(new Leg(prop, finished(100, 90), null, Optional.of(LegResult.LOSE)), leg(upcoming, second, null)))).isEmpty();
    }

    @Test
    void total_et_ecart_en_direct_suivent_le_score_et_le_temps_restant() {
        // 210 pts à 6 min de la fin, ligne 220,5 : « plus de » presque sûr, « moins de » presque perdu.
        Live late = live(4, "6:00", 110, 100, 0.8);
        Quote over = CashOutPricing.price(196, List.of(leg(sel("total", "over", 1.96), match(MatchStatus.LIVE), late))).orElseThrow();
        Quote under = CashOutPricing.price(196, List.of(leg(sel("total", "under", 1.96), match(MatchStatus.LIVE), late))).orElseThrow();
        assertThat(over.value()).isGreaterThan(180);
        assertThat(under.value()).isLessThan(5);

        // Domicile -5,5 qui mène de 10 à 6 min de la fin : environ 87 % de couvrir l'écart.
        Quote spread = CashOutPricing.price(196, List.of(leg(sel("spread", "home", 1.96), match(MatchStatus.LIVE), late))).orElseThrow();
        assertThat(spread.value()).isBetween(155, 170);
        Quote otherSide = CashOutPricing.price(196, List.of(leg(sel("spread", "away", 1.96), match(MatchStatus.LIVE), late))).orElseThrow();
        assertThat(otherSide.value()).isBetween(15, 35);

        // Au coup d'envoi (0-0, tout à jouer), on retrouve à peu près la valeur d'avant le match.
        Quote kickoff = CashOutPricing.price(196, List.of(leg(sel("total", "over", 1.96), match(MatchStatus.LIVE), live(1, "12:00", 0, 0, 0.6)))).orElseThrow();
        assertThat(kickoff.value()).isBetween(88, 96);
    }

    @Test
    void temps_restant_lu_dans_le_chrono() {
        assertThat(CashOutPricing.secondsLeft(1, "12:00")).isEqualTo(2880);
        assertThat(CashOutPricing.secondsLeft(4, "5:42")).isEqualTo(342);
        assertThat(CashOutPricing.secondsLeft(4, "42.3")).isEqualTo(42);
        assertThat(CashOutPricing.secondsLeft(5, "3:00")).isEqualTo(180);
        assertThat(CashOutPricing.secondsLeft(2, null)).isEqualTo(2 * 720);
    }
}
