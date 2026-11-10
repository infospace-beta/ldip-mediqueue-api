package com.springapi.patient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.springapi.common.dto.PageResponse;
import com.springapi.patient.dto.PatientResponse;
import com.springapi.patient.dto.PatientUpdateRequest;
import com.springapi.queue.QueueService;
import com.springapi.queue.QueueStatus;

class PatientControllerTest {

    private PatientService patientService;
    private QueueService queueService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        patientService = mock(PatientService.class);
        queueService = mock(QueueService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new PatientController(patientService, queueService))
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void listsPatients() throws Exception {
        when(patientService.getAllPatients(
                eq((String) null), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/v1/patients"))
                .andExpect(status().isOk());

        verify(patientService).getAllPatients(
                eq((String) null), any(Pageable.class));
    }

    @Test
    void registersPatient() throws Exception {
        when(patientService.registerPatient(any()))
                .thenReturn(new PatientResponse(
                        9L, "jane", "jane@example.com", "Jane Doe"));

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "jane",
                                  "email": "jane@example.com",
                                  "fullName": "Jane Doe",
                                  "password": "secure-password"
                                }
                                """))
                .andExpect(status().isCreated());

        verify(patientService).registerPatient(any());
    }

    @Test
    void getsPatientById() throws Exception {
        when(patientService.getPatient(9L))
                .thenReturn(new PatientResponse(
                        9L, "jane", "jane@example.com", "Jane Doe"));

        mockMvc.perform(get("/api/v1/patients/9"))
                .andExpect(status().isOk());

        verify(patientService).getPatient(9L);
    }

    @Test
    void getsPatientVisitHistory() throws Exception {
        when(queueService.getPatientHistory(
                eq(9L), eq((QueueStatus) null), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/v1/patients/9/visits"))
                .andExpect(status().isOk());

        verify(queueService).getPatientHistory(
                eq(9L), eq((QueueStatus) null), any(Pageable.class));
    }

    @Test
    void updatesPatientById() throws Exception {
        when(patientService.updatePatient(eq(9L), any(PatientUpdateRequest.class)))
                .thenReturn(new PatientResponse(
                        9L, "jane", "jane@example.com", "Jane Doe"));

        mockMvc.perform(put("/api/v1/patients/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "jane",
                                  "email": "jane@example.com",
                                  "fullName": "Jane Doe"
                                }
                                """))
                .andExpect(status().isOk());

        verify(patientService).updatePatient(
                eq(9L), any(PatientUpdateRequest.class));
    }
}
