package com.springapi.emergency;

import java.net.URI;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.springapi.emergency.dto.AssignResponderRequest;
import com.springapi.emergency.dto.EmergencyRequest;
import com.springapi.emergency.dto.EmergencyResponse;
import com.springapi.emergency.dto.EmergencyStatusRequest;
import com.springapi.common.dto.PageResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/hospitals/{hospitalId}/emergency-cases")
@Tag(name = "Emergency cases")
@SecurityRequirement(name = "bearerAuth")
public class EmergencyController {

    private final EmergencyService emergencyService;

    public EmergencyController(EmergencyService emergencyService) {
        this.emergencyService = emergencyService;
    }

    @GetMapping
    @PreAuthorize("@emergencyAuthorization.canAccessHospital(authentication, #p0)")
    public PageResponse<EmergencyResponse> getAll(
            @PathVariable Long hospitalId,
            @RequestParam(required = false) EmergencyStatus status,
            @RequestParam(required = false) EmergencySeverity severity,
            @ParameterObject @SortDefault("createdAt") Pageable pageable) {
        return emergencyService.getAll(
                hospitalId, status, severity, pageable);
    }

    @GetMapping("/{caseId}")
    @PreAuthorize("@emergencyAuthorization.canAccessCase(authentication, #p0, #p1)")
    public EmergencyResponse get(
            @PathVariable Long hospitalId,
            @PathVariable Long caseId) {
        return emergencyService.get(hospitalId, caseId);
    }

    @PostMapping
    @PreAuthorize("@emergencyAuthorization.canReport(authentication, #p0)")
    public ResponseEntity<EmergencyResponse> report(
            @PathVariable Long hospitalId,
            @Valid @RequestBody EmergencyRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        EmergencyResponse created = emergencyService.report(
                hospitalId, currentUser.getUsername(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{caseId}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{caseId}")
    @PreAuthorize("@emergencyAuthorization.canManageCase(authentication, #p0)")
    public EmergencyResponse update(
            @PathVariable Long hospitalId,
            @PathVariable Long caseId,
            @Valid @RequestBody EmergencyRequest request) {
        return emergencyService.update(hospitalId, caseId, request);
    }

    @PutMapping("/{caseId}/assignment")
    @PreAuthorize("@emergencyAuthorization.canAssignResponder(authentication, #p0)")
    public EmergencyResponse assignResponder(
            @PathVariable Long hospitalId,
            @PathVariable Long caseId,
            @Valid @RequestBody AssignResponderRequest request) {
        return emergencyService.assignResponder(
                hospitalId, caseId, request.responderId());
    }

    @PatchMapping("/{caseId}/status")
    @PreAuthorize("@emergencyAuthorization.canUpdateStatus(authentication, #p0, #p1)")
    public EmergencyResponse updateStatus(
            @PathVariable Long hospitalId,
            @PathVariable Long caseId,
            @Valid @RequestBody EmergencyStatusRequest request) {
        return emergencyService.updateStatus(
                hospitalId, caseId, request.status());
    }

    @PostMapping("/{caseId}/resolve")
    @PreAuthorize("@emergencyAuthorization.canResolve(authentication, #p0, #p1)")
    public EmergencyResponse resolve(
            @PathVariable Long hospitalId,
            @PathVariable Long caseId) {
        return emergencyService.resolve(hospitalId, caseId);
    }
}
