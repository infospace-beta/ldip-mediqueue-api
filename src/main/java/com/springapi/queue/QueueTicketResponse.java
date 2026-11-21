package com.springapi.queue;

import java.time.Instant;

public record QueueTicketResponse(
        Long id,
        Long patientId,
        Long departmentId,
        String departmentName,
        Long hospitalId,
        String hospitalName,
        QueueStatus status,
        Instant joinedAt,
        Instant calledAt,
        Instant completedAt,
        Long position,
        Long estimatedWaitMinutes) {

    public static QueueTicketResponse from(
            QueueTicket ticket,
            Long position,
            Long estimatedWaitMinutes) {

        var department = ticket.getDepartment();
        var hospital = department.getHospital();

        return new QueueTicketResponse(
                ticket.getId(),
                ticket.getPatient().getId(),
                department.getId(),
                department.getName(),
                hospital.getId(),
                hospital.getName(),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                ticket.getCalledAt(),
                ticket.getCompletedAt(),
                position,
                estimatedWaitMinutes);
    }
}
