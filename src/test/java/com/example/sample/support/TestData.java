package com.example.sample.support;

import com.example.sample.dto.request.CreateDepartmentRequest;
import com.example.sample.dto.request.CreateEmployeeRequest;
import com.example.sample.dto.response.DepartmentResponse;
import com.example.sample.dto.response.EmployeeResponse;
import com.example.sample.entity.DepartmentEntity;
import com.example.sample.entity.EmployeeEntity;
import com.example.sample.entity.EmployeeStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Shared, deterministic fixtures for unit tests. */
public final class TestData {

    public static final Instant CREATED_AT = Instant.parse("2026-10-01T09:00:00Z");
    public static final Instant MODIFIED_AT = Instant.parse("2026-10-02T09:00:00Z");
    public static final LocalDate HIRE_DATE = LocalDate.of(2024, 1, 15);

    private TestData() {
    }

    public static DepartmentEntity department(Long id, String name) {
        DepartmentEntity department = new DepartmentEntity();
        department.setId(id);
        department.setName(name);
        department.setLocation("Toronto");
        department.setCreatedAt(CREATED_AT);
        department.setModifiedAt(MODIFIED_AT);
        return department;
    }

    public static EmployeeEntity employee(Long id, DepartmentEntity department) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setId(id);
        employee.setFirstName("Ada");
        employee.setLastName("Lovelace");
        employee.setEmail("ada@example.com");
        employee.setJobTitle("Engineer");
        employee.setHireDate(HIRE_DATE);
        employee.setSalary(new BigDecimal("85000.00"));
        employee.setDepartment(department);
        employee.setStatus(EmployeeStatus.ACTIVE);
        employee.setCreatedAt(CREATED_AT);
        employee.setModifiedAt(MODIFIED_AT);
        return employee;
    }

    public static CreateDepartmentRequest createDepartmentRequest() {
        return new CreateDepartmentRequest("Engineering", "Toronto");
    }

    public static CreateEmployeeRequest createEmployeeRequest(Long departmentId) {
        return new CreateEmployeeRequest(
                "Ada", "Lovelace", "ada@example.com", "Engineer", HIRE_DATE, new BigDecimal("85000.00"), departmentId);
    }

    public static DepartmentResponse departmentResponse(Long id, String name) {
        return new DepartmentResponse(id, name, "Toronto", CREATED_AT, MODIFIED_AT);
    }

    public static EmployeeResponse employeeResponse(Long id, Long departmentId) {
        return new EmployeeResponse(
                id, "Ada", "Lovelace", "ada@example.com", "Engineer", HIRE_DATE, new BigDecimal("85000.00"),
                departmentId, "Engineering", "ACTIVE", null, null, CREATED_AT, MODIFIED_AT);
    }
}
