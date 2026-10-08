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

    List<RosterPlayer> findTop5ByStealsPerGameIsNotNullOrderByStealsPerGameDesc();

    List<RosterPlayer> findTop5ByBlocksPerGameIsNotNullOrderByBlocksPerGameDesc();

    // Rapport des blessures (cf. PlayerController.injuries) : statut renseigné
    // par la synchro des effectifs ESPN, null = joueur disponible.
    List<RosterPlayer> findByInjuryStatusIsNotNullOrderByLastNameAsc();

    // Jamais synchronisé (null) en premier, puis le plus ancien synchronisé :
    // fait tourner un rafraîchissement continu sur l'ensemble de l'effectif
    // au fil des passages du batch (cf. EspnPlayerStatsService). Entre les
    // deux, ceux à qui il manque une moyenne ajoutée après coup (tirs à 3
    // points), meilleurs marqueurs d'abord : ce sont eux qui ont des paris
    // joueurs, ils les ont en une heure plutôt qu'au bout d'un tour complet.
    @Query("""
            SELECT r FROM RosterPlayer r ORDER BY
              CASE WHEN r.statsUpdatedAt IS NULL THEN 0 WHEN r.threePointersMadePerGame IS NULL THEN 1 ELSE 2 END,
              CASE WHEN r.threePointersMadePerGame IS NULL THEN r.pointsPerGame END DESC NULLS LAST,
              r.statsUpdatedAt ASC
            """)
    List<RosterPlayer> findAllOrderByStatsUpdatedAtAscNullsFirst(Pageable pageable);

    long countByStatsUpdatedAtIsNull();
}
