package com.springapi.emergency.dto;

import com.springapi.emergency.EmergencySeverity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmergencyRequest(
        @NotBlank
        @Size(max = 150)
        String title,

        @NotBlank
        @Size(max = 2000)
        String description,

        @NotNull
        EmergencySeverity severity,

        Long patientId) {
}
