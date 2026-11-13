package com.springapi.emergency;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.springapi.common.exception.ConflictException;
import com.springapi.common.exception.ResourceNotFoundException;
import com.springapi.common.dto.PageResponse;
import com.springapi.emergency.dto.EmergencyRequest;
import com.springapi.emergency.dto.EmergencyResponse;
import com.springapi.hospital.Hospital;
import com.springapi.hospital.HospitalRepository;
import com.springapi.hospital.HospitalStatus;
import com.springapi.notification.NotificationKind;
import com.springapi.notification.NotificationService;
import com.springapi.user.User;
import com.springapi.user.UserRepository;
import com.springapi.user.UserRole;

@Service
@Transactional
public class EmergencyService {

    private final EmergencyCaseRepository emergencyCaseRepository;
    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public EmergencyService(
            EmergencyCaseRepository emergencyCaseRepository,
            HospitalRepository hospitalRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.emergencyCaseRepository = emergencyCaseRepository;
        this.hospitalRepository = hospitalRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<EmergencyResponse> getAll(
            Long hospitalId,
            EmergencyStatus status,
            EmergencySeverity severity,
            Pageable pageable) {
        if (!hospitalRepository.existsById(hospitalId)) {
            throw new ResourceNotFoundException("Hospital", hospitalId);
        }
        Page<EmergencyCase> cases = emergencyCaseRepository.findFiltered(
                hospitalId, status, severity, pageable);
        return PageResponse.from(cases.map(EmergencyResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<EmergencyResponse> getForPatient(
            Long patientId,
            Pageable pageable) {
        if (userRepository.findByIdAndRole(patientId, UserRole.PATIENT).isEmpty()) {
            throw new ResourceNotFoundException("Patient", patientId);
        }
        return PageResponse.from(emergencyCaseRepository
                .findForPatient(patientId, pageable)
                .map(EmergencyResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<EmergencyResponse> getAssignedToResponder(
            String username,
            Pageable pageable) {
        Page<EmergencyCase> cases = emergencyCaseRepository
                .findAssignedToResponder(
                        username,
                        List.of(
                                EmergencyStatus.ASSIGNED,
                                EmergencyStatus.RESPONDING),
                        pageable);

        return PageResponse.from(cases.map(EmergencyResponse::from));
    }

    @Transactional(readOnly = true)
    public EmergencyResponse get(Long hospitalId, Long caseId) {
        return EmergencyResponse.from(getCase(hospitalId, caseId));
    }

    public EmergencyResponse report(
            Long hospitalId,
            String username,
            EmergencyRequest request) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Hospital", hospitalId));
        if (hospital.getStatus() != HospitalStatus.ACTIVE) {
            throw new ConflictException(
                    "Emergency cases cannot be reported to an inactive hospital.");
        }
        User reporter = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", username));

        EmergencyCase emergencyCase = new EmergencyCase();
        emergencyCase.setHospital(hospital);
        emergencyCase.setReportedBy(reporter);
        emergencyCase.setPatient(resolvePatient(request.patientId()));
        applyRequest(emergencyCase, request);
        emergencyCase.setStatus(EmergencyStatus.REPORTED);
        EmergencyCase saved = emergencyCaseRepository.save(emergencyCase);
        notifyReporterAndPatient(
                saved,
                "Emergency request received",
                "Emergency request #%d was received by %s."
                        .formatted(saved.getId(), hospital.getName()));
        return EmergencyResponse.from(saved);
    }

    public EmergencyResponse update(
            Long hospitalId,
            Long caseId,
            EmergencyRequest request) {
        EmergencyCase emergencyCase = getCase(hospitalId, caseId);
        if (emergencyCase.getStatus() == EmergencyStatus.RESOLVED) {
            throw new ConflictException("A resolved case cannot be edited.");
        }
        emergencyCase.setPatient(resolvePatient(request.patientId()));
        applyRequest(emergencyCase, request);
        return EmergencyResponse.from(
                emergencyCaseRepository.save(emergencyCase));
    }

    public EmergencyResponse assignResponder(
            Long hospitalId,
            Long caseId,
            Long responderId) {
        EmergencyCase emergencyCase = getCase(hospitalId, caseId);
        if (emergencyCase.getStatus() == EmergencyStatus.RESPONDING
                || emergencyCase.getStatus() == EmergencyStatus.RESOLVED) {
            throw new ConflictException(
                    "A responder cannot be reassigned after responding begins.");
        }

        User responder = userRepository
                .findByIdAndRoleAndHospitalId(
                        responderId, UserRole.RESPONDER, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Responder assigned to this hospital", responderId));
        emergencyCase.setAssignedResponder(responder);
        emergencyCase.setStatus(EmergencyStatus.ASSIGNED);
        EmergencyCase saved = emergencyCaseRepository.save(emergencyCase);
        notificationService.create(
                responder,
                NotificationKind.EMERGENCY,
                "Emergency assigned",
                "Emergency case #%d has been assigned to you."
                        .formatted(saved.getId()));
        notifyPatient(
                saved,
                "Responder assigned",
                "A responder has been assigned to emergency case #%d."
                        .formatted(saved.getId()));
        return EmergencyResponse.from(saved);
    }

    public EmergencyResponse updateStatus(
            Long hospitalId,
            Long caseId,
            EmergencyStatus status) {
        EmergencyCase emergencyCase = getCase(hospitalId, caseId);
        if (status != EmergencyStatus.RESPONDING
                || emergencyCase.getStatus() != EmergencyStatus.ASSIGNED) {
            throw new ConflictException(
                    "Only an assigned case can move to RESPONDING.");
        }
        emergencyCase.setStatus(status);
        EmergencyCase saved = emergencyCaseRepository.save(emergencyCase);
        notifyPatient(
                saved,
                "Emergency response started",
                "A responder is attending emergency case #%d."
                        .formatted(saved.getId()));
        return EmergencyResponse.from(saved);
    }

    public EmergencyResponse resolve(Long hospitalId, Long caseId) {
        EmergencyCase emergencyCase = getCase(hospitalId, caseId);
        if (emergencyCase.getStatus() != EmergencyStatus.ASSIGNED
                && emergencyCase.getStatus() != EmergencyStatus.RESPONDING) {
            throw new ConflictException(
                    "Only an assigned or responding case can be resolved.");
        }
        emergencyCase.setStatus(EmergencyStatus.RESOLVED);
        emergencyCase.setResolvedAt(Instant.now());
        EmergencyCase saved = emergencyCaseRepository.save(emergencyCase);
        notifyReporterAndPatient(
                saved,
                "Emergency case resolved",
                "Emergency case #%d has been resolved."
                        .formatted(saved.getId()));
        return EmergencyResponse.from(saved);
    }

    private void notifyReporterAndPatient(
            EmergencyCase emergencyCase,
            String title,
            String message) {
        notifyPatient(emergencyCase, title, message);
        if (emergencyCase.getPatient() == null
                || !emergencyCase.getPatient().getId()
                        .equals(emergencyCase.getReportedBy().getId())) {
            notificationService.create(
                    emergencyCase.getReportedBy(),
                    NotificationKind.EMERGENCY,
                    title,
                    message);
        }
    }

    private void notifyPatient(
            EmergencyCase emergencyCase,
            String title,
            String message) {
        if (emergencyCase.getPatient() != null) {
            notificationService.create(
                    emergencyCase.getPatient(),
                    NotificationKind.EMERGENCY,
                    title,
                    message);
        }
    }

    private EmergencyCase getCase(Long hospitalId, Long caseId) {
        return emergencyCaseRepository.findByHospitalIdAndId(hospitalId, caseId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Emergency case", caseId));
    }

    private User resolvePatient(Long patientId) {
        if (patientId == null) {
            return null;
        }
        return userRepository.findByIdAndRole(patientId, UserRole.PATIENT)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Patient", patientId));
    }

    private void applyRequest(
            EmergencyCase emergencyCase,
            EmergencyRequest request) {
        emergencyCase.setTitle(request.title().trim());
        emergencyCase.setDescription(request.description().trim());
        emergencyCase.setSeverity(request.severity());
    }
}
