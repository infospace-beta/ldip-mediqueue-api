package com.springapi.notification.dto;

import java.time.Instant;
import java.util.Locale;

import com.springapi.notification.Notification;

public record NotificationResponse(
        Long id,
        String kind,
        String title,
        String message,
        Instant createdAt,
        boolean unread) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getKind().name().toLowerCase(Locale.ROOT),
                notification.getTitle(),
                notification.getMessage(),
                notification.getCreatedAt(),
                notification.getReadAt() == null);
    }
}
