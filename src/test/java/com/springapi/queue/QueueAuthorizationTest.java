package com.springapi.queue;

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

import com.springapi.department.DepartmentRepository;
import com.springapi.user.UserRepository;

class QueueAuthorizationTest {

    private DepartmentRepository departmentRepository;
    private QueueTicketRepository queueTicketRepository;
    private UserRepository userRepository;
    private QueueAuthorization authorization;

    @BeforeEach
    void setUp() {
        departmentRepository = mock(DepartmentRepository.class);
        queueTicketRepository = mock(QueueTicketRepository.class);
        userRepository = mock(UserRepository.class);
        authorization = new QueueAuthorization(
                departmentRepository, queueTicketRepository, userRepository);
    }

    @Test
    void staffCanManageOnlyAssignedHospitalDepartment() {
        when(departmentRepository.findHospitalIdById(4L))
                .thenReturn(Optional.of(2L));
        when(userRepository.findHospitalIdByUsername("doctor"))
                .thenReturn(Optional.of(2L));
        when(departmentRepository.findHospitalIdById(5L))
                .thenReturn(Optional.of(3L));
        var authentication = authentication("doctor", "DOCTOR");

        assertTrue(authorization.canManageDepartment(authentication, 4L));
        assertFalse(authorization.canManageDepartment(authentication, 5L));
    }

    @Test
    void patientCanAccessOnlyOwnTicket() {
        when(queueTicketRepository.findByIdAndPatientUsername(12L, "patient"))
                .thenReturn(Optional.of(mock(QueueTicket.class)));
        var authentication = authentication("patient", "PATIENT");

        assertTrue(authorization.canAccessTicket(authentication, 12L));
        assertFalse(authorization.canAccessTicket(authentication, 13L));
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
