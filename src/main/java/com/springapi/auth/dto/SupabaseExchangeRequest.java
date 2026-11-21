package com.springapi.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SupabaseExchangeRequest(
        @NotBlank
        String accessToken
) {
}
