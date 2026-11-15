package com.springapi.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.springapi.user.UserRepository;

class HospitalAuthorizationTest {

    private UserRepository userRepository;
    private HospitalAuthorization hospitalAuthorization;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        hospitalAuthorization = new HospitalAuthorization(userRepository);
    }

    @Test
    void systemAdminCanAccessAnyHospital() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "system-admin", "token",
                List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN")));

        assertTrue(hospitalAuthorization.canAccess(authentication, 3L));
        verifyNoInteractions(userRepository);
    }

    @Test
    void hospitalAdminCanAccessOnlyAssignedHospital() {
        when(userRepository.findHospitalIdByUsername("hospital-admin"))
                .thenReturn(Optional.of(3L));
        var authentication = new UsernamePasswordAuthenticationToken(
                "hospital-admin", "token",
                List.of(new SimpleGrantedAuthority("ROLE_HOSPITAL_ADMIN")));

        assertTrue(hospitalAuthorization.canAccess(authentication, 3L));
        assertFalse(hospitalAuthorization.canAccess(authentication, 4L));
    }

    @Test
    void rejectsUnauthenticatedAccess() {
        var authentication = UsernamePasswordAuthenticationToken.unauthenticated(
                "anonymous", "token");

        assertFalse(hospitalAuthorization.canAccess(authentication, 3L));
        verifyNoInteractions(userRepository);
    }
}
