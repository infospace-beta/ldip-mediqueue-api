package com.springapi.user.dto;

import java.time.Instant;

import com.springapi.user.User;

/**
 * What the API returns for a user. Never return the {@link User} entity itself from a controller.
 */
public record UserResponse(
		Long id,
		String username,
		String email,
		String fullName,
		Instant createdAt,
		Instant updatedAt) {

	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getUsername(),
				user.getEmail(),
				user.getFullName(),
				user.getCreatedAt(),
				user.getUpdatedAt());
	}

}
