package com.springapi.patient.dto;

import com.springapi.user.User;

public record PatientResponse(
        Long id,
        String username,
        String email,
        String fullName) {

    public static PatientResponse from(User patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getUsername(),
                patient.getEmail(),
                patient.getFullName());
    }
}
