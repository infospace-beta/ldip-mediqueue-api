package com.springapi.notification;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientNotificationPreferenceRepository
        extends JpaRepository<PatientNotificationPreference, Long> {

    Optional<PatientNotificationPreference> findByUserUsername(String username);
}