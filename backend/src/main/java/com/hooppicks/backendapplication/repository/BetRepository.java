package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.entity.BetStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BetRepository extends JpaRepository<Bet, String> {
    List<Bet> findByUserIdOrderByPlacedAtDesc(String userId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT b.user.id as userId, b.user.username as username,
               SUM(CASE WHEN b.status = 'WON' THEN b.potentialPayout ELSE 0 END) as points,
               COUNT(b) as totalBets,
               SUM(CASE WHEN b.status = 'WON' THEN 1 ELSE 0 END) as wonBets
        FROM Bet b
        WHERE b.status IN ('WON', 'LOST')
        GROUP BY b.user.id, b.user.username
        ORDER BY points DESC
    """)
    List<Object[]> getLeaderboardRaw();

    List<Bet> findByStatus(BetStatus status);

    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(s) > 0
        FROM Bet b JOIN b.selections s
        WHERE s.matchId = :matchId AND b.status = 'PENDING'
    """)
    boolean existsPendingBetForMatch(String matchId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT DISTINCT b.user
        FROM Bet b JOIN b.selections s
        WHERE s.matchId = :matchId AND b.status = 'PENDING'
    """)
    List<com.hooppicks.backendapplication.entity.User> findUsersWithPendingBetOnMatch(String matchId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT b.user.id as userId, b.user.username as username,
               SUM(CASE WHEN b.status = 'WON' THEN b.potentialPayout ELSE 0 END) as points,
               COUNT(b) as totalBets,
               SUM(CASE WHEN b.status = 'WON' THEN 1 ELSE 0 END) as wonBets
        FROM Bet b
        WHERE b.status IN ('WON', 'LOST') AND b.user.id IN :memberIds
        GROUP BY b.user.id, b.user.username
        ORDER BY points DESC
    """)
    List<Object[]> getLeaderboardRawForUsers(List<String> memberIds);

    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(b), SUM(CASE WHEN b.status = 'WON' THEN 1 ELSE 0 END)
        FROM Bet b
        WHERE b.user.id = :userId AND b.status IN ('WON', 'LOST')
    """)
    List<Object[]> getUserStats(String userId);

    List<Bet> findTop10ByUser_IdInAndStatusOrderByResolvedAtDesc(List<String> userIds, BetStatus status);
}
