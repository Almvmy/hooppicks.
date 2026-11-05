package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.QuizChampion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;

public interface QuizChampionRepository extends JpaRepository<QuizChampion, LocalDate> {

    @Modifying
    @Query("DELETE FROM QuizChampion c WHERE c.userId = :userId")
    void deleteAllForUser(String userId);
}
