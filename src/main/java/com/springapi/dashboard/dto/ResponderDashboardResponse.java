package com.springapi.dashboard.dto;

public record ResponderDashboardResponse(
        Long hospitalId,
        long assignedCases,
        long respondingCases) {
}
