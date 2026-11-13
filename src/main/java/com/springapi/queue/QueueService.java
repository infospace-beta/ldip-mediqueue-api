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
import com.springapi.notification.NotificationKind;
import com.springapi.notification.NotificationService;
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
    private final NotificationService notificationService;

    public QueueService(
            QueueTicketRepository queueTicketRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.queueTicketRepository = queueTicketRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
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
        QueueTicket saved = queueTicketRepository.save(ticket);
        notificationService.create(
                patient,
                NotificationKind.QUEUE,
                "Queue ticket created",
                "Your ticket #%d has been added to the %s queue."
                        .formatted(saved.getId(), department.getName()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public QueueTicketResponse getTicket(Long ticketId) {
        QueueTicket ticket = queueTicketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Queue ticket", ticketId));
        return toResponse(ticket);
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
                .map(ticket -> QueueTicketResponse.from(ticket, null, null)));
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
                .map(this::toResponse));
    }

    public QueueTicketResponse cancelTicket(Long ticketId) {
        QueueTicket ticket = queueTicketRepository.findLockedById(ticketId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Queue ticket", ticketId));
        requireStatus(ticket, QueueStatus.WAITING);
        ticket.setStatus(QueueStatus.CANCELLED);
        QueueTicket saved = queueTicketRepository.save(ticket);
        notificationService.create(
                ticket.getPatient(),
                NotificationKind.QUEUE,
                "Queue ticket cancelled",
                "Your queue ticket #%d was cancelled.".formatted(ticketId));
        return toResponse(saved);
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
        QueueTicket saved = queueTicketRepository.save(ticket);
        notificationService.create(
                ticket.getPatient(),
                NotificationKind.QUEUE,
                "Your turn is next",
                "Please proceed for queue ticket #%d."
                        .formatted(ticket.getId()));
        return toResponse(saved);
    }

    public QueueTicketResponse skipTicket(Long departmentId, Long ticketId) {
        QueueTicket ticket = getDepartmentTicket(departmentId, ticketId);
        requireStatus(ticket, QueueStatus.CALLED);
        ticket.setStatus(QueueStatus.SKIPPED);
        QueueTicket saved = queueTicketRepository.save(ticket);
        notificationService.create(
                ticket.getPatient(),
                NotificationKind.QUEUE,
                "Queue ticket skipped",
                "Your queue ticket #%d was marked as skipped."
                        .formatted(ticket.getId()));
        return toResponse(saved);
    }

    public QueueTicketResponse completeTicket(
            Long departmentId,
            Long ticketId) {
        QueueTicket ticket = getDepartmentTicket(departmentId, ticketId);
        requireStatus(ticket, QueueStatus.CALLED);
        ticket.setStatus(QueueStatus.COMPLETED);
        ticket.setCompletedAt(Instant.now());
        QueueTicket saved = queueTicketRepository.save(ticket);
        notificationService.create(
                ticket.getPatient(),
                NotificationKind.QUEUE,
                "Queue visit completed",
                "Your visit for queue ticket #%d is complete."
                        .formatted(ticket.getId()));
        return toResponse(saved);
    }

    private QueueTicketResponse toResponse(QueueTicket ticket) {
        if (ticket.getStatus() == QueueStatus.CALLED) {
            return QueueTicketResponse.from(ticket, 0L, 0L);
        }
        if (ticket.getStatus() != QueueStatus.WAITING) {
            return QueueTicketResponse.from(ticket, null, null);
        }

        Long departmentId = ticket.getDepartment().getId();
        long ahead = queueTicketRepository.countWaitingAhead(
                departmentId, ticket.getCreatedAt(), ticket.getId());
        Double averageWait = queueTicketRepository
                .findAverageWaitMinutesByDepartmentId(departmentId);
        long position = ahead + 1;
        Long estimatedWait = averageWait == null
                ? null : Math.round(averageWait * position);
        return QueueTicketResponse.from(ticket, position, estimatedWait);
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
