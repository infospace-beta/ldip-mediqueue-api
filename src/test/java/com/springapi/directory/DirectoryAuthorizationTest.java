package com.springapi.directory;

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

class DirectoryAuthorizationTest {

    private UserRepository userRepository;
    private DirectoryAuthorization directoryAuthorization;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        directoryAuthorization = new DirectoryAuthorization(userRepository);
    }

    @Test
    void patientCanReadActiveHospitalDepartmentDirectory() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "patient", "token",
                List.of(new SimpleGrantedAuthority("ROLE_PATIENT")));

        assertTrue(directoryAuthorization.canReadDepartments(
                authentication, 3L));
        verifyNoInteractions(userRepository);
    }

    @Test
    void hospitalStaffCanReadOnlyTheirAssignedDepartmentDirectory() {
        when(userRepository.findHospitalIdByUsername("staff"))
                .thenReturn(Optional.of(3L));
        var authentication = new UsernamePasswordAuthenticationToken(
                "staff", "token",
                List.of(new SimpleGrantedAuthority("ROLE_RECEPTIONIST")));

        assertTrue(directoryAuthorization.canReadDepartments(
                authentication, 3L));
        assertFalse(directoryAuthorization.canReadDepartments(
                authentication, 4L));
    }

    @Test
    void systemAdminCanReadAnyHospitalDepartmentDirectory() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "system-admin", "token",
                List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN")));

        assertTrue(directoryAuthorization.canReadDepartments(
                authentication, 3L));
        verifyNoInteractions(userRepository);
    }

    @Test
    void rejectsUnauthenticatedAndUnsupportedRoles() {
        var anonymous = UsernamePasswordAuthenticationToken.unauthenticated(
                "anonymous", "token");
        var unsupported = new UsernamePasswordAuthenticationToken(
                "guest", "token",
                List.of(new SimpleGrantedAuthority("ROLE_GUEST")));

        assertFalse(directoryAuthorization.canReadDepartments(anonymous, 3L));
        assertFalse(directoryAuthorization.canReadDepartments(
                unsupported, 3L));
        verifyNoInteractions(userRepository);
    }
}
