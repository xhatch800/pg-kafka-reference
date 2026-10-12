package com.example.sample.controller;

import static com.example.sample.support.TestData.employeeResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.sample.dto.request.TerminateEmployeeRequest;
import com.example.sample.exception.ConflictException;
import com.example.sample.exception.NotFoundException;
import com.example.sample.service.EmployeeService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerWebMvcTest {

    private static final String VALID_BODY = """
            {
              "firstName": "Ada",
              "lastName": "Lovelace",
              "email": "ada@example.com",
              "jobTitle": "Engineer",
              "hireDate": "2024-01-15",
              "salary": 85000.00,
              "departmentId": 3
            }
            """;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    EmployeeService employeeService;

    @Test
    void postCreatesEmployee() throws Exception {
        when(employeeService.create(any())).thenReturn(employeeResponse(7L, 3L));

        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/employees/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.hireDate").value("2024-01-15"))
                .andExpect(jsonPath("$.departmentId").value(3))
                .andExpect(jsonPath("$.departmentName").value("Engineering"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void postReportsEveryMissingRequiredField() throws Exception {
        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.lastName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.hireDate").exists())
                .andExpect(jsonPath("$.errors.departmentId").exists());
        verifyNoInteractions(employeeService);
    }

    @Test
    void postRejectsInvalidValues() throws Exception {
        String futureDate = LocalDate.now().plusDays(1).toString();
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Ada",
                                  "lastName": "Lovelace",
                                  "email": "not-an-email",
                                  "hireDate": "%s",
                                  "salary": -1,
                                  "departmentId": 3
                                }
                                """.formatted(futureDate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.hireDate").exists())
                .andExpect(jsonPath("$.errors.salary").exists());
    }

    @Test
    void postRejectsSalaryWithTooManyDecimals() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("85000.00", "85000.123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.salary").exists());
    }

    @Test
    void postRejectsWrongJsonType() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("85000.00", "\"abc\"")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        verifyNoInteractions(employeeService);
    }

    @Test
    void postReturns404ForUnknownDepartment() throws Exception {
        when(employeeService.create(any())).thenThrow(NotFoundException.of("Department", 3L));

        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Department 3 not found"));
    }

    @Test
    void postReturns409ForDuplicateEmail() throws Exception {
        when(employeeService.create(any()))
                .thenThrow(new ConflictException("Employee with email 'ada@example.com' already exists"));

        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void getAllReturnsList() throws Exception {
        when(employeeService.findAll()).thenReturn(List.of(employeeResponse(7L, 3L)));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(7));
    }

    @Test
    void getByIdReturnsEmployee() throws Exception {
        when(employeeService.findById(7L)).thenReturn(employeeResponse(7L, 3L));

        mockMvc.perform(get("/api/employees/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ada"));
    }

    @Test
    void getByIdReturns404() throws Exception {
        when(employeeService.findById(7L)).thenThrow(NotFoundException.of("Employee", 7L));

        mockMvc.perform(get("/api/employees/7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Employee 7 not found"));
    }

    @Test
    void getByIdRejectsNonNumericId() throws Exception {
        mockMvc.perform(get("/api/employees/abc"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(employeeService);
    }

    @Test
    void terminationReturns202() throws Exception {
        mockMvc.perform(post("/api/employees/7/termination")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Restructuring", "effectiveDate": "2026-10-31"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));
        verify(employeeService).requestTermination(
                7L, new TerminateEmployeeRequest("Restructuring", LocalDate.of(2026, 10, 31)));
    }

    @Test
    void terminationAllowsMissingEffectiveDate() throws Exception {
        mockMvc.perform(post("/api/employees/7/termination")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Restructuring"}
                                """))
                .andExpect(status().isAccepted());
        verify(employeeService).requestTermination(7L, new TerminateEmployeeRequest("Restructuring", null));
    }

    @Test
    void terminationRequiresReason() throws Exception {
        mockMvc.perform(post("/api/employees/7/termination")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": " "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").exists());
        verifyNoInteractions(employeeService);
    }

    @Test
    void terminationRejectsBadDateFormat() throws Exception {
        mockMvc.perform(post("/api/employees/7/termination")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Restructuring", "effectiveDate": "31-10-2026"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        verifyNoInteractions(employeeService);
    }

    @Test
    void terminationReturns404ForUnknownEmployee() throws Exception {
        doThrow(NotFoundException.of("Employee", 7L))
                .when(employeeService).requestTermination(eq(7L), any());

        mockMvc.perform(post("/api/employees/7/termination")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Restructuring"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void terminationReturns409WhenAlreadyTerminated() throws Exception {
        doThrow(new ConflictException("Employee 7 is already terminated"))
                .when(employeeService).requestTermination(eq(7L), any());

        mockMvc.perform(post("/api/employees/7/termination")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Restructuring"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Employee 7 is already terminated"));
    }

    @Test
    void postRejectsEmailWithSurroundingWhitespace() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("\"ada@example.com\"", "\"  ada@example.com \"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
        verifyNoInteractions(employeeService);
    }

    @Test
    void unexpectedFailureReturns500ProblemDetailWithoutInternals() throws Exception {
        when(employeeService.findById(7L)).thenThrow(new IllegalStateException("connection refused: db-host:5432"));

        mockMvc.perform(get("/api/employees/7"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."));
    }
}
