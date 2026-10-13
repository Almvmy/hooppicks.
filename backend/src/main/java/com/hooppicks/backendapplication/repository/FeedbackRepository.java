package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, String> {

    List<Feedback> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Feedback> findTop200ByOrderByCreatedAtDesc();

    List<Feedback> findTop200ByStatusOrderByCreatedAtDesc(String status);

    long countByStatus(String status);

    long countByUserIdAndCreatedAtAfter(String userId, Instant since);

    @Transactional
    void deleteByUserId(String userId);
}
