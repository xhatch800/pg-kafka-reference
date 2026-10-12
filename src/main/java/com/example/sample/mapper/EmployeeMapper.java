package com.example.sample.mapper;

import static com.example.sample.mapper.MappingSupport.normalizeEmail;
import static com.example.sample.mapper.MappingSupport.trim;
import static com.example.sample.mapper.MappingSupport.trimToNull;

import com.example.sample.dto.request.CreateEmployeeRequest;
import com.example.sample.dto.response.EmployeeResponse;
import com.example.sample.entity.DepartmentEntity;
import com.example.sample.entity.EmployeeEntity;
import com.example.sample.entity.EmployeeStatus;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper
        implements DtoMapper<CreateEmployeeRequest, DepartmentEntity, EmployeeEntity, EmployeeResponse> {

    @Override
    public EmployeeEntity toEntity(CreateEmployeeRequest request, DepartmentEntity department) {
        EmployeeEntity entity = new EmployeeEntity();
        entity.setFirstName(trim(request.firstName()));
        entity.setLastName(trim(request.lastName()));
        entity.setEmail(normalizeEmail(request.email()));
        entity.setJobTitle(trimToNull(request.jobTitle()));
        entity.setHireDate(request.hireDate());
        entity.setSalary(request.salary());
        entity.setDepartment(department);
        entity.setStatus(EmployeeStatus.ACTIVE);
        return entity;
    }

    @Override
    public EmployeeResponse toResponse(EmployeeEntity entity) {
        DepartmentEntity department = entity.getDepartment();
        return new EmployeeResponse(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getJobTitle(),
                entity.getHireDate(),
                entity.getSalary(),
                department == null ? null : department.getId(),
                department == null ? null : department.getName(),
                entity.getStatus() == null ? null : entity.getStatus().name(),
                entity.getTerminatedAt(),
                entity.getTerminationReason(),
                entity.getCreatedAt(),
                entity.getModifiedAt());
    }
}
