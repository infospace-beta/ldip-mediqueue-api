package com.springapi.auth;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.springapi.auth.dto.AuthResponse;
import com.springapi.common.exception.GlobalExceptionHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {

    private AuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void exchangesSupabaseTokenUsingExistingAuthResponse() throws Exception {
        when(authService.exchangeSupabaseToken("supabase-access-token"))
                .thenReturn(new AuthResponse(
                        "mediqueue-jwt", "Bearer", 900));

        mockMvc.perform(post("/api/v1/auth/supabase/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessToken\":\"supabase-access-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mediqueue-jwt"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));

        verify(authService)
                .exchangeSupabaseToken("supabase-access-token");
    }

    @Test
    void returnsGenericUnauthorizedForAuthenticationFailure()
            throws Exception {
        when(authService.exchangeSupabaseToken("supabase-access-token"))
                .thenThrow(new BadCredentialsException("verifier detail"));

        mockMvc.perform(post("/api/v1/auth/supabase/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessToken\":\"supabase-access-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(
                        "Invalid username/email or password, or account is unavailable."));
    }

    @Test
    void returnsGenericServerErrorForConfigurationFailure()
            throws Exception {
        when(authService.exchangeSupabaseToken("supabase-access-token"))
                .thenThrow(new IllegalStateException(
                        "Supabase JWT issuer and audience must be configured"));

        mockMvc.perform(post("/api/v1/auth/supabase/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessToken\":\"supabase-access-token\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value(
                        "An unexpected error occurred."));
    }

    @Test
    void rejectsBlankAccessToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/supabase/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessToken\":\" \"}"))
                .andExpect(status().isBadRequest());
    }
}
