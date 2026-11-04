package com.springapi.doctor;

import jakarta.validation.constraints.NotNull;

public record DoctorAvailabilityRequest(
        @NotNull
        Boolean onDuty) {
}
