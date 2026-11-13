package com.springapi.clinical.dto;

import java.time.Instant;

import com.springapi.clinical.ClinicalRecord;

public record ClinicalRecordResponse(
        Long id,
        Long queueTicketId,
        Long patientId,
        Long authoredById,
        String authoredByName,
        String diagnosis,
        String treatment,
        String prescription,
        Instant recordedAt) {

    public static ClinicalRecordResponse from(ClinicalRecord record) {
        return new ClinicalRecordResponse(
                record.getId(),
                record.getQueueTicket().getId(),
                record.getPatient().getId(),
                record.getAuthoredBy().getId(),
                record.getAuthoredBy().getFullName(),
                record.getDiagnosis(),
                record.getTreatment(),
                record.getPrescription(),
                record.getCreatedAt());
    }
}
