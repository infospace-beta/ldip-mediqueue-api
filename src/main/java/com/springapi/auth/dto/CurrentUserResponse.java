package com.springapi.auth.dto;

import com.springapi.user.User;

public record CurrentUserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String role,
        Long hospitalId) {

    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getHospital() == null ? null : user.getHospital().getId());
    }
}
