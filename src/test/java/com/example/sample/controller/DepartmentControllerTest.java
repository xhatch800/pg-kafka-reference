package com.example.sample.controller;

import static com.example.sample.support.TestData.createDepartmentRequest;
import static com.example.sample.support.TestData.departmentResponse;
import static com.example.sample.support.TestData.employeeResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.sample.dto.request.CreateDepartmentRequest;
import com.example.sample.dto.response.DepartmentResponse;
import com.example.sample.dto.response.EmployeeResponse;
import com.example.sample.exception.NotFoundException;
import com.example.sample.service.DepartmentService;
import com.example.sample.service.EmployeeService;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class DepartmentControllerTest {

    @Mock
    DepartmentService departmentService;

    @Mock
    EmployeeService employeeService;

    @InjectMocks
    DepartmentController controller;

    @Test
    void createReturns201WithLocationAndBody() {
        CreateDepartmentRequest request = createDepartmentRequest();
        DepartmentResponse created = departmentResponse(5L, "Engineering");
        when(departmentService.create(request)).thenReturn(created);

        ResponseEntity<DepartmentResponse> response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isEqualTo(URI.create("/api/departments/5"));
        assertThat(response.getBody()).isSameAs(created);
    }

    @Test
    void findAllDelegatesToService() {
        List<DepartmentResponse> all = List.of(departmentResponse(1L, "A"));
        when(departmentService.findAll()).thenReturn(all);

        assertThat(controller.findAll()).isSameAs(all);
    }

    @Test
    void findByIdDelegatesToService() {
        DepartmentResponse department = departmentResponse(1L, "A");
        when(departmentService.findById(1L)).thenReturn(department);

        assertThat(controller.findById(1L)).isSameAs(department);
    }

    @Test
    void findByIdPropagatesNotFound() {
        when(departmentService.findById(9L)).thenThrow(NotFoundException.of("Department", 9L));

        assertThatThrownBy(() -> controller.findById(9L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void findEmployeesDelegatesToEmployeeService() {
        List<EmployeeResponse> employees = List.of(employeeResponse(7L, 1L));
        when(employeeService.findByDepartmentId(1L)).thenReturn(employees);

        assertThat(controller.findEmployees(1L)).isSameAs(employees);
    }
}
