package com.example.sample.dto.response;

import java.time.Instant;

public record DepartmentResponse(
        Long id,
        String name,
        String location,
        Instant createdAt,
        Instant modifiedAt) {
}
