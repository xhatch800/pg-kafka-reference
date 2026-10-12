package com.example.sample.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.example.sample.exception.NotFoundException;
import com.example.sample.service.EmployeeService;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeTerminationListenerTest {

    private static final EmployeeTerminationEvent EVENT = new EmployeeTerminationEvent(
            7L, "Restructuring", LocalDate.of(2026, 10, 31), Instant.parse("2026-10-11T10:00:00Z"));

    @Mock
    EmployeeService employeeService;

    @InjectMocks
    EmployeeTerminationListener listener;

    @Test
    void delegatesEventToService() {
        listener.onTermination(EVENT);

        verify(employeeService).terminate(EVENT);
    }

    @Test
    void propagatesServiceFailureSoErrorHandlerCanRetry() {
        doThrow(NotFoundException.of("Employee", 7L)).when(employeeService).terminate(EVENT);

        assertThatThrownBy(() -> listener.onTermination(EVENT)).isInstanceOf(NotFoundException.class);
    }
}
