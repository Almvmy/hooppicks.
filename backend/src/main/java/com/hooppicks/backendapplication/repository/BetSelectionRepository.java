package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.BetSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BetSelectionRepository extends JpaRepository<BetSelection, String> {

    // matchId/market/outcome sont dénormalisés directement sur BetSelection
    // (pas besoin de joindre Bet ni Match) : cf. le comparatif communautaire
    // affiché sur les cartes de match (PickPercentagesService).
    @Query("""
        SELECT s.matchId, s.market, s.outcome, COUNT(s)
        FROM BetSelection s
        WHERE s.matchId IN :matchIds
        GROUP BY s.matchId, s.market, s.outcome
    """)
    List<Object[]> countGroupedByMatchMarketOutcome(@Param("matchIds") List<String> matchIds);
}
