package com.example.sample.mapper;

import static com.example.sample.mapper.MappingSupport.trim;
import static com.example.sample.mapper.MappingSupport.trimToNull;

import com.example.sample.dto.request.CreateDepartmentRequest;
import com.example.sample.dto.response.DepartmentResponse;
import com.example.sample.entity.DepartmentEntity;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper
        implements DtoMapper<CreateDepartmentRequest, Void, DepartmentEntity, DepartmentResponse> {

    @Override
    public DepartmentEntity toEntity(CreateDepartmentRequest request, Void context) {
        DepartmentEntity entity = new DepartmentEntity();
        entity.setName(trim(request.name()));
        entity.setLocation(trimToNull(request.location()));
        return entity;
    }

    @Override
    public DepartmentResponse toResponse(DepartmentEntity entity) {
        return new DepartmentResponse(
                entity.getId(),
                entity.getName(),
                entity.getLocation(),
                entity.getCreatedAt(),
                entity.getModifiedAt());
    }
}
