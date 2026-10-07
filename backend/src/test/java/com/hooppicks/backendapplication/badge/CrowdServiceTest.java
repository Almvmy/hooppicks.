package com.hooppicks.backendapplication.badge;

import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.entity.BetSelection;
import com.hooppicks.backendapplication.entity.BetStatus;
import com.hooppicks.backendapplication.repository.BetSelectionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CrowdServiceTest {

    private final BetSelectionRepository repository = mock(BetSelectionRepository.class);
    private final CrowdService service = new CrowdService(repository);

    private List<Bet> wonOnAway() {
        BetSelection s = new BetSelection();
        s.setMatchId("m1");
        s.setMarket("moneyline");
        s.setOutcome("away");
        Bet b = new Bet();
        b.setStatus(BetStatus.WON);
        b.getSelections().add(s);
        return List.of(b);
    }

    @Test
    void gagner_avec_moins_de_20_pourcent_des_joueurs_debloque_le_badge() {
        when(repository.countGroupedByMatchMarketOutcome(any())).thenReturn(List.<Object[]>of(
                new Object[]{"m1", "moneyline", "home", 9L}, new Object[]{"m1", "moneyline", "away", 1L}));
        assertThat(service.wonAgainstCrowd(wonOnAway())).isTrue();
    }

    @Test
    void trop_peu_de_pronostics_ou_choix_partage_ne_comptent_pas() {
        when(repository.countGroupedByMatchMarketOutcome(any())).thenReturn(List.<Object[]>of(
                new Object[]{"m1", "moneyline", "home", 3L}, new Object[]{"m1", "moneyline", "away", 1L}));
        assertThat(service.wonAgainstCrowd(wonOnAway())).isFalse();

        when(repository.countGroupedByMatchMarketOutcome(any())).thenReturn(List.<Object[]>of(
                new Object[]{"m1", "moneyline", "home", 6L}, new Object[]{"m1", "moneyline", "away", 4L}));
        assertThat(service.wonAgainstCrowd(wonOnAway())).isFalse();
    }
}
