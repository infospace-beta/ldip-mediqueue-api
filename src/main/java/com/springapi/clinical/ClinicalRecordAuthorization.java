package com.springapi.clinical;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.springapi.queue.QueueTicketRepository;
import com.springapi.user.UserRepository;

@Component("clinicalRecordAuthorization")
public class ClinicalRecordAuthorization {

    private final QueueTicketRepository queueTicketRepository;
    private final UserRepository userRepository;

    public ClinicalRecordAuthorization(
            QueueTicketRepository queueTicketRepository,
            UserRepository userRepository) {
        this.queueTicketRepository = queueTicketRepository;
        this.userRepository = userRepository;
    }

    public boolean canWrite(
            Authentication authentication,
            Long queueTicketId) {
        if (authentication == null || !authentication.isAuthenticated()
                || queueTicketId == null
                || !authentication.getAuthorities().stream()
                        .anyMatch(authority ->
                                authority.getAuthority().equals("ROLE_DOCTOR")
                                        || authority.getAuthority().equals(
                                                "ROLE_MEDICAL_STAFF")
                                        || authority.getAuthority().equals(
                                                "ROLE_SYSTEM_ADMIN"))) {
            return false;
        }

        if (authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority()
                        .equals("ROLE_SYSTEM_ADMIN"))) {
            return queueTicketRepository.existsById(queueTicketId);
        }

        return queueTicketRepository
                .findDepartmentHospitalIdByQueueTicketId(queueTicketId)
                .flatMap(ticketHospitalId ->
                        userRepository.findHospitalIdByUsername(
                                authentication.getName())
                                .filter(ticketHospitalId::equals))
                .isPresent();
    }
}
