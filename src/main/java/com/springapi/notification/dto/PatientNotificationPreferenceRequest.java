package com.springapi.notification.dto;

public record PatientNotificationPreferenceRequest(
        boolean queueUpdates,
        boolean appointmentReminders,
        boolean hospitalAnnouncements,
        boolean smsNotifications,
        boolean emailNotifications
) {
}