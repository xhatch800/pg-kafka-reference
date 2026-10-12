package com.example.sample.controller;

import static com.example.sample.support.TestData.createEmployeeRequest;
import static com.example.sample.support.TestData.employeeResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.sample.dto.request.CreateEmployeeRequest;
import com.example.sample.dto.request.TerminateEmployeeRequest;
import com.example.sample.dto.response.EmployeeResponse;
import com.example.sample.exception.ConflictException;
import com.example.sample.service.EmployeeService;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class EmployeeControllerTest {

    @Mock
    EmployeeService employeeService;

    @InjectMocks
    EmployeeController controller;

    @Test
    void createReturns201WithLocationAndBody() {
        CreateEmployeeRequest request = createEmployeeRequest(3L);
        EmployeeResponse created = employeeResponse(7L, 3L);
        when(employeeService.create(request)).thenReturn(created);

        ResponseEntity<EmployeeResponse> response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isEqualTo(URI.create("/api/employees/7"));
        assertThat(response.getBody()).isSameAs(created);
    }

    @Test
    void findAllDelegatesToService() {
        List<EmployeeResponse> all = List.of(employeeResponse(7L, 3L));
        when(employeeService.findAll()).thenReturn(all);

        assertThat(controller.findAll()).isSameAs(all);
    }

    @Test
    void findByIdDelegatesToService() {
        EmployeeResponse employee = employeeResponse(7L, 3L);
        when(employeeService.findById(7L)).thenReturn(employee);

        assertThat(controller.findById(7L)).isSameAs(employee);
    }

    @Test
    void requestTerminationReturns202AndDelegates() {
        TerminateEmployeeRequest request = new TerminateEmployeeRequest("Restructuring", LocalDate.of(2026, 10, 31));

        ResponseEntity<Void> response = controller.requestTermination(7L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNull();
        verify(employeeService).requestTermination(7L, request);
    }

    @Test
    void requestTerminationPropagatesConflict() {
        TerminateEmployeeRequest request = new TerminateEmployeeRequest("Restructuring", null);
        doThrow(new ConflictException("Employee 7 is already terminated"))
                .when(employeeService).requestTermination(7L, request);

        assertThatThrownBy(() -> controller.requestTermination(7L, request)).isInstanceOf(ConflictException.class);
    }
}
