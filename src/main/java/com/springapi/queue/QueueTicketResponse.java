package com.springapi.queue;

import java.time.Instant;

public record QueueTicketResponse(
        Long id,
        Long patientId,
        Long departmentId,
        String departmentName,
        QueueStatus status,
        Instant joinedAt,
        Instant calledAt,
        Instant completedAt) {

    public static QueueTicketResponse from(QueueTicket ticket) {
        return new QueueTicketResponse(
                ticket.getId(),
                ticket.getPatient().getId(),
                ticket.getDepartment().getId(),
                ticket.getDepartment().getName(),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                ticket.getCalledAt(),
                ticket.getCompletedAt());
    }
}
