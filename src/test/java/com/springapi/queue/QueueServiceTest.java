package com.springapi.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springapi.common.exception.ConflictException;
import com.springapi.department.Department;
import com.springapi.department.DepartmentRepository;
import com.springapi.department.DepartmentStatus;
import com.springapi.notification.NotificationService;
import com.springapi.user.User;
import com.springapi.user.UserRepository;
import com.springapi.user.UserRole;

class QueueServiceTest {

    private QueueTicketRepository queueTicketRepository;
    private DepartmentRepository departmentRepository;
    private UserRepository userRepository;
    private NotificationService notificationService;
    private QueueService queueService;

    @BeforeEach
    void setUp() {
        queueTicketRepository = mock(QueueTicketRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        userRepository = mock(UserRepository.class);
        notificationService = mock(NotificationService.class);
        queueService = new QueueService(
                queueTicketRepository,
                departmentRepository,
                userRepository,
                notificationService);
    }

    @Test
    void joinsPatientToActiveDepartmentQueue() {
        User patient = patient();
        Department department = activeDepartment();
        when(userRepository.findByUsername("patient"))
                .thenReturn(Optional.of(patient));
        when(departmentRepository.findById(4L))
                .thenReturn(Optional.of(department));
        when(queueTicketRepository.existsByPatientIdAndDepartmentIdAndStatusIn(
                7L, 4L, java.util.List.of(
                        QueueStatus.WAITING, QueueStatus.CALLED)))
                .thenReturn(false);
        when(queueTicketRepository.save(any(QueueTicket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        QueueTicketResponse response = queueService.joinQueue(4L, "patient");

        assertEquals(QueueStatus.WAITING, response.status());
        assertEquals(7L, response.patientId());
        assertEquals(4L, response.departmentId());
        verify(queueTicketRepository).save(any(QueueTicket.class));
    }

    @Test
    void rejectsDuplicateActiveTicket() {
        User patient = patient();
        Department department = activeDepartment();
        when(userRepository.findByUsername("patient"))
                .thenReturn(Optional.of(patient));
        when(departmentRepository.findById(4L))
                .thenReturn(Optional.of(department));
        when(queueTicketRepository.existsByPatientIdAndDepartmentIdAndStatusIn(
                7L, 4L, java.util.List.of(
                        QueueStatus.WAITING, QueueStatus.CALLED)))
                .thenReturn(true);

        assertThrows(ConflictException.class,
                () -> queueService.joinQueue(4L, "patient"));

        verify(queueTicketRepository, never()).save(any(QueueTicket.class));
    }

    @Test
    void callsOldestWaitingTicket() {
        QueueTicket ticket = ticket(QueueStatus.WAITING);
        Department department = activeDepartment();
        when(departmentRepository.findById(4L))
                .thenReturn(Optional.of(department));
        when(queueTicketRepository
                .findFirstByDepartmentIdAndStatusOrderByCreatedAtAsc(
                        4L, QueueStatus.WAITING))
                .thenReturn(Optional.of(ticket));
        when(queueTicketRepository.save(ticket)).thenReturn(ticket);

        QueueTicketResponse response = queueService.callNext(4L);

        assertEquals(QueueStatus.CALLED, response.status());
        org.junit.jupiter.api.Assertions.assertNotNull(response.calledAt());
        verify(queueTicketRepository).save(ticket);
    }

    @Test
    void rejectsCompletingTicketBeforeItIsCalled() {
        QueueTicket ticket = ticket(QueueStatus.WAITING);
        when(queueTicketRepository.findByIdAndDepartmentId(12L, 4L))
                .thenReturn(Optional.of(ticket));

        assertThrows(ConflictException.class,
                () -> queueService.completeTicket(4L, 12L));

        verify(queueTicketRepository, never()).save(any(QueueTicket.class));
    }

    @Test
    void estimatesWaitUsingOneBasedQueuePosition() {
        Instant joinedAt = Instant.parse("2026-11-04T07:00:00Z");
        QueueTicket ticket = mock(QueueTicket.class);
        Department department = activeDepartment();
        User patient = patient();
        when(ticket.getPatient()).thenReturn(patient);
        when(ticket.getDepartment()).thenReturn(department);
        when(ticket.getId()).thenReturn(12L);
        when(ticket.getStatus()).thenReturn(QueueStatus.WAITING);
        when(ticket.getCreatedAt()).thenReturn(joinedAt);
        when(queueTicketRepository.findById(12L))
                .thenReturn(Optional.of(ticket));
        when(queueTicketRepository.countWaitingAhead(4L, joinedAt, 12L))
                .thenReturn(1L);
        when(queueTicketRepository.findAverageWaitMinutesByDepartmentId(4L))
                .thenReturn(10.4);

        QueueTicketResponse response = queueService.getTicket(12L);

        assertEquals(2L, response.position());
        assertEquals(21L, response.estimatedWaitMinutes());
    }

    private User patient() {
        User patient = mock(User.class);
        when(patient.getId()).thenReturn(7L);
        when(patient.getRole()).thenReturn(UserRole.PATIENT);
        return patient;
    }

    private Department activeDepartment() {
        Department department = mock(Department.class);
        when(department.getId()).thenReturn(4L);
        when(department.getName()).thenReturn("Emergency");
        when(department.getStatus()).thenReturn(DepartmentStatus.ACTIVE);
        return department;
    }

    private QueueTicket ticket(QueueStatus status) {
        QueueTicket ticket = new QueueTicket();
        ticket.setPatient(patient());
        ticket.setDepartment(activeDepartment());
        ticket.setStatus(status);
        return ticket;
    }
}
