package com.springapi.dashboard;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.springapi.common.exception.ConflictException;
import com.springapi.common.exception.ResourceNotFoundException;
import com.springapi.dashboard.dto.DoctorDashboardResponse;
import com.springapi.dashboard.dto.HospitalDashboardResponse;
import com.springapi.dashboard.dto.ResponderDashboardResponse;
import com.springapi.dashboard.dto.StaffDashboardResponse;
import com.springapi.emergency.EmergencyCaseRepository;
import com.springapi.emergency.EmergencyStatus;
import com.springapi.hospital.HospitalRepository;
import com.springapi.queue.QueueStatus;
import com.springapi.queue.QueueTicketRepository;
import com.springapi.user.User;
import com.springapi.user.UserRepository;
import com.springapi.user.UserRole;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final List<EmergencyStatus> OPEN_EMERGENCY_STATUSES =
            List.of(
                    EmergencyStatus.REPORTED,
                    EmergencyStatus.ASSIGNED,
                    EmergencyStatus.RESPONDING);

    private final HospitalRepository hospitalRepository;
    private final QueueTicketRepository queueTicketRepository;
    private final EmergencyCaseRepository emergencyCaseRepository;
    private final UserRepository userRepository;

    public DashboardService(
            HospitalRepository hospitalRepository,
            QueueTicketRepository queueTicketRepository,
            EmergencyCaseRepository emergencyCaseRepository,
            UserRepository userRepository) {
        this.hospitalRepository = hospitalRepository;
        this.queueTicketRepository = queueTicketRepository;
        this.emergencyCaseRepository = emergencyCaseRepository;
        this.userRepository = userRepository;
    }

    public HospitalDashboardResponse getHospitalDashboard(Long hospitalId) {
        if (!hospitalRepository.existsById(hospitalId)) {
            throw new ResourceNotFoundException("Hospital", hospitalId);
        }
        return new HospitalDashboardResponse(
                hospitalId,
                queueTicketRepository.countByHospitalAndStatus(
                        hospitalId, QueueStatus.WAITING),
                queueTicketRepository.findAverageWaitMinutesByHospitalId(
                        hospitalId),
                emergencyCaseRepository.countByHospitalIdAndStatusIn(
                        hospitalId, OPEN_EMERGENCY_STATUSES),
                userRepository.countByHospitalIdAndRoleAndOnDutyTrue(
                        hospitalId, UserRole.DOCTOR));
    }

    public DoctorDashboardResponse getDoctorDashboard(String username) {
        User doctor = getUserWithRole(username, UserRole.DOCTOR);
        Long hospitalId = requireHospital(doctor);
        return new DoctorDashboardResponse(
                hospitalId,
                doctor.isOnDuty(),
                queueTicketRepository.countByHospitalAndStatus(
                        hospitalId, QueueStatus.WAITING),
                emergencyCaseRepository.countByHospitalIdAndStatusIn(
                        hospitalId, OPEN_EMERGENCY_STATUSES));
    }

    public StaffDashboardResponse getStaffDashboard(String username) {
        User staff = userRepository.findByUsername(username)
                .filter(user -> user.getRole() == UserRole.RECEPTIONIST
                        || user.getRole() == UserRole.MEDICAL_STAFF)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Staff member", username));
        Long hospitalId = requireHospital(staff);
        return new StaffDashboardResponse(
                hospitalId,
                queueTicketRepository.countByHospitalAndStatus(
                        hospitalId, QueueStatus.WAITING),
                emergencyCaseRepository.countByHospitalIdAndStatusIn(
                        hospitalId, OPEN_EMERGENCY_STATUSES),
                userRepository.countByHospitalIdAndRoleAndOnDutyTrue(
                        hospitalId, UserRole.DOCTOR));
    }

    public ResponderDashboardResponse getResponderDashboard(String username) {
        User responder = getUserWithRole(username, UserRole.RESPONDER);
        Long hospitalId = requireHospital(responder);
        return new ResponderDashboardResponse(
                hospitalId,
                emergencyCaseRepository.countByAssignedResponderUsernameAndStatusIn(
                        username,
                        List.of(EmergencyStatus.ASSIGNED,
                                EmergencyStatus.RESPONDING)),
                emergencyCaseRepository.countByAssignedResponderUsernameAndStatus(
                        username, EmergencyStatus.RESPONDING));
    }

    private User getUserWithRole(String username, UserRole role) {
        return userRepository.findByUsername(username)
                .filter(user -> user.getRole() == role)
                .orElseThrow(() ->
                        new ResourceNotFoundException(role.name(), username));
    }

    private Long requireHospital(User user) {
        if (user.getHospital() == null) {
            throw new ConflictException(
                    "This account is not assigned to a hospital.");
        }
        return user.getHospital().getId();
    }
}
