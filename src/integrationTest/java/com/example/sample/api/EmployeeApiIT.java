package com.example.sample.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sample.dto.request.CreateEmployeeRequest;
import com.example.sample.dto.response.DepartmentResponse;
import com.example.sample.dto.response.EmployeeResponse;
import com.example.sample.support.AbstractApiIT;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class EmployeeApiIT extends AbstractApiIT {

    @Test
    void createThenGetAndList() {
        DepartmentResponse department = createDepartment(unique("Engineering"));
        String email = unique("ada") + "@example.com";

        EmployeeResponse created = createEmployee(department.id(), email);

        assertThat(created.id()).isNotNull();
        assertThat(created.email()).isEqualTo(email);
        assertThat(created.departmentId()).isEqualTo(department.id());
        assertThat(created.departmentName()).isEqualTo(department.name());
        assertThat(created.status()).isEqualTo("ACTIVE");
        assertThat(created.salary()).isEqualByComparingTo("85000.00");
        assertThat(created.createdAt()).isNotNull();

        assertThat(getEmployee(created.id()))
                .usingRecursiveComparison()
                .ignoringFields("createdAt", "modifiedAt")
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(created);

        List<EmployeeResponse> all = client.get().uri("/api/employees")
                .retrieve().body(new ParameterizedTypeReference<>() { });
        assertThat(all).extracting(EmployeeResponse::id).contains(created.id());
    }

    @Test
    void emailIsStoredNormalized() {
        DepartmentResponse department = createDepartment(unique("Ops"));
        String local = unique("Grace");

        EmployeeResponse created = createEmployee(department.id(), local.toUpperCase() + "@Example.COM");

        assertThat(created.email()).isEqualTo(local.toLowerCase() + "@example.com");
    }

    @Test
    void duplicateEmailIgnoringCaseIsRejected() {
        DepartmentResponse department = createDepartment(unique("Sales"));
        String email = unique("dup") + "@example.com";
        createEmployee(department.id(), email);

        ResponseEntity<Map<String, Object>> response = client.post().uri("/api/employees")
                .body(new CreateEmployeeRequest(
                        "Other", "Person", email.toUpperCase(), null, LocalDate.of(2024, 1, 15), null,
                        department.id()))
                .retrieve().toEntity(PROBLEM);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void emailWithSurroundingWhitespaceIsRejectedAsInvalid() {
        DepartmentResponse department = createDepartment(unique("Support"));

        ResponseEntity<Map<String, Object>> response = client.post().uri("/api/employees")
                .body(new CreateEmployeeRequest(
                        "Other", "Person", "  " + unique("pad") + "@example.com ", null, LocalDate.of(2024, 1, 15),
                        null, department.id()))
                .retrieve().toEntity(PROBLEM);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("errors");
    }

    @Test
    void unknownDepartmentReturns404() {
        ResponseEntity<Map<String, Object>> response = client.post().uri("/api/employees")
                .body(new CreateEmployeeRequest(
                        "Ada", "Lovelace", unique("x") + "@example.com", null, LocalDate.of(2024, 1, 15),
                        new BigDecimal("1.00"), 987_654_321L))
                .retrieve().toEntity(PROBLEM);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("detail", "Department 987654321 not found");
    }

    @Test
    void invalidRequestReturns400WithFieldErrors() {
        ResponseEntity<Map<String, Object>> response = client.post().uri("/api/employees")
                .body(Map.of("firstName", "Ada"))
                .retrieve().toEntity(PROBLEM);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, Object> errors = (Map<String, Object>) response.getBody().get("errors");
        assertThat(errors).containsKeys("lastName", "email", "hireDate", "departmentId");
    }

    @Test
    void unknownEmployeeReturns404() {
        ResponseEntity<Map<String, Object>> response = client.get().uri("/api/employees/{id}", 987_654_321L)
                .retrieve().toEntity(PROBLEM);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
