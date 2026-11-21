package com.springapi.contact;

import java.time.Instant;

public record ContactResponse(
        Long id,
        String name,
        String email,
        String message,
        String status,
        Instant createdAt
) {
    public static ContactResponse from(ContactMessage contactMessage) {
        return new ContactResponse(
                contactMessage.getId(),
                contactMessage.getName(),
                contactMessage.getEmail(),
                contactMessage.getMessage(),
                contactMessage.getStatus(),
                contactMessage.getCreatedAt()
        );
    }
}
