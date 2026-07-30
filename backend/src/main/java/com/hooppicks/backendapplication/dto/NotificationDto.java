package com.hooppicks.backendapplication.dto;

import com.hooppicks.backendapplication.entity.AppNotification;

public record NotificationDto(
        String id,
        String type,
        String message,
        String date,
        boolean read
) {
    public static NotificationDto from(AppNotification n) {
        return new NotificationDto(n.getId(), n.getType().name().toLowerCase(), n.getMessage(), n.getDate().toString(), n.isRead());
    }
}