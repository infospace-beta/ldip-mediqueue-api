package com.springapi.dashboard.dto;

public record DoctorDashboardResponse(
        Long hospitalId,
        boolean onDuty,
        long waitingTickets,
        long openEmergencyCases) {
}
