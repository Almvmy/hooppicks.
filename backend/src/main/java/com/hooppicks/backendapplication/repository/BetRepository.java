package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.entity.BetStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BetRepository extends JpaRepository<Bet, String> {
    List<Bet> findByUserIdOrderByPlacedAtDesc(String userId);

    // Classement : bénéfice net des tickets résolus (gain moins mise si gagné,
    // moins la mise si perdu ; un ticket remboursé ne compte pas). Avant, on
    // additionnait les gains bruts sans jamais retirer les pertes : beaucoup de
    // petits paris sur des favoris passaient devant des joueurs gagnants.
    @org.springframework.data.jpa.repository.Query("""
        SELECT b.user.id as userId, b.user.username as username,
               SUM(CASE WHEN b.status = 'WON' THEN b.potentialPayout - b.stake ELSE 0 - b.stake END) as points,
               COUNT(b) as totalBets,
               SUM(CASE WHEN b.status = 'WON' THEN 1 ELSE 0 END) as wonBets,
               b.user.avatarNumber as avatarNumber, b.user.avatarPosition as avatarPosition,
               b.user.avatarColorway as avatarColorway, b.user.avatarIcon as avatarIcon,
               b.user.favoriteTeam as favoriteTeam
        FROM Bet b
        WHERE b.status IN ('WON', 'LOST')
        GROUP BY b.user.id, b.user.username, b.user.avatarNumber, b.user.avatarPosition,
                 b.user.avatarColorway, b.user.avatarIcon, b.user.favoriteTeam
        ORDER BY points DESC
    """)
    List<Object[]> getLeaderboardRaw();

    // Même calcul que getLeaderboardRaw, limité aux tickets POSÉS depuis
    // `since` (classements de la semaine / du mois) : un pari compte pour la
    // semaine de jeu où il a été joué, avec le solde de cette semaine-là,
    // même s'il se règle après le lundi suivant.
    @org.springframework.data.jpa.repository.Query("""
        SELECT b.user.id as userId, b.user.username as username,
               SUM(CASE WHEN b.status = 'WON' THEN b.potentialPayout - b.stake ELSE 0 - b.stake END) as points,
               COUNT(b) as totalBets,
               SUM(CASE WHEN b.status = 'WON' THEN 1 ELSE 0 END) as wonBets,
               b.user.avatarNumber as avatarNumber, b.user.avatarPosition as avatarPosition,
               b.user.avatarColorway as avatarColorway, b.user.avatarIcon as avatarIcon,
               b.user.favoriteTeam as favoriteTeam
        FROM Bet b
        WHERE b.status IN ('WON', 'LOST') AND b.placedAt >= :since
        GROUP BY b.user.id, b.user.username, b.user.avatarNumber, b.user.avatarPosition,
                 b.user.avatarColorway, b.user.avatarIcon, b.user.favoriteTeam
        ORDER BY points DESC
    """)
    List<Object[]> getLeaderboardRawSince(java.time.Instant since);

    // Les 10 derniers tickets résolus (gagnés/perdus) de chaque joueur, du
    // plus récent au plus ancien : forme récente et série en cours. En SQL
    // natif pour la fonction de fenêtre : une seule requête pour tout le
    // classement plutôt qu'une par joueur.
    @org.springframework.data.jpa.repository.Query(nativeQuery = true, value = """
        SELECT user_id, status FROM (
            SELECT b.user_id, b.status,
                   ROW_NUMBER() OVER (PARTITION BY b.user_id
                                      ORDER BY b.resolved_at DESC NULLS LAST, b.placed_at DESC) AS rn
            FROM bet b
            WHERE b.status IN ('WON', 'LOST')
        ) recent
        WHERE rn <= 10
        ORDER BY user_id, rn
    """)
    List<Object[]> getRecentResultsPerUser();

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
        SELECT DISTINCT b
        FROM Bet b JOIN b.selections s
        WHERE s.matchId = :matchId AND b.status = 'PENDING'
    """)
    List<Bet> findPendingBetsForMatch(String matchId);

    // Paris par match et par statut, pour la liste des matchs de la console
    // admin : une seule requête pour les 100 matchs affichés.
    @org.springframework.data.jpa.repository.Query("""
        SELECT s.matchId, b.status, COUNT(DISTINCT b)
        FROM Bet b JOIN b.selections s
        WHERE s.matchId IN :matchIds
        GROUP BY s.matchId, b.status
    """)
    List<Object[]> countBetsByMatchAndStatus(List<String> matchIds);

    @org.springframework.data.jpa.repository.Query("""
        SELECT b.user.id, COUNT(b) FROM Bet b WHERE b.user.id IN :userIds GROUP BY b.user.id
    """)
    List<Object[]> countBetsByUser(List<String> userIds);

    @org.springframework.data.jpa.repository.Query("""
        SELECT b.user.id as userId, b.user.username as username,
               SUM(CASE WHEN b.status = 'WON' THEN b.potentialPayout - b.stake ELSE 0 - b.stake END) as points,
               COUNT(b) as totalBets,
               SUM(CASE WHEN b.status = 'WON' THEN 1 ELSE 0 END) as wonBets,
               b.user.avatarNumber as avatarNumber, b.user.avatarPosition as avatarPosition,
               b.user.avatarColorway as avatarColorway, b.user.avatarIcon as avatarIcon,
               b.user.favoriteTeam as favoriteTeam
        FROM Bet b
        WHERE b.status IN ('WON', 'LOST') AND b.user.id IN :memberIds
        GROUP BY b.user.id, b.user.username, b.user.avatarNumber, b.user.avatarPosition,
                 b.user.avatarColorway, b.user.avatarIcon, b.user.favoriteTeam
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

    List<Bet> findTop10ByUser_IdInAndStatusOrderByPlacedAtDesc(List<String> userIds, BetStatus status);

    void deleteByUserId(String userId);

    // --- Vue d'ensemble de la console admin ---
    long countByPlacedAtAfter(java.time.Instant since);

    long countByStatus(BetStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT b.user.id) FROM Bet b WHERE b.placedAt >= :since")
    long countDistinctBettorsSince(java.time.Instant since);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(b.stake), 0) FROM Bet b WHERE b.placedAt >= :since")
    long sumStakesSince(java.time.Instant since);

    @org.springframework.data.jpa.repository.Query("SELECT b.placedAt FROM Bet b WHERE b.placedAt >= :since")
    List<java.time.Instant> findPlacedAtSince(java.time.Instant since);

    // --- Semaines de jeu (BankrollService) ---
    @org.springframework.data.jpa.repository.Query("""
        SELECT COALESCE(SUM(b.stake), 0) FROM Bet b
        WHERE b.user.id = :userId AND b.status = 'PENDING' AND b.placedAt >= :since
    """)
    long sumPendingStakesSince(String userId, java.time.Instant since);

    // [nombre de tickets résolus, bénéfice net] des paris posés entre from et to.
    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(b), SUM(CASE WHEN b.status = 'WON' THEN b.potentialPayout - b.stake ELSE 0 - b.stake END)
        FROM Bet b
        WHERE b.user.id = :userId AND b.status IN ('WON', 'LOST')
          AND b.placedAt >= :from AND b.placedAt < :to
    """)
    List<Object[]> getNetResultBetween(String userId, java.time.Instant from, java.time.Instant to);
}
