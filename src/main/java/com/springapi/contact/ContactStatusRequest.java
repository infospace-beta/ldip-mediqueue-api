package com.springapi.contact;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ContactStatusRequest(
        @NotBlank
        @Pattern(regexp = "READ|RESOLVED")
        String status
) {
}
