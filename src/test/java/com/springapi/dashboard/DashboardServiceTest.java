package com.springapi.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springapi.common.exception.ConflictException;
import com.springapi.emergency.EmergencyCaseRepository;
import com.springapi.emergency.EmergencyStatus;
import com.springapi.hospital.Hospital;
import com.springapi.hospital.HospitalRepository;
import com.springapi.queue.QueueStatus;
import com.springapi.queue.QueueTicketRepository;
import com.springapi.user.User;
import com.springapi.user.UserRepository;
import com.springapi.user.UserRole;

class DashboardServiceTest {

    private HospitalRepository hospitalRepository;
    private QueueTicketRepository queueTicketRepository;
    private EmergencyCaseRepository emergencyCaseRepository;
    private UserRepository userRepository;
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        hospitalRepository = mock(HospitalRepository.class);
        queueTicketRepository = mock(QueueTicketRepository.class);
        emergencyCaseRepository = mock(EmergencyCaseRepository.class);
        userRepository = mock(UserRepository.class);
        dashboardService = new DashboardService(
                hospitalRepository,
                queueTicketRepository,
                emergencyCaseRepository,
                userRepository);
    }

    @Test
    void aggregatesHospitalDashboardMetrics() {
        when(hospitalRepository.existsById(2L)).thenReturn(true);
        when(queueTicketRepository.countByHospitalAndStatus(
                2L, QueueStatus.WAITING)).thenReturn(4L);
        when(queueTicketRepository.findAverageWaitMinutesByHospitalId(2L))
                .thenReturn(12.5);
        when(emergencyCaseRepository.countByHospitalIdAndStatusIn(
                2L, List.of(
                        EmergencyStatus.REPORTED,
                        EmergencyStatus.ASSIGNED,
                        EmergencyStatus.RESPONDING)))
                .thenReturn(1L);
        when(userRepository.countByHospitalIdAndRoleAndOnDutyTrue(
                2L, UserRole.DOCTOR)).thenReturn(3L);

        var response = dashboardService.getHospitalDashboard(2L);

        assertEquals(4L, response.waitingTickets());
        assertEquals(12.5, response.averageWaitMinutes());
        assertEquals(1L, response.openEmergencyCases());
        assertEquals(3L, response.onDutyDoctors());
    }

    @Test
    void buildsDoctorDashboardFromAssignedHospital() {
        User doctor = mock(User.class);
        Hospital hospital = mock(Hospital.class);
        when(doctor.getRole()).thenReturn(UserRole.DOCTOR);
        when(doctor.getHospital()).thenReturn(hospital);
        when(doctor.isOnDuty()).thenReturn(true);
        when(hospital.getId()).thenReturn(2L);
        when(userRepository.findByUsername("doctor"))
                .thenReturn(Optional.of(doctor));
        when(queueTicketRepository.countByHospitalAndStatus(
                2L, QueueStatus.WAITING)).thenReturn(4L);
        when(emergencyCaseRepository.countByHospitalIdAndStatusIn(
                2L, List.of(
                        EmergencyStatus.REPORTED,
                        EmergencyStatus.ASSIGNED,
                        EmergencyStatus.RESPONDING)))
                .thenReturn(1L);

        var response = dashboardService.getDoctorDashboard("doctor");

        assertEquals(2L, response.hospitalId());
        assertEquals(true, response.onDuty());
        assertEquals(4L, response.waitingTickets());
    }

    @Test
    void rejectsStaffDashboardWithoutHospitalAssignment() {
        User staff = mock(User.class);
        when(staff.getRole()).thenReturn(UserRole.RECEPTIONIST);
        when(staff.getHospital()).thenReturn(null);
        when(userRepository.findByUsername("staff"))
                .thenReturn(Optional.of(staff));

        assertThrows(ConflictException.class,
                () -> dashboardService.getStaffDashboard("staff"));
    }
}
