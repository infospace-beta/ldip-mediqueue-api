package com.springapi.appointment.dto;

import java.time.Instant;

import com.springapi.appointment.Appointment;
import com.springapi.appointment.AppointmentStatus;

public record AppointmentResponse(
        Long id,
        Long patientId,
        Long doctorId,
        String doctorName,
        Long departmentId,
        String departmentName,
        Long hospitalId,
        String hospitalName,
        Instant startsAt,
        String reason,
        AppointmentStatus status) {

    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatient().getId(),
                appointment.getDoctor().getId(),
                appointment.getDoctor().getFullName(),
                appointment.getDepartment().getId(),
                appointment.getDepartment().getName(),
                appointment.getDepartment().getHospital().getId(),
                appointment.getDepartment().getHospital().getName(),
                appointment.getStartsAt(),
                appointment.getReason(),
                appointment.getStatus());
    }
}
