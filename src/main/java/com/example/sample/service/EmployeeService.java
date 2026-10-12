package com.example.sample.service;

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
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeMapper employeeMapper;
    private final EmployeeTerminationPublisher terminationPublisher;
    private final Clock clock;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            EmployeeMapper employeeMapper,
            EmployeeTerminationPublisher terminationPublisher,
            Clock clock) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.employeeMapper = employeeMapper;
        this.terminationPublisher = terminationPublisher;
        this.clock = clock;
    }

    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request) {
        DepartmentEntity department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> NotFoundException.of("Department", request.departmentId()));
        EmployeeEntity employee = employeeMapper.toEntity(request, department);
        if (employeeRepository.existsByEmailIgnoreCase(employee.getEmail())) {
            throw new ConflictException("Employee with email '" + employee.getEmail() + "' already exists");
        }
        return employeeMapper.toResponse(employeeRepository.save(employee));
    }

    public EmployeeResponse findById(Long id) {
        return employeeRepository.findWithDepartmentById(id)
                .map(employeeMapper::toResponse)
                .orElseThrow(() -> NotFoundException.of("Employee", id));
    }

    public List<EmployeeResponse> findAll() {
        return employeeMapper.toResponseList(employeeRepository.findAllByOrderByIdAsc());
    }

    public List<EmployeeResponse> findByDepartmentId(Long departmentId) {
        if (!departmentRepository.existsById(departmentId)) {
            throw NotFoundException.of("Department", departmentId);
        }
        return employeeMapper.toResponseList(employeeRepository.findByDepartmentIdOrderByIdAsc(departmentId));
    }

    /** Validates the request and publishes a termination event; the change is applied asynchronously. */
    public void requestTermination(Long id, TerminateEmployeeRequest request) {
        EmployeeEntity employee = employeeRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Employee", id));
        if (employee.getStatus() == EmployeeStatus.TERMINATED) {
            throw new ConflictException("Employee " + id + " is already terminated");
        }
        LocalDate effectiveDate = request.effectiveDate() != null ? request.effectiveDate() : LocalDate.now(clock);
        terminationPublisher.publish(new EmployeeTerminationEvent(
                id, request.reason().trim(), effectiveDate, clock.instant()));
    }

    /** Applies a termination event. Re-delivered events for an already terminated employee are ignored. */
    @Transactional
    public void terminate(EmployeeTerminationEvent event) {
        EmployeeEntity employee = employeeRepository.findById(event.employeeId())
                .orElseThrow(() -> NotFoundException.of("Employee", event.employeeId()));
        if (employee.getStatus() == EmployeeStatus.TERMINATED) {
            log.info("Employee {} is already terminated; skipping duplicate event", event.employeeId());
            return;
        }
        employee.setStatus(EmployeeStatus.TERMINATED);
        employee.setTerminatedAt(event.effectiveDate());
        employee.setTerminationReason(event.reason());
        employeeRepository.save(employee);
    }
}
