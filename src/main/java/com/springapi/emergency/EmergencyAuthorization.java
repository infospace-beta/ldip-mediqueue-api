package com.springapi.emergency;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.springapi.user.UserRepository;

@Component("emergencyAuthorization")
public class EmergencyAuthorization {

    private final EmergencyCaseRepository emergencyCaseRepository;
    private final UserRepository userRepository;

    public EmergencyAuthorization(
            EmergencyCaseRepository emergencyCaseRepository,
            UserRepository userRepository) {
        this.emergencyCaseRepository = emergencyCaseRepository;
        this.userRepository = userRepository;
    }

    public boolean canAccessHospital(
            Authentication authentication,
            Long hospitalId) {
        if (!isAuthenticated(authentication) || hospitalId == null) {
            return false;
        }
        if (hasRole(authentication, "SYSTEM_ADMIN")) {
            return true;
        }
        if (!hasAnyRole(authentication, "HOSPITAL_ADMIN", "DOCTOR",
                "RECEPTIONIST", "MEDICAL_STAFF", "RESPONDER")) {
            return false;
        }
        return userRepository.findHospitalIdByUsername(authentication.getName())
                .filter(hospitalId::equals)
                .isPresent();
    }

    public boolean canReport(
            Authentication authentication,
            Long hospitalId) {
        return isAuthenticated(authentication)
                && (hasRole(authentication, "PATIENT")
                        || canAccessHospital(authentication, hospitalId));
    }

    public boolean canAccessCase(
            Authentication authentication,
            Long hospitalId,
            Long caseId) {
        if (canAccessHospital(authentication, hospitalId)) {
            return true;
        }
        return isAuthenticated(authentication)
                && hasRole(authentication, "PATIENT")
                && emergencyCaseRepository.existsReportedBy(
                        hospitalId, caseId, authentication.getName());
    }

    public boolean canManageCase(
            Authentication authentication,
            Long hospitalId) {
        return canAccessHospital(authentication, hospitalId)
                && hasAnyRole(authentication, "SYSTEM_ADMIN",
                        "HOSPITAL_ADMIN", "DOCTOR", "RECEPTIONIST",
                        "MEDICAL_STAFF");
    }

    public boolean canAssignResponder(
            Authentication authentication,
            Long hospitalId) {
        return canAccessHospital(authentication, hospitalId)
                && hasAnyRole(authentication, "SYSTEM_ADMIN", "HOSPITAL_ADMIN");
    }

    public boolean canUpdateStatus(
            Authentication authentication,
            Long hospitalId,
            Long caseId) {
        if (!isAuthenticated(authentication)) {
            return false;
        }
        if (canAccessHospital(authentication, hospitalId)
                && hasAnyRole(authentication, "SYSTEM_ADMIN",
                        "HOSPITAL_ADMIN", "DOCTOR", "MEDICAL_STAFF")) {
            return true;
        }
        return hasRole(authentication, "RESPONDER")
                && emergencyCaseRepository.existsAssignedResponder(
                        hospitalId, caseId, authentication.getName());
    }

    public boolean canResolve(
            Authentication authentication,
            Long hospitalId,
            Long caseId) {
        if (!isAuthenticated(authentication)) {
            return false;
        }
        if (canAccessHospital(authentication, hospitalId)
                && hasAnyRole(authentication, "SYSTEM_ADMIN",
                        "HOSPITAL_ADMIN", "DOCTOR", "MEDICAL_STAFF")) {
            return true;
        }
        return hasRole(authentication, "RESPONDER")
                && emergencyCaseRepository.existsAssignedResponder(
                        hospitalId, caseId, authentication.getName());
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
