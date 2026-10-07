package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.SeasonPick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SeasonPickRepository extends JpaRepository<SeasonPick, String> {

    List<SeasonPick> findBySeason(String season);

    List<SeasonPick> findBySeasonAndUserId(String season, String userId);

    Optional<SeasonPick> findBySeasonAndUserIdAndQuestion(String season, String userId, String question);

    @Modifying
    @Query("DELETE FROM SeasonPick p WHERE p.userId = :userId")
    void deleteAllForUser(String userId);
}
