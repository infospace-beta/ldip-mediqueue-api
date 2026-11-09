package com.springapi.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

import com.springapi.user.UserRole;

/**
 * Request body for creating and updating a user.
 */
public record UserRequest(

		@NotBlank
		@Size(min = 3, max = 50)
		@Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "may only contain letters, digits, '.', '_' and '-'")
		String username,

		@NotBlank
		@Email
		@Size(max = 255)
		String email,

		@NotBlank
		@Size(max = 100)
		String fullName,

		@NotNull
		UserRole role,

		@NotNull
		Boolean enabled,

		Long hospitalId,

		@Size(min = 8, max = 72)
		String password,

		Boolean onDuty) {
}
