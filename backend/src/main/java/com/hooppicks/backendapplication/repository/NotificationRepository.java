package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<AppNotification, String> {
    List<AppNotification> findTop50ByUserIdOrderByDateDesc(String userId);

    long countByUserIdAndReadFalse(String userId);

    @Modifying
    @Transactional
    @Query("UPDATE AppNotification n SET n.read = true WHERE n.user.id = :userId AND n.read = false")
    int markAllRead(String userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM AppNotification n WHERE n.date < :before")
    int deleteOlderThan(Instant before);

    void deleteByUserId(String userId);
}
