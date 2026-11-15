package com.springapi.emergency.dto;

import java.time.Instant;

import com.springapi.emergency.EmergencyCase;
import com.springapi.emergency.EmergencySeverity;
import com.springapi.emergency.EmergencyStatus;

public record EmergencyResponse(
        Long id,
        Long hospitalId,
        Long patientId,
        Long reportedById,
        Long assignedResponderId,
        String title,
        String description,
        EmergencySeverity severity,
        EmergencyStatus status,
        Instant resolvedAt,
        Instant createdAt,
        Instant updatedAt) {

    public static EmergencyResponse from(EmergencyCase emergencyCase) {
        return new EmergencyResponse(
                emergencyCase.getId(),
                emergencyCase.getHospital().getId(),
                emergencyCase.getPatient() == null
                        ? null : emergencyCase.getPatient().getId(),
                emergencyCase.getReportedBy().getId(),
                emergencyCase.getAssignedResponder() == null
                        ? null : emergencyCase.getAssignedResponder().getId(),
                emergencyCase.getTitle(),
                emergencyCase.getDescription(),
                emergencyCase.getSeverity(),
                emergencyCase.getStatus(),
                emergencyCase.getResolvedAt(),
                emergencyCase.getCreatedAt(),
                emergencyCase.getUpdatedAt());
    }
}
