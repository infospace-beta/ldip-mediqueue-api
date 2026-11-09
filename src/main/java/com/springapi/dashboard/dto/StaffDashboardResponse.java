package com.springapi.dashboard.dto;

public record StaffDashboardResponse(
        Long hospitalId,
        long waitingTickets,
        long openEmergencyCases,
        long onDutyDoctors) {
}
