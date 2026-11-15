package com.springapi.department.dto;

import java.time.Instant;

import com.springapi.department.Department;
import com.springapi.department.DepartmentStatus;

public record DepartmentResponse(
        Long id,
        Long hospitalId,
        String name,
        String code,
        String description,
        DepartmentStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static DepartmentResponse from(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getHospital().getId(),
                department.getName(),
                department.getCode(),
                department.getDescription(),
                department.getStatus(),
                department.getCreatedAt(),
                department.getUpdatedAt());
    }
}
