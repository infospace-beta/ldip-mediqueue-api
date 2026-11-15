package com.springapi.patient;

import java.util.Locale;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.springapi.auth.AuthService;
import com.springapi.auth.dto.RegisterRequest;
import com.springapi.common.dto.PageResponse;
import com.springapi.common.exception.ConflictException;
import com.springapi.common.exception.ResourceNotFoundException;
import com.springapi.patient.dto.PatientResponse;
import com.springapi.patient.dto.PatientUpdateRequest;
import com.springapi.user.User;
import com.springapi.user.UserRepository;
import com.springapi.user.UserRole;

@Service
@Transactional(readOnly = true)
public class PatientService {

    private final UserRepository userRepository;
    private final AuthService authService;

    public PatientService(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    public PageResponse<PatientResponse> getAllPatients(
            String search,
            Pageable pageable) {
        String normalizedSearch = search == null || search.isBlank()
                ? null : search.trim();
        return PageResponse.from(userRepository
                .findPatients(normalizedSearch, pageable)
                .map(PatientResponse::from));
    }

    public PatientResponse getPatient(Long id) {
        return PatientResponse.from(getPatientEntity(id));
    }

    @Transactional
    public PatientResponse registerPatient(RegisterRequest request) {
        return PatientResponse.from(authService.register(request));
    }

    @Transactional
    public PatientResponse updatePatient(Long id, PatientUpdateRequest request) {
        User patient = getPatientEntity(id);
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        ensureUnique(username, email, id);

        patient.setUsername(username);
        patient.setEmail(email);
        patient.setFullName(request.fullName().trim());

        return PatientResponse.from(userRepository.saveAndFlush(patient));
    }

    private User getPatientEntity(Long id) {
        return userRepository.findByIdAndRole(id, UserRole.PATIENT)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", id));
    }

    private void ensureUnique(String username, String email, Long patientId) {
        userRepository.findByUsername(username)
                .filter(existing -> !existing.getId().equals(patientId))
                .ifPresent(existing -> {
                    throw new ConflictException(
                            "Username '%s' is already taken"
                                    .formatted(username));
                });
        userRepository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(patientId))
                .ifPresent(existing -> {
                    throw new ConflictException(
                            "Email '%s' is already registered"
                                    .formatted(email));
                });
    }
}
