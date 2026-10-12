package com.example.sample.mapper;

import static com.example.sample.support.TestData.CREATED_AT;
import static com.example.sample.support.TestData.HIRE_DATE;
import static com.example.sample.support.TestData.MODIFIED_AT;
import static com.example.sample.support.TestData.createEmployeeRequest;
import static com.example.sample.support.TestData.department;
import static com.example.sample.support.TestData.employee;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.sample.dto.request.CreateEmployeeRequest;
import com.example.sample.dto.response.EmployeeResponse;
import com.example.sample.entity.DepartmentEntity;
import com.example.sample.entity.EmployeeEntity;
import com.example.sample.entity.EmployeeStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmployeeMapperTest {

    private final EmployeeMapper mapper = new EmployeeMapper();
    private final DepartmentEntity engineering = department(3L, "Engineering");

    @Test
    void toEntityCopiesFieldsAttachesDepartmentAndStartsActive() {
        EmployeeEntity entity = mapper.toEntity(createEmployeeRequest(3L), engineering);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getFirstName()).isEqualTo("Ada");
        assertThat(entity.getLastName()).isEqualTo("Lovelace");
        assertThat(entity.getEmail()).isEqualTo("ada@example.com");
        assertThat(entity.getJobTitle()).isEqualTo("Engineer");
        assertThat(entity.getHireDate()).isEqualTo(HIRE_DATE);
        assertThat(entity.getSalary()).isEqualByComparingTo("85000.00");
        assertThat(entity.getDepartment()).isSameAs(engineering);
        assertThat(entity.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(entity.getTerminatedAt()).isNull();
        assertThat(entity.getTerminationReason()).isNull();
    }

    @Test
    void toEntityNormalizesTextAndEmail() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                " Ada ", " Lovelace ", "  Ada@Example.COM ", "   ", HIRE_DATE, null, 3L);

        EmployeeEntity entity = mapper.toEntity(request, engineering);

        assertThat(entity.getFirstName()).isEqualTo("Ada");
        assertThat(entity.getLastName()).isEqualTo("Lovelace");
        assertThat(entity.getEmail()).isEqualTo("ada@example.com");
        assertThat(entity.getJobTitle()).isNull();
    }

    @Test
    void toEntityKeepsMissingOptionalFieldsNull() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Ada", "Lovelace", "ada@example.com", null, HIRE_DATE, null, 3L);

        EmployeeEntity entity = mapper.toEntity(request, engineering);

        assertThat(entity.getJobTitle()).isNull();
        assertThat(entity.getSalary()).isNull();
    }

    @Test
    void toResponseCopiesAllFieldsIncludingDepartment() {
        EmployeeResponse response = mapper.toResponse(employee(7L, engineering));

        assertThat(response).isEqualTo(new EmployeeResponse(
                7L, "Ada", "Lovelace", "ada@example.com", "Engineer", HIRE_DATE, new BigDecimal("85000.00"),
                3L, "Engineering", "ACTIVE", null, null, CREATED_AT, MODIFIED_AT));
    }

    @Test
    void toResponseIncludesTerminationFields() {
        EmployeeEntity entity = employee(7L, engineering);
        entity.setStatus(EmployeeStatus.TERMINATED);
        entity.setTerminatedAt(LocalDate.of(2026, 10, 31));
        entity.setTerminationReason("Restructuring");

        EmployeeResponse response = mapper.toResponse(entity);

        assertThat(response.status()).isEqualTo("TERMINATED");
        assertThat(response.terminatedAt()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(response.terminationReason()).isEqualTo("Restructuring");
    }

    @Test
    void toResponseToleratesMissingDepartment() {
        EmployeeResponse response = mapper.toResponse(employee(7L, null));

        assertThat(response.departmentId()).isNull();
        assertThat(response.departmentName()).isNull();
    }

    @Test
    void toResponseListMapsEachEntityInOrder() {
        List<EmployeeResponse> responses = mapper.toResponseList(
                List.of(employee(1L, engineering), employee(2L, engineering)));

        assertThat(responses).extracting(EmployeeResponse::id).containsExactly(1L, 2L);
    }
}
