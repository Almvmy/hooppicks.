package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.PlayerMatchStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerMatchStatRepository extends JpaRepository<PlayerMatchStat, String> {
    List<PlayerMatchStat> findByMatchIdOrderByPointsDesc(String matchId);

    /**
     * Totaux de la saison par joueur (nom tel qu'écrit sur la feuille), matchs
     * terminés depuis `from`, hors présaison, play-in, playoffs et All-Star,
     * seulement les matchs où il a joué. Minutes en texte (« 27 », « -- ») :
     * la partie entière est lue en SQL. Ordre des colonnes : nom, matchs,
     * titularisations, minutes, points, rebonds, passes, interceptions,
     * contres, balles perdues, tirs réussis/tentés, 3 pts réussis/tentés,
     * lancers réussis/tentés.
     */
    @org.springframework.data.jpa.repository.Query(nativeQuery = true, value = """
        SELECT s.player_name, COUNT(*), SUM(CASE WHEN s.starter THEN 1 ELSE 0 END),
               SUM(CAST(NULLIF(regexp_replace(s.minutes, '[^0-9].*$', ''), '') AS integer)),
               SUM(s.points), SUM(s.rebounds), SUM(s.assists), SUM(s.steals), SUM(s.blocks), SUM(s.turnovers),
               SUM(s.field_goals_made), SUM(s.field_goals_attempted), SUM(s.three_points_made), SUM(s.three_points_attempted),
               SUM(s.free_throws_made), SUM(s.free_throws_attempted)
        FROM player_match_stat s JOIN match m ON m.id = s.match_id
        WHERE m.date >= :from AND m.status = 'FINISHED'
          AND COALESCE(m.type, 'REGULAR') IN ('REGULAR', 'NBA_CUP')
          AND COALESCE(CAST(NULLIF(regexp_replace(s.minutes, '[^0-9].*$', ''), '') AS integer), 0) > 0
        GROUP BY s.player_name
    """)
    List<Object[]> seasonTotals(java.time.Instant from);

    /** Feuilles de match des matchs terminés entre deux instants (les perfs de la nuit). */
    @org.springframework.data.jpa.repository.Query("""
        SELECT s FROM PlayerMatchStat s JOIN FETCH s.match m
        WHERE m.date >= :from AND m.date < :to AND m.status = com.hooppicks.backendapplication.entity.MatchStatus.FINISHED
    """)
    List<PlayerMatchStat> findFinishedBetween(java.time.Instant from, java.time.Instant to);
}
