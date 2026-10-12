package com.example.sample.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record EmployeeResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String jobTitle,
        LocalDate hireDate,
        BigDecimal salary,
        Long departmentId,
        String departmentName,
        String status,
        LocalDate terminatedAt,
        String terminationReason,
        Instant createdAt,
        Instant modifiedAt) {
}
