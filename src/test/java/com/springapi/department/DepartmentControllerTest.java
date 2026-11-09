package com.springapi.department;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.springapi.department.dto.DepartmentRequest;
import com.springapi.hospital.Hospital;

class DepartmentControllerTest {

    private DepartmentService departmentService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        departmentService = mock(DepartmentService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new DepartmentController(departmentService))
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void listsDepartmentsForHospitalRoute() throws Exception {
        when(departmentService.getAllDepartmentsByHospital(
                eq(3L), eq((String) null), eq((DepartmentStatus) null),
                any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/hospitals/3/departments"))
                .andExpect(status().isOk());

        verify(departmentService).getAllDepartmentsByHospital(
                eq(3L), eq((String) null), eq((DepartmentStatus) null),
                any(Pageable.class));
    }

    @Test
    void createsDepartmentForHospitalRoute() throws Exception {
        when(departmentService.createDepartment(
                eq(3L), any(DepartmentRequest.class)))
                .thenReturn(departmentForHospital());

        mockMvc.perform(post("/api/hospitals/3/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hospitalId": 3,
                                  "name": "Emergency",
                                  "code": "ER",
                                  "description": "Emergency care",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isCreated());

        verify(departmentService).createDepartment(
                eq(3L), any(DepartmentRequest.class));
    }

    @Test
    void getsDepartmentWithinHospitalRoute() throws Exception {
        when(departmentService.getDepartmentById(3L, 7L))
                .thenReturn(departmentForHospital());

        mockMvc.perform(get("/api/hospitals/3/departments/7"))
                .andExpect(status().isOk());

        verify(departmentService).getDepartmentById(3L, 7L);
    }

    @Test
    void updatesDepartmentWithinHospitalRoute() throws Exception {
        when(departmentService.updateDepartment(
                eq(3L), eq(7L), any(DepartmentRequest.class)))
                .thenReturn(departmentForHospital());

        mockMvc.perform(put("/api/hospitals/3/departments/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hospitalId": 3,
                                  "name": "Emergency",
                                  "code": "ER",
                                  "description": "Emergency care",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isOk());

        verify(departmentService).updateDepartment(
                eq(3L), eq(7L), any(DepartmentRequest.class));
    }

    @Test
    void deletesDepartmentWithinHospitalRoute() throws Exception {
        mockMvc.perform(delete("/api/hospitals/3/departments/7"))
                .andExpect(status().isNoContent());

        verify(departmentService).deleteDepartment(3L, 7L);
    }

    private Department departmentForHospital() {
        Department department = new Department();
        Hospital hospital = new Hospital();
        department.setHospital(hospital);
        department.setName("Emergency");
        department.setCode("ER");
        department.setStatus(DepartmentStatus.ACTIVE);
        return department;
    }
}
