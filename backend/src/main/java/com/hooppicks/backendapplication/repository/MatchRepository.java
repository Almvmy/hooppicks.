package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.entity.Team;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;


public interface MatchRepository extends JpaRepository<Match, String> {
    Optional<Match> findByExternalId(Long externalId);

    List<Match> findTop100ByOrderByDateDesc();

    long countByStatus(MatchStatus status);

    // Bornées par Pageable plutôt qu'un findTopN fixe : le nombre à traiter
    // par tick de synchro doit rester ajustable sans recompiler (cf. l'OOM
    // Railway causé par un lot trop gros traité d'un coup).
    List<Match> findByTypeIsNullOrderByDateDesc(Pageable pageable);

    Optional<Match> findFirstByEspnEventId(String espnEventId);

    List<Match> findByStatusAndDateBetween(MatchStatus status, Instant from, Instant to);

    Optional<Match> findFirstByHomeTeamAndAwayTeamAndDateBetween(Team homeTeam, Team awayTeam, Instant from, Instant to);

    @Query("""
        SELECT m FROM Match m
        WHERE m.status = :status AND m.espnEventId IS NOT NULL
        AND NOT EXISTS (SELECT 1 FROM PlayerMatchStat s WHERE s.match = m)
    """)
    List<Match> findFinishedWithoutBoxScore(MatchStatus status, Pageable pageable);

    // Liste des matchs de la console admin : filtres appliqués en base, sur
    // tous les matchs. Filtrer après un « top 100 par date » ratait les matchs
    // passés dès que le calendrier à venir dépassait 100 matchs : justement
    // ceux qu'on corrige. q = motif LIKE déjà en minuscules ("%nyk%").
    @Query("""
        SELECT m FROM Match m
        WHERE (:status IS NULL OR m.status = :status)
          AND (:q IS NULL
               OR LOWER(m.homeTeam.name) LIKE :q OR LOWER(m.awayTeam.name) LIKE :q
               OR LOWER(m.homeTeam.abbreviation) LIKE :q OR LOWER(m.awayTeam.abbreviation) LIKE :q)
        ORDER BY m.date DESC
    """)
    List<Match> searchForAdmin(MatchStatus status, String q, Pageable pageable);

    // --- Vue d'ensemble de la console admin ---
    long countByAdminLockedTrue();

    long countByEspnEventIdIsNull();

    @Query("SELECT m.type, COUNT(m) FROM Match m GROUP BY m.type")
    List<Object[]> countByType();

    // Derniers matchs terminés d'une équipe (ligne des totaux, OddsService).
    @Query("""
        SELECT m FROM Match m
        WHERE (m.homeTeam = :team OR m.awayTeam = :team)
          AND m.status = com.hooppicks.backendapplication.entity.MatchStatus.FINISHED
          AND m.homeScore IS NOT NULL AND m.awayScore IS NOT NULL
          AND (m.type IS NULL OR m.type <> :excluded)
        ORDER BY m.date DESC
    """)
    List<Match> findRecentFinishedForTeam(Team team, com.hooppicks.backendapplication.entity.MatchType excluded, Pageable pageable);

    // Premier match de saison régulière : fin des pronostics de saison (SeasonPickService).
    java.util.Optional<Match> findFirstByTypeAndDateAfterOrderByDateAsc(com.hooppicks.backendapplication.entity.MatchType type, java.time.Instant after);
}
