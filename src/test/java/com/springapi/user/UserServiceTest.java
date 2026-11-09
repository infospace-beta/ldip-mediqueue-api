package com.springapi.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.springapi.common.exception.ConflictException;
import com.springapi.hospital.Hospital;
import com.springapi.hospital.HospitalRepository;
import com.springapi.user.dto.UserRequest;
import com.springapi.user.dto.UserResponse;

class UserServiceTest {

    private UserRepository userRepository;
    private HospitalRepository hospitalRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        hospitalRepository = mock(HospitalRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userService = new UserService(
                userRepository, hospitalRepository, passwordEncoder);
    }

    @Test
    void createsHospitalDoctorWithEncodedPasswordAndAvailability() {
        Hospital hospital = new Hospital();
        when(userRepository.findByUsername("doctor"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("doctor@example.com"))
                .thenReturn(Optional.empty());
        when(hospitalRepository.findById(2L))
                .thenReturn(Optional.of(hospital));
        when(passwordEncoder.encode("secure-password"))
                .thenReturn("encoded-password");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.create(new UserRequest(
                "doctor",
                "doctor@example.com",
                "Doctor",
                UserRole.DOCTOR,
                true,
                2L,
                "secure-password",
                true));

        assertEquals(UserRole.DOCTOR, response.role());
        assertEquals(true, response.onDuty());
        verify(passwordEncoder).encode("secure-password");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void requiresHospitalAssignmentForHospitalAdmin() {
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode("secure-password"))
                .thenReturn("encoded-password");

        assertThrows(IllegalArgumentException.class,
                () -> userService.create(new UserRequest(
                        "admin",
                        "admin@example.com",
                        "Admin",
                        UserRole.HOSPITAL_ADMIN,
                        true,
                        null,
                        "secure-password",
                        null)));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void rejectsDuplicateUsernames() {
        when(userRepository.findByUsername("existing"))
                .thenReturn(Optional.of(new User()));

        assertThrows(ConflictException.class,
                () -> userService.create(new UserRequest(
                        "existing",
                        "new@example.com",
                        "New User",
                        UserRole.PATIENT,
                        true,
                        null,
                        "secure-password",
                        null)));

        verify(userRepository, never()).save(any(User.class));
    }
}
