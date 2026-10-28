
package com.springapi.hospital.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HospitalRequest(

    @NotBlank(message = "Hospital name is required")
    @Size(max = 150, message = "Hospital name must not exceed 150 characters")
    String name,

    @Size(max = 255, message = "Address must not exceed 255 characters")
    String address,

    @Size(max = 30, message = "Phone must not exceed 30 characters")
    String phone,

    @Email(message = "Please provide a valid email address")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    String email

) {}
