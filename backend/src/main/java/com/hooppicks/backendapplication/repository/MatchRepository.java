package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;


public interface MatchRepository extends JpaRepository<Match, String> {
    Optional<Match> findByExternalId(Long externalId);

    List<Match> findTop100ByOrderByDateDesc();

    // Bornées par Pageable plutôt qu'un findTopN fixe : le nombre à traiter
    // par tick de synchro doit rester ajustable sans recompiler (cf. l'OOM
    // Railway causé par un lot trop gros traité d'un coup).
    List<Match> findByEspnEventIdIsNull(Pageable pageable);

    @Query("""
        SELECT m FROM Match m
        WHERE m.status = :status AND m.espnEventId IS NOT NULL
        AND NOT EXISTS (SELECT 1 FROM PlayerMatchStat s WHERE s.match = m)
    """)
    List<Match> findFinishedWithoutBoxScore(MatchStatus status, Pageable pageable);
}