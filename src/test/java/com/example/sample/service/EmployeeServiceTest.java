package com.example.sample.service;

import static com.example.sample.support.TestData.createEmployeeRequest;
import static com.example.sample.support.TestData.department;
import static com.example.sample.support.TestData.employee;
import static com.example.sample.support.TestData.employeeResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.example.sample.dto.request.CreateEmployeeRequest;
import com.example.sample.dto.request.TerminateEmployeeRequest;
import com.example.sample.dto.response.EmployeeResponse;
import com.example.sample.entity.DepartmentEntity;
import com.example.sample.entity.EmployeeEntity;
import com.example.sample.entity.EmployeeStatus;
import com.example.sample.exception.ConflictException;
import com.example.sample.exception.NotFoundException;
import com.example.sample.mapper.EmployeeMapper;
import com.example.sample.messaging.EmployeeTerminationEvent;
import com.example.sample.messaging.EmployeeTerminationPublisher;
import com.example.sample.repository.DepartmentRepository;
import com.example.sample.repository.EmployeeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    EmployeeRepository employeeRepository;

    @Mock
    DepartmentRepository departmentRepository;

    @Mock
    EmployeeMapper employeeMapper;

    @Mock
    EmployeeTerminationPublisher terminationPublisher;

    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-11T10:00:00Z"), ZoneOffset.UTC);

    EmployeeService employeeService;

    final DepartmentEntity engineering = department(3L, "Engineering");

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeService(
                employeeRepository, departmentRepository, employeeMapper, terminationPublisher, CLOCK);
    }

    @Test
    void createResolvesDepartmentMapsChecksEmailAndSaves() {
        CreateEmployeeRequest request = createEmployeeRequest(3L);
        EmployeeEntity mapped = employee(null, engineering);
        EmployeeEntity saved = employee(7L, engineering);
        EmployeeResponse response = employeeResponse(7L, 3L);
        when(departmentRepository.findById(3L)).thenReturn(Optional.of(engineering));
        when(employeeMapper.toEntity(request, engineering)).thenReturn(mapped);
        when(employeeRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(employeeRepository.save(mapped)).thenReturn(saved);
        when(employeeMapper.toResponse(saved)).thenReturn(response);

        assertThat(employeeService.create(request)).isSameAs(response);
    }

    @Test
    void createThrowsWhenDepartmentMissing() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.create(createEmployeeRequest(99L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Department 99 not found");
        verifyNoInteractions(employeeMapper);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateEmail() {
        CreateEmployeeRequest request = createEmployeeRequest(3L);
        when(departmentRepository.findById(3L)).thenReturn(Optional.of(engineering));
        when(employeeMapper.toEntity(request, engineering)).thenReturn(employee(null, engineering));
        when(employeeRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Employee with email 'ada@example.com' already exists");
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void findByIdReturnsMappedResponse() {
        EmployeeEntity entity = employee(7L, engineering);
        EmployeeResponse response = employeeResponse(7L, 3L);
        when(employeeRepository.findWithDepartmentById(7L)).thenReturn(Optional.of(entity));
        when(employeeMapper.toResponse(entity)).thenReturn(response);

        assertThat(employeeService.findById(7L)).isSameAs(response);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(employeeRepository.findWithDepartmentById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.findById(7L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Employee 7 not found");
    }

    @Test
    void findAllReturnsMappedList() {
        List<EmployeeEntity> entities = List.of(employee(1L, engineering));
        List<EmployeeResponse> responses = List.of(employeeResponse(1L, 3L));
        when(employeeRepository.findAllByOrderByIdAsc()).thenReturn(entities);
        when(employeeMapper.toResponseList(entities)).thenReturn(responses);

        assertThat(employeeService.findAll()).isSameAs(responses);
    }

    @Test
    void findByDepartmentIdReturnsThatDepartmentsEmployees() {
        List<EmployeeEntity> entities = List.of(employee(1L, engineering));
        List<EmployeeResponse> responses = List.of(employeeResponse(1L, 3L));
        when(departmentRepository.existsById(3L)).thenReturn(true);
        when(employeeRepository.findByDepartmentIdOrderByIdAsc(3L)).thenReturn(entities);
        when(employeeMapper.toResponseList(entities)).thenReturn(responses);

        assertThat(employeeService.findByDepartmentId(3L)).isSameAs(responses);
    }

    @Test
    void findByDepartmentIdThrowsWhenDepartmentMissing() {
        when(departmentRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> employeeService.findByDepartmentId(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Department 99 not found");
        verifyNoInteractions(employeeRepository);
    }

    @Test
    void requestTerminationPublishesEventWithGivenDate() {
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(employee(7L, engineering)));

        employeeService.requestTermination(7L, new TerminateEmployeeRequest("  Restructuring ", LocalDate.of(2026, 10, 31)));

        ArgumentCaptor<EmployeeTerminationEvent> captor = ArgumentCaptor.forClass(EmployeeTerminationEvent.class);
        verify(terminationPublisher).publish(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new EmployeeTerminationEvent(
                7L, "Restructuring", LocalDate.of(2026, 10, 31), Instant.parse("2026-10-11T10:00:00Z")));
    }

    @Test
    void requestTerminationDefaultsEffectiveDateToToday() {
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(employee(7L, engineering)));

        employeeService.requestTermination(7L, new TerminateEmployeeRequest("Restructuring", null));

        ArgumentCaptor<EmployeeTerminationEvent> captor = ArgumentCaptor.forClass(EmployeeTerminationEvent.class);
        verify(terminationPublisher).publish(captor.capture());
        assertThat(captor.getValue().effectiveDate()).isEqualTo(LocalDate.of(2026, 10, 11));
    }

    @Test
    void requestTerminationThrowsWhenEmployeeMissing() {
        when(employeeRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.requestTermination(7L, new TerminateEmployeeRequest("x", null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Employee 7 not found");
        verifyNoInteractions(terminationPublisher);
    }

    @Test
    void requestTerminationRejectsAlreadyTerminatedEmployee() {
        EmployeeEntity terminated = employee(7L, engineering);
        terminated.setStatus(EmployeeStatus.TERMINATED);
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(terminated));

        assertThatThrownBy(() -> employeeService.requestTermination(7L, new TerminateEmployeeRequest("x", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Employee 7 is already terminated");
        verifyNoInteractions(terminationPublisher);
    }

    @Test
    void terminateMarksEmployeeTerminatedAndSaves() {
        EmployeeEntity active = employee(7L, engineering);
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(active));

        employeeService.terminate(new EmployeeTerminationEvent(
                7L, "Restructuring", LocalDate.of(2026, 10, 31), Instant.parse("2026-10-11T10:00:00Z")));

        assertThat(active.getStatus()).isEqualTo(EmployeeStatus.TERMINATED);
        assertThat(active.getTerminatedAt()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(active.getTerminationReason()).isEqualTo("Restructuring");
        verify(employeeRepository).save(active);
    }

    @Test
    void terminateIsIdempotentForAlreadyTerminatedEmployee() {
        EmployeeEntity terminated = employee(7L, engineering);
        terminated.setStatus(EmployeeStatus.TERMINATED);
        terminated.setTerminatedAt(LocalDate.of(2026, 9, 30));
        terminated.setTerminationReason("Original reason");
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(terminated));

        employeeService.terminate(new EmployeeTerminationEvent(
                7L, "Duplicate delivery", LocalDate.of(2026, 10, 31), Instant.parse("2026-10-11T10:00:00Z")));

        assertThat(terminated.getTerminatedAt()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(terminated.getTerminationReason()).isEqualTo("Original reason");
        verify(employeeRepository).findById(7L);
        verifyNoMoreInteractions(employeeRepository);
    }

    @Test
    void terminateThrowsWhenEmployeeMissing() {
        when(employeeRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.terminate(new EmployeeTerminationEvent(
                7L, "x", LocalDate.of(2026, 10, 31), Instant.parse("2026-10-11T10:00:00Z"))))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Employee 7 not found");
    }
}
