package com.springapi.notification;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.springapi.common.exception.ResourceNotFoundException;
import com.springapi.notification.dto.PatientNotificationPreferenceRequest;
import com.springapi.notification.dto.PatientNotificationPreferenceResponse;
import com.springapi.user.User;
import com.springapi.user.UserRepository;
import com.springapi.user.UserRole;

@Service
public class PatientNotificationPreferenceService {

    private final PatientNotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    public PatientNotificationPreferenceService(
            PatientNotificationPreferenceRepository preferenceRepository,
            UserRepository userRepository) {
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PatientNotificationPreferenceResponse getPreferences(String username) {
        User user = getPatient(username);

        PatientNotificationPreference preference = preferenceRepository
                .findByUserUsername(username)
                .orElseGet(() -> createDefaultPreferences(user));

        return PatientNotificationPreferenceResponse.from(preference);
    }

    @Transactional
    public PatientNotificationPreferenceResponse savePreferences(
            String username,
            PatientNotificationPreferenceRequest request) {

        User user = getPatient(username);

        PatientNotificationPreference preference = preferenceRepository
                .findByUserUsername(username)
                .orElseGet(() -> createDefaultPreferences(user));

        preference.setQueueUpdates(request.queueUpdates());
        preference.setAppointmentReminders(request.appointmentReminders());
        preference.setHospitalAnnouncements(request.hospitalAnnouncements());
        preference.setSmsNotifications(request.smsNotifications());
        preference.setEmailNotifications(request.emailNotifications());

        PatientNotificationPreference saved = preferenceRepository.save(preference);

        return PatientNotificationPreferenceResponse.from(saved);
    }

    private User getPatient(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        if (user.getRole() != UserRole.PATIENT) {
            throw new AccessDeniedException(
                    "Only patient accounts can manage patient notification preferences.");
        }

        return user;
    }

    private PatientNotificationPreference createDefaultPreferences(User user) {
        PatientNotificationPreference preference = new PatientNotificationPreference();
        preference.setUser(user);
        return preferenceRepository.save(preference);
    }
}