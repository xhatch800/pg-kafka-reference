package com.example.sample.messaging;

import java.time.Instant;
import java.time.LocalDate;

public record EmployeeTerminationEvent(
        Long employeeId,
        String reason,
        LocalDate effectiveDate,
        Instant requestedAt) {
}
