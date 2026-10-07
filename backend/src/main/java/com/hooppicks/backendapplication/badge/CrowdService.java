package com.hooppicks.backendapplication.badge;

import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.entity.BetSelection;
import com.hooppicks.backendapplication.entity.BetStatus;
import com.hooppicks.backendapplication.repository.BetSelectionRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * « Contre la foule » : une sélection gagnante que moins de 20 % des joueurs
 * avaient choisie sur ce pari (même match, même marché). Il faut au moins
 * 5 pronostics sur le pari, sinon 1 contre 1 suffirait. Mêmes comptes que
 * la répartition affichée sous les cotes (PickPercentagesService).
 */
@Service
public class CrowdService {

    static final int MIN_PICKS = 5;
    static final int MAX_SHARE_PERCENT = 20;

    private final BetSelectionRepository selectionRepository;

    public CrowdService(BetSelectionRepository selectionRepository) {
        this.selectionRepository = selectionRepository;
    }

    public boolean wonAgainstCrowd(List<Bet> bets) {
        List<BetSelection> won = bets.stream()
                .filter(b -> b.getStatus() == BetStatus.WON)
                .flatMap(b -> b.getSelections().stream())
                .toList();
        if (won.isEmpty()) return false;

        Map<String, Long> byOutcome = new HashMap<>();
        Map<String, Long> byMarket = new HashMap<>();
        for (Object[] row : selectionRepository.countGroupedByMatchMarketOutcome(
                won.stream().map(BetSelection::getMatchId).distinct().toList())) {
            String market = row[0] + "|" + row[1];
            long count = ((Number) row[3]).longValue();
            byOutcome.merge(market + "|" + row[2], count, Long::sum);
            byMarket.merge(market, count, Long::sum);
        }
        for (BetSelection s : won) {
            String market = s.getMatchId() + "|" + s.getMarket();
            long total = byMarket.getOrDefault(market, 0L);
            long mine = byOutcome.getOrDefault(market + "|" + s.getOutcome(), 0L);
            if (total >= MIN_PICKS && mine * 100 < MAX_SHARE_PERCENT * total) return true;
        }
        return false;
    }
}
