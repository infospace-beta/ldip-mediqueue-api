package com.springapi.emergency;

import java.time.Instant;
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
import com.springapi.user.User;
import com.springapi.user.UserRepository;
import com.springapi.user.UserRole;

@Service
@Transactional
public class EmergencyService {

    private final EmergencyCaseRepository emergencyCaseRepository;
    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;

    public EmergencyService(
            EmergencyCaseRepository emergencyCaseRepository,
            HospitalRepository hospitalRepository,
            UserRepository userRepository) {
        this.emergencyCaseRepository = emergencyCaseRepository;
        this.hospitalRepository = hospitalRepository;
        this.userRepository = userRepository;
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
        return EmergencyResponse.from(
                emergencyCaseRepository.save(emergencyCase));
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
        return EmergencyResponse.from(
                emergencyCaseRepository.save(emergencyCase));
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
        return EmergencyResponse.from(
                emergencyCaseRepository.save(emergencyCase));
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
        return EmergencyResponse.from(
                emergencyCaseRepository.save(emergencyCase));
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
