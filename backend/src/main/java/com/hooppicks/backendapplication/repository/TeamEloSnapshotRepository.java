package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.TeamEloSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TeamEloSnapshotRepository extends JpaRepository<TeamEloSnapshot, String> {

    boolean existsByDate(LocalDate date);

    List<TeamEloSnapshot> findByDate(LocalDate date);

    /** Dernier jour photographié au plus tard à cette date. */
    @Query("SELECT MAX(s.date) FROM TeamEloSnapshot s WHERE s.date <= :date")
    Optional<LocalDate> findLatestDateOnOrBefore(LocalDate date);
}
