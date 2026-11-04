
package com.springapi.hospital.dto;

import com.springapi.hospital.HospitalType;
import com.springapi.hospital.HospitalStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HospitalRequest(

    @NotBlank(message = "Hospital name is required")
    @Size(max = 150, message = "Hospital name must not exceed 150 characters")
    String name,

    @Size(max = 50, message = "Registration number must not exceed 50 characters")
    String registrationNumber,

    @NotNull(message = "Hospital type is required")
    HospitalType hospitalType,

    @Size(max = 255, message = "Address must not exceed 255 characters")
    String address,

    @Size(max = 100, message = "County must not exceed 100 characters")
    String county,

    @Size(max = 100, message = "Town must not exceed 100 characters")
    String town,

    @Pattern(
        regexp = "^[+]?[0-9\\s()-]{7,30}$",
        message = "Please provide a valid phone number"
    )
    String phone,

    @Email(message = "Please provide a valid email address")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    String email,

    @NotNull(message = "Hospital status is required")
    HospitalStatus status

) {}
