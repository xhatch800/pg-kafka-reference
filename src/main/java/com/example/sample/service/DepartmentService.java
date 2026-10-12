package com.example.sample.service;

import com.example.sample.dto.request.CreateDepartmentRequest;
import com.example.sample.dto.response.DepartmentResponse;
import com.example.sample.entity.DepartmentEntity;
import com.example.sample.exception.ConflictException;
import com.example.sample.exception.NotFoundException;
import com.example.sample.mapper.DepartmentMapper;
import com.example.sample.repository.DepartmentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    public DepartmentService(DepartmentRepository departmentRepository, DepartmentMapper departmentMapper) {
        this.departmentRepository = departmentRepository;
        this.departmentMapper = departmentMapper;
    }

    @Transactional
    public DepartmentResponse create(CreateDepartmentRequest request) {
        DepartmentEntity department = departmentMapper.toEntity(request, null);
        if (departmentRepository.existsByNameIgnoreCase(department.getName())) {
            throw new ConflictException("Department with name '" + department.getName() + "' already exists");
        }
        return departmentMapper.toResponse(departmentRepository.save(department));
    }

    public DepartmentResponse findById(Long id) {
        return departmentRepository.findById(id)
                .map(departmentMapper::toResponse)
                .orElseThrow(() -> NotFoundException.of("Department", id));
    }

    public List<DepartmentResponse> findAll() {
        return departmentMapper.toResponseList(departmentRepository.findAllByOrderByIdAsc());
    }
}
