package com.springapi.clinical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClinicalRecordRequest(
        @NotBlank @Size(max = 2000) String diagnosis,
        @NotBlank @Size(max = 4000) String treatment,
        @Size(max = 2000) String prescription) {
}
