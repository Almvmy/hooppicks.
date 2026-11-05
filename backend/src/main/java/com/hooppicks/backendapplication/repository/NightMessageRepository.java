package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.NightMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NightMessageRepository extends JpaRepository<NightMessage, String> {

    List<NightMessage> findTop100ByNightAndDebateDayIsNullOrderByCreatedAtDesc(LocalDate night);

    List<NightMessage> findTop100ByDebateDayOrderByCreatedAtDesc(LocalDate debateDay);

    @Query("SELECT COUNT(m) FROM NightMessage m WHERE m.debateDay = :day AND (m.hidden IS NULL OR m.hidden = false)")
    long countVisibleForDebate(LocalDate day);

    List<NightMessage> findTop100ByMatchIdOrderByCreatedAtDesc(String matchId);

    Optional<NightMessage> findTopByUser_IdOrderByCreatedAtDesc(String userId);

    long countByUser_IdAndCreatedAtAfter(String userId, Instant since);

    @Modifying
    @Query("DELETE FROM NightMessage m WHERE m.user.id = :userId")
    void deleteAllForUser(String userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM NightMessage m WHERE m.createdAt < :before")
    int deleteOlderThan(Instant before);
}
