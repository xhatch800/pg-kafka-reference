package com.example.sample.service;

import static com.example.sample.support.TestData.createDepartmentRequest;
import static com.example.sample.support.TestData.department;
import static com.example.sample.support.TestData.departmentResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.sample.dto.request.CreateDepartmentRequest;
import com.example.sample.dto.response.DepartmentResponse;
import com.example.sample.entity.DepartmentEntity;
import com.example.sample.exception.ConflictException;
import com.example.sample.exception.NotFoundException;
import com.example.sample.mapper.DepartmentMapper;
import com.example.sample.repository.DepartmentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    DepartmentRepository departmentRepository;

    @Mock
    DepartmentMapper departmentMapper;

    @InjectMocks
    DepartmentService departmentService;

    @Test
    void createSavesMappedEntityAndReturnsResponse() {
        CreateDepartmentRequest request = createDepartmentRequest();
        DepartmentEntity mapped = department(null, "Engineering");
        DepartmentEntity saved = department(1L, "Engineering");
        DepartmentResponse response = departmentResponse(1L, "Engineering");
        when(departmentMapper.toEntity(request, null)).thenReturn(mapped);
        when(departmentRepository.existsByNameIgnoreCase("Engineering")).thenReturn(false);
        when(departmentRepository.save(mapped)).thenReturn(saved);
        when(departmentMapper.toResponse(saved)).thenReturn(response);

        assertThat(departmentService.create(request)).isSameAs(response);
    }

    @Test
    void createRejectsDuplicateNameIgnoringCase() {
        CreateDepartmentRequest request = new CreateDepartmentRequest("engineering", null);
        when(departmentMapper.toEntity(request, null)).thenReturn(department(null, "engineering"));
        when(departmentRepository.existsByNameIgnoreCase("engineering")).thenReturn(true);

        assertThatThrownBy(() -> departmentService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Department with name 'engineering' already exists");
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void findByIdReturnsMappedResponse() {
        DepartmentEntity entity = department(1L, "Engineering");
        DepartmentResponse response = departmentResponse(1L, "Engineering");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(departmentMapper.toResponse(entity)).thenReturn(response);

        assertThat(departmentService.findById(1L)).isSameAs(response);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(departmentRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.findById(42L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Department 42 not found");
    }

    @Test
    void findAllReturnsMappedListInRepositoryOrder() {
        List<DepartmentEntity> entities = List.of(department(1L, "A"), department(2L, "B"));
        List<DepartmentResponse> responses = List.of(departmentResponse(1L, "A"), departmentResponse(2L, "B"));
        when(departmentRepository.findAllByOrderByIdAsc()).thenReturn(entities);
        when(departmentMapper.toResponseList(entities)).thenReturn(responses);

        assertThat(departmentService.findAll()).isSameAs(responses);
    }
}
