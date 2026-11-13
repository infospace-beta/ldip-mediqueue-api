package com.springapi.clinical;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.springapi.clinical.dto.ClinicalRecordRequest;
import com.springapi.clinical.dto.ClinicalRecordResponse;
import com.springapi.common.dto.PageResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Clinical records")
@SecurityRequirement(name = "bearerAuth")
public class ClinicalRecordController {

    private final ClinicalRecordService clinicalRecordService;

    public ClinicalRecordController(
            ClinicalRecordService clinicalRecordService) {
        this.clinicalRecordService = clinicalRecordService;
    }

    @GetMapping("/patients/{patientId}/clinical-records")
    @PreAuthorize("@patientAuthorization.canRead(authentication, #p0)")
    public PageResponse<ClinicalRecordResponse> getPatientRecords(
            @PathVariable Long patientId,
            @ParameterObject @SortDefault(
                    sort = "createdAt",
                    direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        return clinicalRecordService.getPatientRecords(patientId, pageable);
    }

    @PostMapping("/queue-tickets/{queueTicketId}/clinical-records")
    @PreAuthorize("@clinicalRecordAuthorization.canWrite(authentication, #p0)")
    @ResponseStatus(HttpStatus.CREATED)
    public ClinicalRecordResponse addRecord(
            @PathVariable Long queueTicketId,
            @Valid @RequestBody ClinicalRecordRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        return clinicalRecordService.addRecord(
                queueTicketId, currentUser.getUsername(), request);
    }
}
