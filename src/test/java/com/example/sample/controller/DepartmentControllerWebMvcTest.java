package com.example.sample.controller;

import static com.example.sample.support.TestData.departmentResponse;
import static com.example.sample.support.TestData.employeeResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.sample.dto.request.CreateDepartmentRequest;
import com.example.sample.exception.ConflictException;
import com.example.sample.exception.NotFoundException;
import com.example.sample.service.DepartmentService;
import com.example.sample.service.EmployeeService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DepartmentController.class)
class DepartmentControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DepartmentService departmentService;

    @MockitoBean
    EmployeeService employeeService;

    @Test
    void postCreatesDepartment() throws Exception {
        when(departmentService.create(new CreateDepartmentRequest("Engineering", "Toronto")))
                .thenReturn(departmentResponse(5L, "Engineering"));

        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Engineering", "location": "Toronto"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/departments/5"))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Engineering"))
                .andExpect(jsonPath("$.location").value("Toronto"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-01T09:00:00Z"))
                .andExpect(jsonPath("$.modifiedAt").value("2026-10-02T09:00:00Z"));
    }

    @Test
    void postRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").exists());
        verifyNoInteractions(departmentService);
    }

    @Test
    void postRejectsTooLongFields() throws Exception {
        String longText = "x".repeat(101);
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + longText + "\", \"location\": \"" + longText + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.location").exists());
    }

    @Test
    void postRejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        verifyNoInteractions(departmentService);
    }

    @Test
    void postReturns409OnDuplicateName() throws Exception {
        when(departmentService.create(any()))
                .thenThrow(new ConflictException("Department with name 'Engineering' already exists"));

        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Engineering"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Department with name 'Engineering' already exists"));
    }

    @Test
    void postReturns409OnDataIntegrityViolation() throws Exception {
        when(departmentService.create(any())).thenThrow(new DataIntegrityViolationException("uk_departments_name"));

        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Engineering"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("The request conflicts with existing data."));
    }

    @Test
    void getAllReturnsList() throws Exception {
        when(departmentService.findAll())
                .thenReturn(List.of(departmentResponse(1L, "A"), departmentResponse(2L, "B")));

        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("A"))
                .andExpect(jsonPath("$[1].name").value("B"));
    }

    @Test
    void getByIdReturnsDepartment() throws Exception {
        when(departmentService.findById(1L)).thenReturn(departmentResponse(1L, "A"));

        mockMvc.perform(get("/api/departments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getByIdReturns404() throws Exception {
        when(departmentService.findById(9L)).thenThrow(NotFoundException.of("Department", 9L));

        mockMvc.perform(get("/api/departments/9"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Department 9 not found"));
    }

    @Test
    void getByIdRejectsNonNumericId() throws Exception {
        mockMvc.perform(get("/api/departments/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        verifyNoInteractions(departmentService);
    }

    @Test
    void getEmployeesReturnsList() throws Exception {
        when(employeeService.findByDepartmentId(1L)).thenReturn(List.of(employeeResponse(7L, 1L)));

        mockMvc.perform(get("/api/departments/1/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].departmentId").value(1));
    }

    @Test
    void getEmployeesReturns404ForUnknownDepartment() throws Exception {
        when(employeeService.findByDepartmentId(9L)).thenThrow(NotFoundException.of("Department", 9L));

        mockMvc.perform(get("/api/departments/9/employees"))
                .andExpect(status().isNotFound());
    }
}
