
package com.springapi.user.dto;

import java.time.Instant;

import com.springapi.user.User;
import com.springapi.user.UserRole;

/**
 * User information returned by the API.
 * Password hashes must never be exposed.
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        UserRole role,
        boolean enabled,
        boolean onDuty,
        Long hospitalId,
        Instant createdAt,
        Instant updatedAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isEnabled(),
                user.isOnDuty(),
                user.getHospital() == null ? null : user.getHospital().getId(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
