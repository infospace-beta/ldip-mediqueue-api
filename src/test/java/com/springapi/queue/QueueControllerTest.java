package com.springapi.queue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.bind.support.WebDataBinderFactory;

import com.springapi.common.dto.PageResponse;

class QueueControllerTest {

    private QueueService queueService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        queueService = mock(QueueService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new QueueController(queueService))
                .setCustomArgumentResolvers(
                        new PatientPrincipalResolver(),
                        new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void listsDepartmentQueue() throws Exception {
        when(queueService.getDepartmentQueue(
                eq(4L), eq((QueueStatus) null), any(Pageable.class)))
                .thenReturn(new PageResponse<>(
                        List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/v1/departments/4/queue-tickets"))
                .andExpect(status().isOk());

        verify(queueService).getDepartmentQueue(
                eq(4L), eq((QueueStatus) null), any(Pageable.class));
    }

    @Test
    void joinsQueueForAuthenticatedPatient() throws Exception {
        when(queueService.joinQueue(4L, "patient"))
                .thenReturn(ticket(QueueStatus.WAITING));

        mockMvc.perform(post("/api/v1/departments/4/queue-tickets"))
                .andExpect(status().isCreated());

        verify(queueService).joinQueue(4L, "patient");
    }

    @Test
    void getsQueueTicket() throws Exception {
        when(queueService.getTicket(12L))
                .thenReturn(ticket(QueueStatus.WAITING));

        mockMvc.perform(get("/api/v1/queue-tickets/12"))
                .andExpect(status().isOk());

        verify(queueService).getTicket(12L);
    }

    @Test
    void cancelsQueueTicket() throws Exception {
        mockMvc.perform(delete("/api/v1/queue-tickets/12"))
                .andExpect(status().isNoContent());

        verify(queueService).cancelTicket(12L);
    }

    @Test
    void callsNextTicketInDepartment() throws Exception {
        when(queueService.callNext(4L))
                .thenReturn(ticket(QueueStatus.CALLED));

        mockMvc.perform(post("/api/v1/departments/4/queue/call-next"))
                .andExpect(status().isOk());

        verify(queueService).callNext(4L);
    }

    @Test
    void skipsTicketInDepartment() throws Exception {
        when(queueService.skipTicket(4L, 12L))
                .thenReturn(ticket(QueueStatus.SKIPPED));

        mockMvc.perform(post(
                        "/api/v1/departments/4/queue-tickets/12/skip"))
                .andExpect(status().isOk());

        verify(queueService).skipTicket(4L, 12L);
    }

    @Test
    void completesTicketInDepartment() throws Exception {
        when(queueService.completeTicket(4L, 12L))
                .thenReturn(ticket(QueueStatus.COMPLETED));

        mockMvc.perform(post(
                        "/api/v1/departments/4/queue-tickets/12/complete"))
                .andExpect(status().isOk());

        verify(queueService).completeTicket(4L, 12L);
    }

    private QueueTicketResponse ticket(QueueStatus status) {
        return new QueueTicketResponse(
                12L, 7L, 4L, "Emergency", null, null, status,
                Instant.parse("2026-11-04T07:00:00Z"), null, null,
                null, null);
    }

    private static class PatientPrincipalResolver
            implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                    && UserDetails.class.isAssignableFrom(
                            parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory) {
            return User.withUsername("patient")
                    .password("")
                    .roles("PATIENT")
                    .build();
        }
    }
}
