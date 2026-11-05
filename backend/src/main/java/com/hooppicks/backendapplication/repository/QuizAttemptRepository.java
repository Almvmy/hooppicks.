package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.QuizAttempt;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, String> {

    /** Un double appui ne doit pas jouer deux fois la même question. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from QuizAttempt a where a.id = :id")
    Optional<QuizAttempt> findByIdForUpdate(String id);

    List<QuizAttempt> findByWeek(LocalDate week);

    @Modifying
    @Query("DELETE FROM QuizAttempt a WHERE a.userId = :userId")
    void deleteAllForUser(String userId);
}
