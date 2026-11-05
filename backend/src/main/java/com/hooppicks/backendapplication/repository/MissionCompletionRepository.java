package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.MissionCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MissionCompletionRepository extends JpaRepository<MissionCompletion, String> {

    long countByUserId(String userId);

    @Modifying
    @Query("DELETE FROM MissionCompletion m WHERE m.userId = :userId")
    void deleteAllForUser(String userId);
}
