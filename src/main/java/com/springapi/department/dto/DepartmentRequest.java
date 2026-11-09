
package com.springapi.department.dto;

import com.springapi.department.DepartmentStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(

        @NotNull(message = "Hospital ID is required")
        Long hospitalId,

        @NotBlank(message = "Department name is required")
        @Size(max = 150, message = "Department name must not exceed 150 characters")
        String name,

        @NotBlank(message = "Department code is required")
        @Size(max = 30, message = "Department code must not exceed 30 characters")
        String code,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotNull(message = "Department status is required")
        DepartmentStatus status

) {}
