package com.springapi.emergency;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.springapi.user.UserRepository;

class EmergencyAuthorizationTest {

    private EmergencyCaseRepository emergencyCaseRepository;
    private UserRepository userRepository;
    private EmergencyAuthorization authorization;

    @BeforeEach
    void setUp() {
        emergencyCaseRepository = mock(EmergencyCaseRepository.class);
        userRepository = mock(UserRepository.class);
        authorization = new EmergencyAuthorization(
                emergencyCaseRepository, userRepository);
    }

    @Test
    void patientCanReadOnlyCaseTheyReported() {
        when(emergencyCaseRepository.existsReportedBy(2L, 5L, "patient"))
                .thenReturn(true);
        var authentication = authentication("patient", "PATIENT");

        assertTrue(authorization.canAccessCase(authentication, 2L, 5L));
        assertFalse(authorization.canAccessCase(authentication, 2L, 6L));
    }

    @Test
    void responderCanUpdateOnlyAssignedCase() {
        when(emergencyCaseRepository.existsAssignedResponder(
                2L, 5L, "responder"))
                .thenReturn(true);
        var authentication = authentication("responder", "RESPONDER");

        assertTrue(authorization.canUpdateStatus(authentication, 2L, 5L));
        assertTrue(authorization.canResolve(authentication, 2L, 5L));
        assertFalse(authorization.canUpdateStatus(authentication, 2L, 6L));
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
