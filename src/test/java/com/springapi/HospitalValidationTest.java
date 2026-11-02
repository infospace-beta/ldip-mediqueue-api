package com.springapi;

import com.springapi.hospital.HospitalController;
import com.springapi.hospital.HospitalService;
import com.springapi.common.exception.GlobalExceptionHandler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HospitalController.class)
class HospitalValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HospitalService hospitalService;

    @Test
    void blankHospitalNameReturns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/hospitals")
                .contentType("application/json")
                .content("""
                    {
                        "name": " ",
                        "address": "Main Street",
                        "phone": "123456",
                        "email": "hospital@example.com"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation failed"))
            .andExpect(jsonPath("$.errors.name")
                .value("Hospital name is required"));

        verifyNoInteractions(hospitalService);
    }
}