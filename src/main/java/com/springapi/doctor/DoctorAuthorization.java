package com.springapi.doctor;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.springapi.config.HospitalAuthorization;
import com.springapi.user.UserRepository;
import com.springapi.user.UserRole;

@Component("doctorAuthorization")
public class DoctorAuthorization {

    private final HospitalAuthorization hospitalAuthorization;
    private final UserRepository userRepository;

    public DoctorAuthorization(
            HospitalAuthorization hospitalAuthorization,
            UserRepository userRepository) {
        this.hospitalAuthorization = hospitalAuthorization;
        this.userRepository = userRepository;
    }

    public boolean canAccessDoctor(
            Authentication authentication,
            Long hospitalId,
            Long doctorId) {
        if (hospitalAuthorization.canAccess(authentication, hospitalId)) {
            return true;
        }
        if (authentication == null || !authentication.isAuthenticated()
                || !authentication.getAuthorities().stream()
                        .anyMatch(authority ->
                                authority.getAuthority().equals("ROLE_DOCTOR"))) {
            return false;
        }
        return userRepository.findByIdAndHospitalIdAndRole(
                        doctorId, hospitalId, UserRole.DOCTOR)
                .filter(doctor -> doctor.getUsername()
                        .equals(authentication.getName()))
                .isPresent();
    }
}
