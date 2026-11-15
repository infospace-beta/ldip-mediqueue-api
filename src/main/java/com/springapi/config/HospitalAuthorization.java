package com.springapi.config;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.springapi.user.UserRepository;

@Component("hospitalAuthorization")
public class HospitalAuthorization {

    private final UserRepository userRepository;

    public HospitalAuthorization(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean canAccess(Authentication authentication, Long hospitalId) {
        if (authentication == null || !authentication.isAuthenticated()
                || hospitalId == null) {
            return false;
        }

        if (authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_SYSTEM_ADMIN"))) {
            return true;
        }

        if (authentication.getAuthorities().stream()
                .noneMatch(authority ->
                        authority.getAuthority().equals("ROLE_HOSPITAL_ADMIN"))) {
            return false;
        }

        return userRepository.findHospitalIdByUsername(authentication.getName())
                .filter(hospitalId::equals)
                .isPresent();
    }
}
