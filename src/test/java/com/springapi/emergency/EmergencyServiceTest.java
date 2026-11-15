package com.springapi.emergency;

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

import com.springapi.common.exception.ConflictException;
import com.springapi.emergency.dto.EmergencyRequest;
import com.springapi.emergency.dto.EmergencyResponse;
import com.springapi.hospital.Hospital;
import com.springapi.hospital.HospitalRepository;
import com.springapi.hospital.HospitalStatus;
import com.springapi.user.User;
import com.springapi.user.UserRepository;
import com.springapi.user.UserRole;

class EmergencyServiceTest {

    private EmergencyCaseRepository emergencyCaseRepository;
    private HospitalRepository hospitalRepository;
    private UserRepository userRepository;
    private EmergencyService emergencyService;

    @BeforeEach
    void setUp() {
        emergencyCaseRepository = mock(EmergencyCaseRepository.class);
        hospitalRepository = mock(HospitalRepository.class);
        userRepository = mock(UserRepository.class);
        emergencyService = new EmergencyService(
                emergencyCaseRepository, hospitalRepository, userRepository);
    }

    @Test
    void reportsEmergencyToActiveHospital() {
        Hospital hospital = mock(Hospital.class);
        User reporter = mock(User.class);
        when(hospital.getStatus()).thenReturn(HospitalStatus.ACTIVE);
        when(hospital.getId()).thenReturn(2L);
        when(reporter.getId()).thenReturn(3L);
        when(hospitalRepository.findById(2L))
                .thenReturn(Optional.of(hospital));
        when(userRepository.findByUsername("reporter"))
                .thenReturn(Optional.of(reporter));
        when(emergencyCaseRepository.save(any(EmergencyCase.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EmergencyResponse response = emergencyService.report(
                2L,
                "reporter",
                new EmergencyRequest(
                        "Accident", "Assistance required",
                        EmergencySeverity.HIGH, null));

        assertEquals(2L, response.hospitalId());
        assertEquals(3L, response.reportedById());
        assertEquals(EmergencyStatus.REPORTED, response.status());
        verify(emergencyCaseRepository).save(any(EmergencyCase.class));
    }

    @Test
    void assignsOnlyResponderFromSameHospital() {
        EmergencyCase emergencyCase = emergencyCase(EmergencyStatus.REPORTED);
        User responder = mock(User.class);
        when(responder.getId()).thenReturn(8L);
        when(emergencyCaseRepository.findByHospitalIdAndId(2L, 5L))
                .thenReturn(Optional.of(emergencyCase));
        when(userRepository.findByIdAndRoleAndHospitalId(
                8L, UserRole.RESPONDER, 2L))
                .thenReturn(Optional.of(responder));
        when(emergencyCaseRepository.save(emergencyCase))
                .thenReturn(emergencyCase);

        EmergencyResponse response =
                emergencyService.assignResponder(2L, 5L, 8L);

        assertEquals(8L, response.assignedResponderId());
        assertEquals(EmergencyStatus.ASSIGNED, response.status());
    }

    @Test
    void onlyAssignedCasesCanBecomeResponding() {
        EmergencyCase emergencyCase = emergencyCase(EmergencyStatus.REPORTED);
        when(emergencyCaseRepository.findByHospitalIdAndId(2L, 5L))
                .thenReturn(Optional.of(emergencyCase));

        assertThrows(ConflictException.class,
                () -> emergencyService.updateStatus(
                        2L, 5L, EmergencyStatus.RESPONDING));

        verify(emergencyCaseRepository, never())
                .save(any(EmergencyCase.class));
    }

    private EmergencyCase emergencyCase(EmergencyStatus status) {
        Hospital hospital = mock(Hospital.class);
        User reporter = mock(User.class);
        when(hospital.getId()).thenReturn(2L);
        when(reporter.getId()).thenReturn(3L);

        EmergencyCase emergencyCase = new EmergencyCase();
        emergencyCase.setHospital(hospital);
        emergencyCase.setReportedBy(reporter);
        emergencyCase.setTitle("Accident");
        emergencyCase.setDescription("Assistance required");
        emergencyCase.setSeverity(EmergencySeverity.HIGH);
        emergencyCase.setStatus(status);
        return emergencyCase;
    }
}
