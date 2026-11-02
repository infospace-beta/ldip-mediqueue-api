package com.springapi.hospital.dto;

import java.time.Instant;
import com.springapi.hospital.Hospital;

public record HospitalResponse(
        Long id,
        String name,
        String address,
        String phone,
        String email,
        Instant createdAt,
        Instant updatedAt
) {
    public static HospitalResponse from(Hospital hospital) {
        return new HospitalResponse(
                hospital.getId(),
                hospital.getName(),
                hospital.getAddress(),
                hospital.getPhone(),
                hospital.getEmail(),
                hospital.getCreatedAt(),
                hospital.getUpdatedAt()
        );
    }
}