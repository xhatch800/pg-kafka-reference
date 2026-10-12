package com.example.sample.repository;

import com.example.sample.entity.EmployeeEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<EmployeeEntity, Long> {

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "department")
    List<EmployeeEntity> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = "department")
    List<EmployeeEntity> findByDepartmentIdOrderByIdAsc(Long departmentId);

    @EntityGraph(attributePaths = "department")
    Optional<EmployeeEntity> findWithDepartmentById(Long id);
}
