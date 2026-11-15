package com.springapi.emergency.dto;

import com.springapi.emergency.EmergencyStatus;

import jakarta.validation.constraints.NotNull;

public record EmergencyStatusRequest(
        @NotNull
        EmergencyStatus status) {
}
