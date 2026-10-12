package com.example.sample.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sample.TestcontainersConfiguration;
import com.example.sample.dto.request.CreateDepartmentRequest;
import com.example.sample.dto.request.CreateEmployeeRequest;
import com.example.sample.dto.response.DepartmentResponse;
import com.example.sample.dto.response.EmployeeResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

/** Boots the whole app against Testcontainers Postgres + Kafka and talks to it over real HTTP. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
public abstract class AbstractApiIT {

    protected static final ParameterizedTypeReference<Map<String, Object>> PROBLEM =
            new ParameterizedTypeReference<>() { };

    @LocalServerPort
    int port;

    protected RestClient client;

    @BeforeEach
    void setUpClient() {
        client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> { })
                .build();
    }

    /** Test data is shared across tests in one database, so names and emails must be unique. */
    protected static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected DepartmentResponse createDepartment(String name) {
        ResponseEntity<DepartmentResponse> response = client.post().uri("/api/departments")
                .body(new CreateDepartmentRequest(name, "Toronto"))
                .retrieve()
                .toEntity(DepartmentResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    protected EmployeeResponse createEmployee(Long departmentId, String email) {
        ResponseEntity<EmployeeResponse> response = client.post().uri("/api/employees")
                .body(new CreateEmployeeRequest(
                        "Ada", "Lovelace", email, "Engineer", LocalDate.of(2024, 1, 15),
                        new BigDecimal("85000.00"), departmentId))
                .retrieve()
                .toEntity(EmployeeResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    protected EmployeeResponse getEmployee(Long id) {
        return client.get().uri("/api/employees/{id}", id).retrieve().body(EmployeeResponse.class);
    }
}
