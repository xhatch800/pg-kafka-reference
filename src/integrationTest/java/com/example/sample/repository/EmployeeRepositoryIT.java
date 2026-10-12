package com.example.sample.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.sample.PostgresContainerConfiguration;
import com.example.sample.entity.DepartmentEntity;
import com.example.sample.entity.EmployeeEntity;
import com.example.sample.entity.EmployeeStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresContainerConfiguration.class)
class EmployeeRepositoryIT {

    @Autowired
    EmployeeRepository employeeRepository;

    @Autowired
    DepartmentRepository departmentRepository;

    @Autowired
    EntityManager entityManager;

    DepartmentEntity engineering;
    DepartmentEntity finance;

    @BeforeEach
    void setUp() {
        engineering = departmentRepository.saveAndFlush(department("Engineering"));
        finance = departmentRepository.saveAndFlush(department("Finance"));
    }

    @Test
    void saveRoundTripsAllColumns() {
        EmployeeEntity saved = employeeRepository.saveAndFlush(employee("ada@example.com", engineering));
        entityManager.clear();

        EmployeeEntity loaded = employeeRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getFirstName()).isEqualTo("Ada");
        assertThat(loaded.getLastName()).isEqualTo("Lovelace");
        assertThat(loaded.getEmail()).isEqualTo("ada@example.com");
        assertThat(loaded.getJobTitle()).isEqualTo("Engineer");
        assertThat(loaded.getHireDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(loaded.getSalary()).isEqualByComparingTo("85000.00");
        assertThat(loaded.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(loaded.getTerminatedAt()).isNull();
        assertThat(loaded.getTerminationReason()).isNull();
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getModifiedAt()).isNotNull();
    }

    @Test
    void duplicateEmailViolatesUniqueConstraint() {
        employeeRepository.saveAndFlush(employee("dup@example.com", engineering));

        assertThatThrownBy(() -> employeeRepository.saveAndFlush(employee("dup@example.com", finance)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsByEmailIgnoreCaseMatchesRegardlessOfCase() {
        employeeRepository.saveAndFlush(employee("grace@example.com", engineering));

        assertThat(employeeRepository.existsByEmailIgnoreCase("GRACE@example.com")).isTrue();
        assertThat(employeeRepository.existsByEmailIgnoreCase("nobody@example.com")).isFalse();
    }

    @Test
    void findByDepartmentIdReturnsOnlyThatDepartmentsEmployeesInIdOrder() {
        EmployeeEntity first = employeeRepository.saveAndFlush(employee("a@example.com", engineering));
        employeeRepository.saveAndFlush(employee("b@example.com", finance));
        EmployeeEntity third = employeeRepository.saveAndFlush(employee("c@example.com", engineering));

        assertThat(employeeRepository.findByDepartmentIdOrderByIdAsc(engineering.getId()))
                .extracting(EmployeeEntity::getId)
                .containsExactly(first.getId(), third.getId());
    }

    @Test
    void findWithDepartmentByIdFetchesDepartment() {
        EmployeeEntity saved = employeeRepository.saveAndFlush(employee("fetch@example.com", engineering));
        entityManager.clear();

        EmployeeEntity loaded = employeeRepository.findWithDepartmentById(saved.getId()).orElseThrow();

        assertThat(Hibernate.isInitialized(loaded.getDepartment())).isTrue();
        assertThat(loaded.getDepartment().getName()).isEqualTo("Engineering");
    }

    @Test
    void statusDefaultsToActiveAtDatabaseLevel() {
        insertRaw("default@example.com", engineering.getId(), "1.00", null);

        Object status = entityManager
                .createNativeQuery("select status from employees where email = 'default@example.com'")
                .getSingleResult();
        assertThat(status).isEqualTo("ACTIVE");
    }

    @Test
    void foreignKeyRejectsUnknownDepartment() {
        assertThatThrownBy(() -> insertRaw("fk@example.com", 999_999L, "1.00", "ACTIVE"))
                .isInstanceOf(PersistenceException.class)
                .rootCause().hasMessageContaining("fk_employees_department");
    }

    @Test
    void departmentIsRequired() {
        assertThatThrownBy(() -> insertRaw("nodept@example.com", null, "1.00", "ACTIVE"))
                .isInstanceOf(PersistenceException.class)
                .rootCause().hasMessageContaining("department_id");
    }

    @Test
    void negativeSalaryIsRejected() {
        assertThatThrownBy(() -> insertRaw("neg@example.com", engineering.getId(), "-1.00", "ACTIVE"))
                .isInstanceOf(PersistenceException.class)
                .rootCause().hasMessageContaining("ck_employees_salary_non_negative");
    }

    @Test
    void unknownStatusIsRejected() {
        assertThatThrownBy(() -> insertRaw("status@example.com", engineering.getId(), "1.00", "RETIRED"))
                .isInstanceOf(PersistenceException.class)
                .rootCause().hasMessageContaining("ck_employees_status");
    }

    /** Inserts with plain SQL so DB constraints are exercised without JPA in the way. Null status uses the column default. */
    private void insertRaw(String email, Long departmentId, String salary, String status) {
        String statusColumn = status == null ? "" : ", status";
        String statusValue = status == null ? "" : ", :status";
        var query = entityManager.createNativeQuery("""
                insert into employees (first_name, last_name, email, hire_date, salary, department_id, created_at, modified_at%s)
                values ('Raw', 'Insert', :email, date '2024-01-15', cast(:salary as numeric), cast(:departmentId as bigint), now(), now()%s)
                """.formatted(statusColumn, statusValue))
                .setParameter("email", email)
                .setParameter("salary", salary)
                .setParameter("departmentId", departmentId);
        if (status != null) {
            query.setParameter("status", status);
        }
        query.executeUpdate();
    }

    private static DepartmentEntity department(String name) {
        DepartmentEntity department = new DepartmentEntity();
        department.setName(name);
        return department;
    }

    private static EmployeeEntity employee(String email, DepartmentEntity department) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setFirstName("Ada");
        employee.setLastName("Lovelace");
        employee.setEmail(email);
        employee.setJobTitle("Engineer");
        employee.setHireDate(LocalDate.of(2024, 1, 15));
        employee.setSalary(new BigDecimal("85000.00"));
        employee.setDepartment(department);
        employee.setStatus(EmployeeStatus.ACTIVE);
        return employee;
    }
}
