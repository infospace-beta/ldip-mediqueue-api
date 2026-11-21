package com.springapi.notification.dto;

import com.springapi.notification.PatientNotificationPreference;

public record PatientNotificationPreferenceResponse(
        boolean queueUpdates,
        boolean appointmentReminders,
        boolean hospitalAnnouncements,
        boolean smsNotifications,
        boolean emailNotifications
) {
    public static PatientNotificationPreferenceResponse from(
            PatientNotificationPreference preference) {

        return new PatientNotificationPreferenceResponse(
                preference.isQueueUpdates(),
                preference.isAppointmentReminders(),
                preference.isHospitalAnnouncements(),
                preference.isSmsNotifications(),
                preference.isEmailNotifications()
        );
    }
}