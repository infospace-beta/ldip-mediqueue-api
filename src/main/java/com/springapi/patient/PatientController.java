package com.springapi.patient;

import java.net.URI;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.springapi.auth.dto.RegisterRequest;
import com.springapi.common.dto.PageResponse;
import com.springapi.emergency.EmergencyService;
import com.springapi.emergency.dto.EmergencyResponse;
import com.springapi.patient.dto.PatientResponse;
import com.springapi.patient.dto.PatientUpdateRequest;
import com.springapi.queue.QueueService;
import com.springapi.queue.QueueStatus;
import com.springapi.queue.QueueTicketResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/patients")
@Tag(name = "Patients")
public class PatientController {

    private final PatientService patientService;
    private final QueueService queueService;
    private final EmergencyService emergencyService;

    public PatientController(
            PatientService patientService,
            QueueService queueService,
            EmergencyService emergencyService) {
        this.patientService = patientService;
        this.queueService = queueService;
        this.emergencyService = emergencyService;
    }

    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResponse<PatientResponse> getAllPatients(
            @RequestParam(required = false) String search,
            @ParameterObject @SortDefault("id") Pageable pageable) {
        return patientService.getAllPatients(search, pageable);
    }

    @GetMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("@patientAuthorization.canRead(authentication, #p0)")
    public PatientResponse getPatient(@PathVariable Long id) {
        return patientService.getPatient(id);
    }

    @GetMapping("/{id}/visits")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("@patientAuthorization.canRead(authentication, #p0)")
    public PageResponse<QueueTicketResponse> getPatientVisits(
            @PathVariable Long id,
            @RequestParam(required = false) QueueStatus status,
            @ParameterObject @SortDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return queueService.getPatientHistory(id, status, pageable);
    }

    @GetMapping("/{id}/emergency-cases")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("@emergencyAuthorization.canReadPatientCases(authentication, #p0)")
    public PageResponse<EmergencyResponse> getPatientEmergencyCases(
            @PathVariable Long id,
            @ParameterObject @SortDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return emergencyService.getForPatient(id, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PatientResponse> registerPatient(
            @Valid @RequestBody RegisterRequest request) {
        PatientResponse created = patientService.registerPatient(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("@patientAuthorization.canUpdate(authentication, #p0)")
    public PatientResponse updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody PatientUpdateRequest request) {
        return patientService.updatePatient(id, request);
    }
}
