package com.springapi.emergency.dto;

import jakarta.validation.constraints.NotNull;

public record AssignResponderRequest(
        @NotNull
        Long responderId) {
}
