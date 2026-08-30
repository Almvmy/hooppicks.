package com.hooppicks.backendapplication.bet;

import com.hooppicks.backendapplication.dto.PickPercentagesDto;
import com.hooppicks.backendapplication.repository.BetSelectionRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * "X% des joueurs ont misé sur..." : calculé à partir des BetSelection déjà
 * en base, en un seul aller-retour groupé pour toute une liste de matchs
 * plutôt qu'une requête par match (évite le N+1 sur la page /matches).
 */
@Service
public class PickPercentagesService {

    private final BetSelectionRepository betSelectionRepository;

    public PickPercentagesService(BetSelectionRepository betSelectionRepository) {
        this.betSelectionRepository = betSelectionRepository;
    }

    public Map<String, PickPercentagesDto> forMatches(List<String> matchIds) {
        Map<String, PickPercentagesDto> result = new HashMap<>();
        if (matchIds.isEmpty()) return result;

        // matchId -> market -> outcome -> nombre de sélections
        Map<String, Map<String, Map<String, Long>>> counts = new HashMap<>();
        for (Object[] row : betSelectionRepository.countGroupedByMatchMarketOutcome(matchIds)) {
            String matchId = (String) row[0];
            String market = (String) row[1];
            String outcome = (String) row[2];
            Long count = (Long) row[3];
            counts.computeIfAbsent(matchId, k -> new HashMap<>())
                    .computeIfAbsent(market, k -> new HashMap<>())
                    .put(outcome, count);
        }

        for (String matchId : matchIds) {
            Map<String, Map<String, Long>> byMarket = counts.getOrDefault(matchId, Map.of());
            result.put(matchId, new PickPercentagesDto(
                    percentage(byMarket, "moneyline", "home"),
                    percentage(byMarket, "moneyline", "away"),
                    percentage(byMarket, "spread", "home"),
                    percentage(byMarket, "spread", "away"),
                    percentage(byMarket, "total", "over"),
                    percentage(byMarket, "total", "under")
            ));
        }
        return result;
    }

    private Integer percentage(Map<String, Map<String, Long>> byMarket, String market, String outcome) {
        Map<String, Long> outcomes = byMarket.get(market);
        if (outcomes == null) return null; // personne n'a encore parié sur ce marché

        long total = outcomes.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) return null;

        long count = outcomes.getOrDefault(outcome, 0L);
        return (int) Math.round(count * 100.0 / total);
    }
}
