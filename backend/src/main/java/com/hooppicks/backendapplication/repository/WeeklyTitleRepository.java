package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.WeeklyTitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface WeeklyTitleRepository extends JpaRepository<WeeklyTitle, String> {

    List<WeeklyTitle> findByUserIdOrderByWeekDesc(String userId);

    boolean existsByUserIdAndWeekGreaterThanEqual(String userId, LocalDate week);

    List<WeeklyTitle> findByWeekAndLeagueIdIsNull(LocalDate week);

    List<WeeklyTitle> findByWeekAndLeagueId(LocalDate week, String leagueId);

    @Modifying
    @Query("DELETE FROM WeeklyTitle t WHERE t.userId = :userId")
    void deleteAllForUser(String userId);
}
