package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    @Modifying
    @Transactional
    @Query("DELETE FROM UserSession s WHERE s.userId = :userId")
    int deleteAllForUser(String userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :now")
    int deleteExpired(Instant now);
}
