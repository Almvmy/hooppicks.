package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.Duel;
import com.hooppicks.backendapplication.entity.DuelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface DuelRepository extends JpaRepository<Duel, String> {

    @Query("""
        SELECT d FROM Duel d
        WHERE d.challenger.id = :userId OR d.opponent.id = :userId
        ORDER BY d.createdAt DESC
    """)
    List<Duel> findAllForUser(String userId);

    /** Duel déjà lancé ou en cours entre ces deux joueurs cette semaine, dans un sens ou dans l'autre. */
    @Query("""
        SELECT COUNT(d) > 0 FROM Duel d
        WHERE d.week = :week AND d.status IN :statuses
          AND ((d.challenger.id = :a AND d.opponent.id = :b) OR (d.challenger.id = :b AND d.opponent.id = :a))
    """)
    boolean existsBetween(String a, String b, LocalDate week, Collection<DuelStatus> statuses);

    long countByChallengerIdAndStatusAndWeek(String challengerId, DuelStatus status, LocalDate week);

    List<Duel> findByStatusAndWeekBefore(DuelStatus status, LocalDate week);

    @Query("SELECT COUNT(d) FROM Duel d WHERE d.winnerId = :userId AND d.status = com.hooppicks.backendapplication.entity.DuelStatus.FINISHED")
    long countWins(String userId);

    @Modifying
    @Query("DELETE FROM Duel d WHERE d.challenger.id = :userId OR d.opponent.id = :userId")
    void deleteAllForUser(String userId);
}
