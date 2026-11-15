package com.springapi.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.springapi.common.dto.PageResponse;
import com.springapi.user.dto.UserRequest;
import com.springapi.user.dto.UserResponse;

class UserControllerTest {

    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new UserController(userService))
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void listsUsersWithFilters() throws Exception {
        when(userService.findAll(
                eq((UserRole) null), eq((Boolean) null),
                eq((Long) null), eq((String) null), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk());

        verify(userService).findAll(
                eq((UserRole) null), eq((Boolean) null),
                eq((Long) null), eq((String) null), any(Pageable.class));
    }

    @Test
    void createsUserWithRoleAndHospital() throws Exception {
        when(userService.create(any(UserRequest.class)))
                .thenReturn(userResponse());

        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content(userRequestJson()))
                .andExpect(status().isCreated());

        verify(userService).create(any(UserRequest.class));
    }

    @Test
    void getsUserById() throws Exception {
        when(userService.findById(6L)).thenReturn(userResponse());

        mockMvc.perform(get("/api/v1/users/6"))
                .andExpect(status().isOk());

        verify(userService).findById(6L);
    }

    @Test
    void updatesUserById() throws Exception {
        when(userService.update(eq(6L), any(UserRequest.class)))
                .thenReturn(userResponse());

        mockMvc.perform(put("/api/v1/users/6")
                        .contentType("application/json")
                        .content(userRequestJson()))
                .andExpect(status().isOk());

        verify(userService).update(eq(6L), any(UserRequest.class));
    }

    @Test
    void deletesUserById() throws Exception {
        mockMvc.perform(delete("/api/v1/users/6"))
                .andExpect(status().isNoContent());

        verify(userService).delete(6L);
    }

    private String userRequestJson() {
        return """
                {
                  "username": "worker",
                  "email": "worker@example.com",
                  "fullName": "Hospital Worker",
                  "role": "MEDICAL_STAFF",
                  "enabled": true,
                  "hospitalId": 2,
                  "password": "secure-password"
                }
                """;
    }

    private UserResponse userResponse() {
        return new UserResponse(
                6L, "worker", "worker@example.com", "Hospital Worker",
                UserRole.MEDICAL_STAFF, true, false, 2L, null, null);
    }
}
