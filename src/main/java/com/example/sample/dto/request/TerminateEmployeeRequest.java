package com.example.sample.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TerminateEmployeeRequest(
        @NotBlank @Size(max = 255) String reason,
        LocalDate effectiveDate) {
}
