package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<AppNotification, String> {
    List<AppNotification> findByUserIdOrderByDateDesc(String userId);

    void deleteByUserId(String userId);
}
