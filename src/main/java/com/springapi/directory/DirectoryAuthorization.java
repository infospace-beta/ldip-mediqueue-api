package com.springapi.directory;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.springapi.user.UserRepository;

@Component("directoryAuthorization")
public class DirectoryAuthorization {

    private final UserRepository userRepository;

    public DirectoryAuthorization(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean canReadDepartments(
            Authentication authentication,
            Long hospitalId) {
        if (authentication == null || !authentication.isAuthenticated()
                || hospitalId == null) {
            return false;
        }

        if (hasRole(authentication, "SYSTEM_ADMIN")
                || hasRole(authentication, "PATIENT")) {
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
