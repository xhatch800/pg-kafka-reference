package com.example.sample.repository;

import com.example.sample.entity.DepartmentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Long> {

    boolean existsByNameIgnoreCase(String name);

    List<DepartmentEntity> findAllByOrderByIdAsc();
}
