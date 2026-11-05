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
        SELECT s FROM BetSelection s JOIN FETCH s.bet b JOIN FETCH b.user
        WHERE s.matchId = :matchId AND b.user.id IN :userIds
          AND b.status <> com.hooppicks.backendapplication.entity.BetStatus.VOID
        ORDER BY b.placedAt DESC
    """)
    List<com.hooppicks.backendapplication.entity.BetSelection> findFriendsOnMatch(@Param("matchId") String matchId, @Param("userIds") java.util.Collection<String> userIds);

    @Query("""
        SELECT s.matchId, s.market, s.outcome, COUNT(s)
        FROM BetSelection s
        WHERE s.matchId IN :matchIds
        GROUP BY s.matchId, s.market, s.outcome
    """)
    List<Object[]> countGroupedByMatchMarketOutcome(@Param("matchIds") List<String> matchIds);

    /** Paris joueurs : nombre de sélections par match, marché, joueur et côté (plus/moins). */
    @Query("""
        SELECT s.matchId, s.market, s.playerId, s.outcome, COUNT(s)
        FROM BetSelection s
        WHERE s.matchId IN :matchIds AND s.playerId IS NOT NULL
        GROUP BY s.matchId, s.market, s.playerId, s.outcome
    """)
    List<Object[]> countPropPicks(@Param("matchIds") List<String> matchIds);
}
