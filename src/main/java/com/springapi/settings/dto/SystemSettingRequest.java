package com.springapi.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SystemSettingRequest(

        @NotBlank(message = "Setting value is required")
        String settingValue,

        @NotBlank(message = "Category is required")
        @Size(max = 50, message = "Category must not exceed 50 characters")
        String category,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description

) {
}
