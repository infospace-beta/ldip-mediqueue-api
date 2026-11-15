package com.springapi.dashboard.dto;

public record HospitalDashboardResponse(
        Long hospitalId,
        long waitingTickets,
        Double averageWaitMinutes,
        long openEmergencyCases,
        long onDutyDoctors) {
}
