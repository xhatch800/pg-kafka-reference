package com.example.sample.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sample.dto.request.CreateDepartmentRequest;
import com.example.sample.dto.response.DepartmentResponse;
import com.example.sample.dto.response.EmployeeResponse;
import com.example.sample.support.AbstractApiIT;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class DepartmentApiIT extends AbstractApiIT {

    @Test
    void createThenGetAndList() {
        String name = unique("Engineering");
        DepartmentResponse created = createDepartment(name);

        assertThat(created.id()).isNotNull();
        assertThat(created.createdAt()).isNotNull();

        DepartmentResponse fetched = client.get().uri("/api/departments/{id}", created.id())
                .retrieve().body(DepartmentResponse.class);

        // Timestamps are compared loosely: Postgres stores microseconds, the save response may carry nanoseconds.
        assertThat(fetched).usingRecursiveComparison().ignoringFields("createdAt", "modifiedAt").isEqualTo(created);

        List<DepartmentResponse> all = client.get().uri("/api/departments")
                .retrieve().body(new ParameterizedTypeReference<>() { });
        assertThat(all).extracting(DepartmentResponse::name).contains(name);
    }

    @Test
    void createReturnsLocationHeader() {
        ResponseEntity<DepartmentResponse> response = client.post().uri("/api/departments")
                .body(new CreateDepartmentRequest(unique("Ops"), null))
                .retrieve().toEntity(DepartmentResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation())
                .hasToString("/api/departments/" + response.getBody().id());
    }

    @Test
    void duplicateNameIgnoringCaseIsRejected() {
        String name = unique("Finance");
        createDepartment(name);

        ResponseEntity<Map<String, Object>> response = client.post().uri("/api/departments")
                .body(new CreateDepartmentRequest("  " + name.toUpperCase() + " ", null))
                .retrieve().toEntity(PROBLEM);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("status", 409);
    }

    @Test
    void invalidRequestReturns400WithFieldErrors() {
        ResponseEntity<Map<String, Object>> response = client.post().uri("/api/departments")
                .body(new CreateDepartmentRequest("", null))
                .retrieve().toEntity(PROBLEM);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("errors");
    }

    @Test
    void unknownDepartmentReturns404() {
        ResponseEntity<Map<String, Object>> response = client.get().uri("/api/departments/{id}", 987_654_321L)
                .retrieve().toEntity(PROBLEM);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("detail", "Department 987654321 not found");
    }

    @Test
    void listsEmployeesOfDepartment() {
        DepartmentResponse department = createDepartment(unique("Research"));
        EmployeeResponse employee = createEmployee(department.id(), unique("r") + "@example.com");

        List<EmployeeResponse> employees = client.get().uri("/api/departments/{id}/employees", department.id())
                .retrieve().body(new ParameterizedTypeReference<>() { });

        assertThat(employees).extracting(EmployeeResponse::id).containsExactly(employee.id());
    }

    @Test
    void listingEmployeesOfUnknownDepartmentReturns404() {
        ResponseEntity<Map<String, Object>> response = client.get()
                .uri("/api/departments/{id}/employees", 987_654_321L)
                .retrieve().toEntity(PROBLEM);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
