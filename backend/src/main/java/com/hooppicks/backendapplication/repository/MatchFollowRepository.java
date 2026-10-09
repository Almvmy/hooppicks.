package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.MatchFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface MatchFollowRepository extends JpaRepository<MatchFollow, String> {

    Optional<MatchFollow> findByUserIdAndMatchId(String userId, String matchId);

    List<MatchFollow> findByUserId(String userId);

    @Query("SELECT f FROM MatchFollow f WHERE f.finalNotified IS NULL OR f.finalNotified = false")
    List<MatchFollow> findActive();

    @Query("SELECT COUNT(f) FROM MatchFollow f WHERE f.userId = :userId AND (f.finalNotified IS NULL OR f.finalNotified = false)")
    long countActiveForUser(String userId);

    @Transactional
    void deleteByUserId(String userId);
}
