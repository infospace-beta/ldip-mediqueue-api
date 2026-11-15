package com.springapi.patient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.springapi.queue.QueueTicketRepository;
import com.springapi.user.User;
import com.springapi.user.UserRepository;
import com.springapi.user.UserRole;

class PatientAuthorizationTest {

    private UserRepository userRepository;
    private QueueTicketRepository queueTicketRepository;
    private PatientAuthorization authorization;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        queueTicketRepository = mock(QueueTicketRepository.class);
        authorization = new PatientAuthorization(
                userRepository, queueTicketRepository);
    }

    @Test
    void patientCanReadAndUpdateOwnRecordOnly() {
        User patient = mock(User.class);
        when(patient.getId()).thenReturn(9L);
        when(patient.getRole()).thenReturn(UserRole.PATIENT);
        when(userRepository.findByUsername("patient"))
                .thenReturn(Optional.of(patient));
        var authentication = authentication("patient", "PATIENT");

        assertTrue(authorization.canRead(authentication, 9L));
        assertTrue(authorization.canUpdate(authentication, 9L));
        assertFalse(authorization.canRead(authentication, 10L));
        assertFalse(authorization.canUpdate(authentication, 10L));
    }

    @Test
    void clinicalStaffCanReadOnlyPatientsWithHistoryAtTheirHospital() {
        when(userRepository.findHospitalIdByUsername("doctor"))
                .thenReturn(Optional.of(2L));
        when(queueTicketRepository.existsByPatientIdAndHospitalId(9L, 2L))
                .thenReturn(true);
        when(queueTicketRepository.existsByPatientIdAndHospitalId(10L, 2L))
                .thenReturn(false);
        var authentication = authentication("doctor", "DOCTOR");

        assertTrue(authorization.canRead(authentication, 9L));
        assertFalse(authorization.canRead(authentication, 10L));
    }

    private UsernamePasswordAuthenticationToken authentication(
            String username,
            String role) {
        return new UsernamePasswordAuthenticationToken(
                username,
                "token",
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
