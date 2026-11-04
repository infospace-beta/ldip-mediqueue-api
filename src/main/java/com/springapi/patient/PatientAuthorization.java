package com.springapi.patient;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.springapi.queue.QueueTicketRepository;
import com.springapi.user.UserRepository;
import com.springapi.user.UserRole;

@Component("patientAuthorization")
public class PatientAuthorization {

    private final UserRepository userRepository;
    private final QueueTicketRepository queueTicketRepository;

    public PatientAuthorization(
            UserRepository userRepository,
            QueueTicketRepository queueTicketRepository) {
        this.userRepository = userRepository;
        this.queueTicketRepository = queueTicketRepository;
    }

    public boolean canRead(Authentication authentication, Long patientId) {
        if (!isAuthenticated(authentication) || patientId == null) {
            return false;
        }

        if (hasRole(authentication, "SYSTEM_ADMIN")) {
            return userRepository.findByIdAndRole(patientId, UserRole.PATIENT)
                    .isPresent();
        }

        if (isOwnPatient(authentication, patientId)) {
            return true;
        }

        if (hasAnyRole(authentication,
                "HOSPITAL_ADMIN", "DOCTOR", "RECEPTIONIST", "MEDICAL_STAFF")) {
            return userRepository.findHospitalIdByUsername(authentication.getName())
                    .map(hospitalId -> queueTicketRepository
                            .existsByPatientIdAndHospitalId(patientId, hospitalId))
                    .orElse(false);
        }

        return false;
    }

    public boolean canUpdate(Authentication authentication, Long patientId) {
        return isAuthenticated(authentication)
                && (hasRole(authentication, "SYSTEM_ADMIN")
                        || isOwnPatient(authentication, patientId));
    }

    private boolean isOwnPatient(
            Authentication authentication,
            Long patientId) {
        if (!hasRole(authentication, "PATIENT")) {
            return false;
        }

        return userRepository.findByUsername(authentication.getName())
                .filter(user -> user.getRole() == UserRole.PATIENT)
                .map(user -> patientId.equals(user.getId()))
                .orElse(false);
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated();
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_" + role));
    }

    private boolean hasAnyRole(Authentication authentication, String... roles) {
        for (String role : roles) {
            if (hasRole(authentication, role)) {
                return true;
            }
        }
        return false;
    }
}
