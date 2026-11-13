package com.springapi.appointment.dto;

import java.time.Instant;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AppointmentRequest(
        @NotNull Long doctorId,
        @NotNull Long departmentId,
        @NotNull @Future Instant startsAt,
        @NotBlank @Size(max = 500) String reason) {
}
