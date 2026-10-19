package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.entity.BetStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BetRepository extends JpaRepository<Bet, String> {
    List<Bet> findByUserIdOrderByPlacedAtDesc(String userId);

    // Les tickets de présaison ne comptent pas au classement (matchs de
    // rodage, titulaires peu utilisés), à partir de la semaine du 19 octobre
    // 2026 : la semaine du 12, la première officielle, a été jouée sur la
    // présaison et garde ses points et son champion. Un ticket compte s'il
    // n'a aucune sélection sur un match de présaison. Repris par toutes les
    // requêtes de points (classement, duels, palmarès, récap de la semaine).
    String COUNTED = """
          AND (date_trunc('week', (b.placed_at AT TIME ZONE 'UTC') - INTERVAL '12 hours') < TIMESTAMP '2026-10-19'
               OR NOT EXISTS (SELECT 1 FROM bet_selection bs JOIN match m ON m.id = bs.match_id
                              WHERE bs.bet_id = b.id AND m.type = 'PRESEASON'))
    """;

    // Classement : chaque semaine de jeu (lundi 12h GMT) vaut son bénéfice net
    // (gain − mise si gagné, − mise si perdu, remboursé : 0), ramené à 0
    // s'il est négatif ; le classement additionne ces semaines. Un ticket
    // perdu pèse donc sur sa semaine, mais une mauvaise semaine ne fait
    // jamais perdre les points des autres. Chaque pari compte pour la semaine
    // où il a été posé. En SQL natif (Postgres) : regroupement par semaine
    // décalée de 12 h, puis somme par joueur, en une seule requête.
    String WEEKLY_SCORES_ALL = """
        SELECT b.user_id, COUNT(*) AS total,
               SUM(CASE WHEN b.status = 'WON' THEN 1 ELSE 0 END) AS won,
               GREATEST(0, SUM(CASE WHEN b.status = 'WON' THEN b.potential_payout - b.stake ELSE 0 - b.stake END)) AS score
        FROM bet b
        WHERE b.status IN ('WON', 'LOST')
    """ + COUNTED + """
        GROUP BY b.user_id, date_trunc('week', (b.placed_at AT TIME ZONE 'UTC') - INTERVAL '12 hours')
        HAVING date_trunc('week', (b.placed_at AT TIME ZONE 'UTC') - INTERVAL '12 hours') >= :fromWeek
    """;
    String WEEKLY_SCORES_MEMBERS = """
        SELECT b.user_id, COUNT(*) AS total,
               SUM(CASE WHEN b.status = 'WON' THEN 1 ELSE 0 END) AS won,
               GREATEST(0, SUM(CASE WHEN b.status = 'WON' THEN b.potential_payout - b.stake ELSE 0 - b.stake END)) AS score
        FROM bet b
        WHERE b.status IN ('WON', 'LOST') AND b.user_id IN (:memberIds)
    """ + COUNTED + """
        GROUP BY b.user_id, date_trunc('week', (b.placed_at AT TIME ZONE 'UTC') - INTERVAL '12 hours')
        HAVING date_trunc('week', (b.placed_at AT TIME ZONE 'UTC') - INTERVAL '12 hours') >= :fromWeek
    """;
    String LEADERBOARD_OUTER_START = """
        SELECT u.id, u.username, CAST(SUM(w.score) AS bigint) AS points,
               CAST(SUM(w.total) AS bigint) AS total_bets, CAST(SUM(w.won) AS bigint) AS won_bets,
               u.avatar_number, u.avatar_position, u.avatar_colorway, u.avatar_icon, u.favorite_team
        FROM (
    """;
    String LEADERBOARD_OUTER_END = """
        ) w JOIN app_user u ON u.id = w.user_id
        GROUP BY u.id, u.username, u.avatar_number, u.avatar_position, u.avatar_colorway, u.avatar_icon, u.favorite_team
        ORDER BY points DESC, u.username
    """;

    @org.springframework.data.jpa.repository.Query(nativeQuery = true,
            value = LEADERBOARD_OUTER_START + "SELECT * FROM (" + WEEKLY_SCORES_ALL + ") s" + LEADERBOARD_OUTER_END)
    List<Object[]> getWeeklyScoreLeaderboard(java.time.LocalDateTime fromWeek);

    @org.springframework.data.jpa.repository.Query(nativeQuery = true,
            value = LEADERBOARD_OUTER_START + "SELECT * FROM (" + WEEKLY_SCORES_MEMBERS + ") s" + LEADERBOARD_OUTER_END)
    List<Object[]> getWeeklyScoreLeaderboardForUsers(java.time.LocalDateTime fromWeek, List<String> memberIds);

    /** Classement de la saison : toutes les semaines. */
    default List<Object[]> getLeaderboardRaw() {
        return getWeeklyScoreLeaderboard(java.time.LocalDateTime.of(1970, 1, 1, 0, 0));
    }

    /**
     * Classement depuis `since` (semaine : lundi 12h ; mois : le 1er à minuit,
     * GMT) : semaines de jeu dont le lundi tombe à partir de cette date.
     * Les semaines sont repérées par leur lundi à minuit, décalé de 12 h.
     */
    default List<Object[]> getLeaderboardRawSince(java.time.Instant since) {
        return getWeeklyScoreLeaderboard(java.time.LocalDateTime.ofInstant(since.minusSeconds(12 * 3600), java.time.ZoneOffset.UTC));
    }

    /** Classement d'une ligue : saison, limité à ses membres. */
    default List<Object[]> getLeaderboardRawForUsers(List<String> memberIds) {
        return getWeeklyScoreLeaderboardForUsers(java.time.LocalDateTime.of(1970, 1, 1, 0, 0), memberIds);
    }

    /**
     * Points d'une semaine de jeu précise (même règle que le classement :
     * bilan de la semaine, jamais moins de 0) pour quelques joueurs. `week` =
     * lundi de la semaine à minuit, comme le regroupement décalé de 12 h.
     * Un joueur sans ticket réglé cette semaine n'a pas de ligne (= 0).
     */
    @org.springframework.data.jpa.repository.Query(nativeQuery = true, value = """
        SELECT b.user_id,
               CAST(GREATEST(0, SUM(CASE WHEN b.status = 'WON' THEN b.potential_payout - b.stake ELSE 0 - b.stake END)) AS bigint)
        FROM bet b
        WHERE b.status IN ('WON', 'LOST') AND b.user_id IN (:userIds)
          AND date_trunc('week', (b.placed_at AT TIME ZONE 'UTC') - INTERVAL '12 hours') = :week
    """ + COUNTED + """
        GROUP BY b.user_id
    """)
    List<Object[]> getWeekScores(java.time.LocalDateTime week, List<String> userIds);

    /** Points de tous les joueurs ayant un ticket réglé cette semaine-là (palmarès). */
    @org.springframework.data.jpa.repository.Query(nativeQuery = true, value = """
        SELECT b.user_id,
               CAST(GREATEST(0, SUM(CASE WHEN b.status = 'WON' THEN b.potential_payout - b.stake ELSE 0 - b.stake END)) AS bigint)
        FROM bet b
        WHERE b.status IN ('WON', 'LOST')
          AND date_trunc('week', (b.placed_at AT TIME ZONE 'UTC') - INTERVAL '12 hours') = :week
    """ + COUNTED + """
        GROUP BY b.user_id
    """)
    List<Object[]> getAllWeekScores(java.time.LocalDateTime week);

    /** Semaines de jeu (lundi à minuit) ayant au moins un ticket réglé. */
    @org.springframework.data.jpa.repository.Query(nativeQuery = true, value = """
        SELECT DISTINCT date_trunc('week', (b.placed_at AT TIME ZONE 'UTC') - INTERVAL '12 hours')
        FROM bet b WHERE b.status IN ('WON', 'LOST')
    """ + COUNTED)
    List<Object> findSettledWeeks();

    /** Tickets encore en attente posés pendant cette période, tous joueurs. */
    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(b) FROM Bet b
        WHERE b.status = com.hooppicks.backendapplication.entity.BetStatus.PENDING
          AND b.placedAt >= :from AND b.placedAt < :to
    """)
    long countAllPendingPlacedBetween(java.time.Instant from, java.time.Instant to);

    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(b) FROM Bet b
        WHERE b.user.id IN :userIds AND b.status = com.hooppicks.backendapplication.entity.BetStatus.PENDING
          AND b.placedAt >= :from AND b.placedAt < :to
    """)
    long countPendingPlacedBetween(List<String> userIds, java.time.Instant from, java.time.Instant to);

    /** Classement d'une ligue depuis `since` (même règle que getLeaderboardRawSince). */
    default List<Object[]> getLeaderboardRawForUsersSince(java.time.Instant since, List<String> memberIds) {
        return getWeeklyScoreLeaderboardForUsers(java.time.LocalDateTime.ofInstant(since.minusSeconds(12 * 3600), java.time.ZoneOffset.UTC), memberIds);
    }

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
    """ + COUNTED + """
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

    // [nombre de tickets résolus, bénéfice net] des paris posés entre from et
    // to, comptés comme au classement (récap de la semaine).
    @org.springframework.data.jpa.repository.Query(nativeQuery = true, value = """
        SELECT COUNT(*), SUM(CASE WHEN b.status = 'WON' THEN b.potential_payout - b.stake ELSE 0 - b.stake END)
        FROM bet b
        WHERE b.user_id = :userId AND b.status IN ('WON', 'LOST')
          AND b.placed_at >= :from AND b.placed_at < :to
    """ + COUNTED)
    List<Object[]> getNetResultBetween(String userId, java.time.Instant from, java.time.Instant to);
}
