package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.RankSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface RankSnapshotRepository extends JpaRepository<RankSnapshot, String> {

    @Query("SELECT MAX(s.snapshotDate) FROM RankSnapshot s WHERE s.snapshotDate < :date")
    LocalDate findLatestDateBefore(LocalDate date);

    List<RankSnapshot> findBySnapshotDate(LocalDate date);

    boolean existsBySnapshotDate(LocalDate date);

    @Modifying
    @Query("DELETE FROM RankSnapshot s WHERE s.snapshotDate = :date")
    void deleteBySnapshotDate(LocalDate date);

    @Modifying
    @Query("DELETE FROM RankSnapshot s WHERE s.snapshotDate < :date")
    void deleteOlderThan(LocalDate date);
}
