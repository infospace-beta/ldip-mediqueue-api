package com.springapi.queue;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.springapi.department.DepartmentRepository;
import com.springapi.user.UserRepository;

@Component("queueAuthorization")
public class QueueAuthorization {

    private final DepartmentRepository departmentRepository;
    private final QueueTicketRepository queueTicketRepository;
    private final UserRepository userRepository;

    public QueueAuthorization(
            DepartmentRepository departmentRepository,
            QueueTicketRepository queueTicketRepository,
            UserRepository userRepository) {
        this.departmentRepository = departmentRepository;
        this.queueTicketRepository = queueTicketRepository;
        this.userRepository = userRepository;
    }

    public boolean canManageDepartment(
            Authentication authentication,
            Long departmentId) {
        if (!isAuthenticated(authentication) || departmentId == null) {
            return false;
        }

        if (hasRole(authentication, "SYSTEM_ADMIN")) {
            return true;
        }

        if (!hasAnyRole(authentication,
                "HOSPITAL_ADMIN", "DOCTOR", "RECEPTIONIST", "MEDICAL_STAFF")) {
            return false;
        }

        return departmentRepository.findHospitalIdById(departmentId)
                .flatMap(departmentHospitalId ->
                        userRepository.findHospitalIdByUsername(
                                authentication.getName())
                                .filter(departmentHospitalId::equals))
                .isPresent();
    }

    public boolean canAccessTicket(
            Authentication authentication,
            Long ticketId) {
        if (!isAuthenticated(authentication) || ticketId == null) {
            return false;
        }

        if (hasAnyRole(authentication,
                "SYSTEM_ADMIN", "HOSPITAL_ADMIN", "DOCTOR",
                "RECEPTIONIST", "MEDICAL_STAFF")) {
            return queueTicketRepository.findById(ticketId)
                    .map(ticket -> canManageDepartment(
                            authentication, ticket.getDepartment().getId()))
                    .orElse(false);
        }

        return hasRole(authentication, "PATIENT")
                && queueTicketRepository.findByIdAndPatientUsername(
                        ticketId, authentication.getName())
                        .isPresent();
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated();
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_" + role));
    }

    private boolean hasAnyRole(Authentication authentication, String... roles) {
        for (String role : roles) {
            if (hasRole(authentication, role)) {
                return true;
            }
        }
        return false;
    }
}
