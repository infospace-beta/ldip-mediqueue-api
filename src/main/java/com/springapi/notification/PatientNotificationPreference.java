package com.springapi.notification;

import com.springapi.common.entity.BaseEntity;
import com.springapi.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "patient_notification_preferences")
public class PatientNotificationPreference extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "queue_updates", nullable = false)
    private boolean queueUpdates;

    @Column(name = "appointment_reminders", nullable = false)
    private boolean appointmentReminders;

    @Column(name = "hospital_announcements", nullable = false)
    private boolean hospitalAnnouncements;

    @Column(name = "sms_notifications", nullable = false)
    private boolean smsNotifications;

    @Column(name = "email_notifications", nullable = false)
    private boolean emailNotifications;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isQueueUpdates() {
        return queueUpdates;
    }

    public void setQueueUpdates(boolean queueUpdates) {
        this.queueUpdates = queueUpdates;
    }

    public boolean isAppointmentReminders() {
        return appointmentReminders;
    }

    public void setAppointmentReminders(boolean appointmentReminders) {
        this.appointmentReminders = appointmentReminders;
    }

    public boolean isHospitalAnnouncements() {
        return hospitalAnnouncements;
    }

    public void setHospitalAnnouncements(boolean hospitalAnnouncements) {
        this.hospitalAnnouncements = hospitalAnnouncements;
    }

    public boolean isSmsNotifications() {
        return smsNotifications;
    }

    public void setSmsNotifications(boolean smsNotifications) {
        this.smsNotifications = smsNotifications;
    }

    public boolean isEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }
}