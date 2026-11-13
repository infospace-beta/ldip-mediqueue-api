package com.springapi.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springapi.auth.AuthService;
import com.springapi.auth.dto.RegisterRequest;
import com.springapi.common.exception.ConflictException;
import com.springapi.patient.dto.PatientUpdateRequest;
import com.springapi.user.User;
import com.springapi.user.UserRepository;
import com.springapi.user.UserRole;

class PatientServiceTest {

    private UserRepository userRepository;
    private AuthService authService;
    private PatientService patientService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        authService = mock(AuthService.class);
        patientService = new PatientService(userRepository, authService);
    }

    @Test
    void registersPatientUsingAuthenticationService() {
        User patient = new User();
        patient.setUsername("patient");
        patient.setEmail("patient@example.com");
        patient.setFullName("Patient");
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(patient);

        var response = patientService.registerPatient(new RegisterRequest(
                "patient", "patient@example.com", "Patient",
                "secure-password"));

        assertEquals("patient", response.username());
        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void normalizesUpdatedPatientProfile() {
        User patient = mock(User.class);
        when(patient.getId()).thenReturn(9L);
        when(patient.getRole()).thenReturn(UserRole.PATIENT);
        when(patient.getUsername()).thenReturn("jane");
        when(patient.getEmail()).thenReturn("jane@example.com");
        when(patient.getFullName()).thenReturn("Jane Doe");
        when(userRepository.findByIdAndRole(9L, UserRole.PATIENT))
                .thenReturn(Optional.of(patient));
        when(userRepository.findByUsername("jane"))
                .thenReturn(Optional.of(patient));
        when(userRepository.findByEmail("jane@example.com"))
                .thenReturn(Optional.of(patient));
        when(userRepository.saveAndFlush(patient)).thenReturn(patient);

        patientService.updatePatient(9L, new PatientUpdateRequest(
                "jane", "JANE@example.com", "Jane Doe"));

        verify(patient).setEmail("jane@example.com");
        verify(userRepository).saveAndFlush(patient);
    }

    @Test
    void rejectsPatientProfileCollision() {
        User patient = mock(User.class);
        User otherPatient = mock(User.class);
        when(patient.getId()).thenReturn(9L);
        when(userRepository.findByIdAndRole(9L, UserRole.PATIENT))
                .thenReturn(Optional.of(patient));
        when(userRepository.findByUsername("taken"))
                .thenReturn(Optional.of(otherPatient));
        when(otherPatient.getId()).thenReturn(10L);

        assertThrows(ConflictException.class,
                () -> patientService.updatePatient(9L,
                        new PatientUpdateRequest(
                                "taken", "new@example.com", "Jane Doe")));
    }
}
