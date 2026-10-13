package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.dto.NotificationDto;
import com.hooppicks.backendapplication.entity.AppNotification;
import com.hooppicks.backendapplication.repository.NotificationRepository;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final SessionStore sessionStore;

    public NotificationController(NotificationRepository notificationRepository, SessionStore sessionStore) {
        this.notificationRepository = notificationRepository;
        this.sessionStore = sessionStore;
    }

    /** Les 50 plus récentes : avec les matchs suivis, un joueur en reçoit des dizaines par semaine. */
    @GetMapping
    public ResponseEntity<List<NotificationDto>> getNotifications(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();

        List<NotificationDto> notifications = notificationRepository.findTop50ByUserIdOrderByDateDesc(userId)
                .stream().map(NotificationDto::from).toList();
        return ResponseEntity.ok(notifications);
    }

    /** Compté en base, pas sur la liste : des non-lues peuvent dépasser les 50 affichées. */
    @GetMapping("/unread-count")
    public ResponseEntity<?> unreadCount(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of("count", notificationRepository.countByUserIdAndReadFalse(userId)));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        notificationRepository.markAllRead(userId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable String id, HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();

        AppNotification notification = notificationRepository.findById(id).orElse(null);
        if (notification == null || !notification.getUser().getId().equals(userId)) {
            return ResponseEntity.status(404).build();
        }

        notification.setRead(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok().build();
    }
}
