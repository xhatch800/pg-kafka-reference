package com.example.sample.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.sample.PostgresContainerConfiguration;
import com.example.sample.entity.DepartmentEntity;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresContainerConfiguration.class)
class DepartmentRepositoryIT {

    @Autowired
    DepartmentRepository departmentRepository;

    @Test
    void saveAssignsIdAndTimestamps() {
        DepartmentEntity saved = departmentRepository.saveAndFlush(department("Engineering"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getModifiedAt()).isNotNull();
        assertThat(departmentRepository.findById(saved.getId()))
                .get()
                .extracting(DepartmentEntity::getName, DepartmentEntity::getLocation)
                .containsExactly("Engineering", "Toronto");
    }

    @Test
    void duplicateNameViolatesUniqueConstraint() {
        departmentRepository.saveAndFlush(department("Finance"));

        assertThatThrownBy(() -> departmentRepository.saveAndFlush(department("Finance")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nameDifferingOnlyByCaseViolatesUniqueIndex() {
        departmentRepository.saveAndFlush(department("Audit"));

        assertThatThrownBy(() -> departmentRepository.saveAndFlush(department("AUDIT")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsByNameIgnoreCaseMatchesRegardlessOfCase() {
        departmentRepository.saveAndFlush(department("Marketing"));

        assertThat(departmentRepository.existsByNameIgnoreCase("MARKETING")).isTrue();
        assertThat(departmentRepository.existsByNameIgnoreCase("Sales")).isFalse();
    }

    @Test
    void findAllByOrderByIdAscReturnsAscendingIds() {
        DepartmentEntity first = departmentRepository.saveAndFlush(department("Legal"));
        DepartmentEntity second = departmentRepository.saveAndFlush(department("Support"));

        List<DepartmentEntity> all = departmentRepository.findAllByOrderByIdAsc();

        assertThat(all).isSortedAccordingTo(Comparator.comparing(DepartmentEntity::getId));
        assertThat(all).extracting(DepartmentEntity::getId).contains(first.getId(), second.getId());
    }

    private static DepartmentEntity department(String name) {
        DepartmentEntity department = new DepartmentEntity();
        department.setName(name);
        department.setLocation("Toronto");
        return department;
    }
}
