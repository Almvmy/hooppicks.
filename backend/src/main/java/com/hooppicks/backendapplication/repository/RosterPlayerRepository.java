package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.RosterPlayer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RosterPlayerRepository extends JpaRepository<RosterPlayer, String> {
    List<RosterPlayer> findByTeamIdOrderByLastNameAsc(String teamId);

    List<RosterPlayer> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrderByLastNameAsc(
            String firstName, String lastName);

    // Meneurs statistiques actuels : affichés par défaut sur la page joueurs,
    // avant toute recherche (cf. PlayerController.leaders).
    List<RosterPlayer> findTop5ByPointsPerGameIsNotNullOrderByPointsPerGameDesc();

    List<RosterPlayer> findTop5ByReboundsPerGameIsNotNullOrderByReboundsPerGameDesc();

    List<RosterPlayer> findTop5ByAssistsPerGameIsNotNullOrderByAssistsPerGameDesc();

    // Jamais synchronisé (null) en premier, puis le plus ancien synchronisé :
    // fait tourner un rafraîchissement continu sur l'ensemble de l'effectif
    // au fil des passages du batch (cf. EspnPlayerStatsService).
    @Query("SELECT r FROM RosterPlayer r ORDER BY r.statsUpdatedAt ASC NULLS FIRST")
    List<RosterPlayer> findAllOrderByStatsUpdatedAtAscNullsFirst(Pageable pageable);
}
