
package com.springapi.hospital.dto;

import java.time.Instant;

import com.springapi.hospital.Hospital;
import com.springapi.hospital.HospitalType;
import com.springapi.hospital.HospitalStatus;

public record HospitalResponse(

        Long id,
        String name,
        String registrationNumber,
        HospitalType hospitalType,
        String address,
        String county,
        String town,
        String phone,
        String email,
        HospitalStatus status,
        Instant createdAt,
        Instant updatedAt

) {

    public static HospitalResponse from(Hospital hospital) {

        return new HospitalResponse(
                hospital.getId(),
                hospital.getName(),
                hospital.getRegistrationNumber(),
                hospital.getHospitalType(),
                hospital.getAddress(),
                hospital.getCounty(),
                hospital.getTown(),
                hospital.getPhone(),
                hospital.getEmail(),
                hospital.getStatus(),
                hospital.getCreatedAt(),
                hospital.getUpdatedAt()
        );
    }
}
