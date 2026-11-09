package com.springapi.queue;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.springapi.common.exception.ConflictException;
import com.springapi.common.exception.ResourceNotFoundException;
import com.springapi.common.dto.PageResponse;
import com.springapi.department.Department;
import com.springapi.department.DepartmentRepository;
import com.springapi.department.DepartmentStatus;
import com.springapi.user.User;
import com.springapi.user.UserRepository;
import com.springapi.user.UserRole;

@Service
@Transactional
public class QueueService {

    private static final List<QueueStatus> ACTIVE_STATUSES =
            List.of(QueueStatus.WAITING, QueueStatus.CALLED);

    private final QueueTicketRepository queueTicketRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public QueueService(
            QueueTicketRepository queueTicketRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository) {
        this.queueTicketRepository = queueTicketRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    public QueueTicketResponse joinQueue(Long departmentId, String username) {
        User patient = userRepository.findByUsername(username)
                .filter(user -> user.getRole() == UserRole.PATIENT)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Patient", username));
        Department department = getActiveDepartment(departmentId);

        if (queueTicketRepository.existsByPatientIdAndDepartmentIdAndStatusIn(
                patient.getId(), departmentId, ACTIVE_STATUSES)) {
            throw new ConflictException(
                    "This patient already has an active ticket in the department.");
        }

        QueueTicket ticket = new QueueTicket();
        ticket.setPatient(patient);
        ticket.setDepartment(department);
        ticket.setStatus(QueueStatus.WAITING);
        return QueueTicketResponse.from(queueTicketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public QueueTicketResponse getTicket(Long ticketId) {
        QueueTicket ticket = queueTicketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Queue ticket", ticketId));
        return QueueTicketResponse.from(ticket);
    }

    @Transactional(readOnly = true)
    public PageResponse<QueueTicketResponse> getDepartmentQueue(
            Long departmentId,
            QueueStatus status,
            Pageable pageable) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new ResourceNotFoundException("Department", departmentId);
        }
        return PageResponse.from(queueTicketRepository
                .findDepartmentQueue(departmentId, status, pageable)
                .map(QueueTicketResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<QueueTicketResponse> getPatientHistory(
            Long patientId,
            QueueStatus status,
            Pageable pageable) {
        if (!userRepository.findByIdAndRole(patientId, UserRole.PATIENT)
                .isPresent()) {
            throw new ResourceNotFoundException("Patient", patientId);
        }
        return PageResponse.from(queueTicketRepository
                .findPatientHistory(patientId, status, pageable)
                .map(QueueTicketResponse::from));
    }

    public QueueTicketResponse cancelTicket(Long ticketId) {
        QueueTicket ticket = queueTicketRepository.findLockedById(ticketId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Queue ticket", ticketId));
        requireStatus(ticket, QueueStatus.WAITING);
        ticket.setStatus(QueueStatus.CANCELLED);
        return QueueTicketResponse.from(queueTicketRepository.save(ticket));
    }

    public QueueTicketResponse callNext(Long departmentId) {
        getActiveDepartment(departmentId);
        QueueTicket ticket = queueTicketRepository
                .findFirstByDepartmentIdAndStatusOrderByCreatedAtAsc(
                        departmentId, QueueStatus.WAITING)
                .orElseThrow(() -> new ConflictException(
                        "There are no waiting tickets in this department."));
        ticket.setStatus(QueueStatus.CALLED);
        ticket.setCalledAt(Instant.now());
        return QueueTicketResponse.from(queueTicketRepository.save(ticket));
    }

    public QueueTicketResponse skipTicket(Long departmentId, Long ticketId) {
        QueueTicket ticket = getDepartmentTicket(departmentId, ticketId);
        requireStatus(ticket, QueueStatus.CALLED);
        ticket.setStatus(QueueStatus.SKIPPED);
        return QueueTicketResponse.from(queueTicketRepository.save(ticket));
    }

    public QueueTicketResponse completeTicket(
            Long departmentId,
            Long ticketId) {
        QueueTicket ticket = getDepartmentTicket(departmentId, ticketId);
        requireStatus(ticket, QueueStatus.CALLED);
        ticket.setStatus(QueueStatus.COMPLETED);
        ticket.setCompletedAt(Instant.now());
        return QueueTicketResponse.from(queueTicketRepository.save(ticket));
    }

    private Department getActiveDepartment(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Department", departmentId));
        if (department.getStatus() != DepartmentStatus.ACTIVE) {
            throw new ConflictException(
                    "Tickets cannot be added to an inactive department.");
        }
        return department;
    }

    private QueueTicket getDepartmentTicket(
            Long departmentId,
            Long ticketId) {
        return queueTicketRepository
                .findByIdAndDepartmentId(ticketId, departmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Queue ticket", ticketId));
    }

    private void requireStatus(QueueTicket ticket, QueueStatus expectedStatus) {
        if (ticket.getStatus() != expectedStatus) {
            throw new ConflictException(
                    "Queue ticket must be %s to perform this operation."
                            .formatted(expectedStatus));
        }
    }
}
