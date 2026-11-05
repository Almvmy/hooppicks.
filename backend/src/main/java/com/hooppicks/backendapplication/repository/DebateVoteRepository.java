package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.DebateVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface DebateVoteRepository extends JpaRepository<DebateVote, String> {

    long countByDayAndSide(LocalDate day, String side);

    long countByDay(LocalDate day);

    List<DebateVote> findByDay(LocalDate day);

    @Modifying
    @Query("DELETE FROM DebateVote v WHERE v.userId = :userId")
    void deleteAllForUser(String userId);
}
