package com.example.sample.mapper;

import java.util.Locale;

/** Small text-normalization helpers shared by mappers. */
final class MappingSupport {

    private MappingSupport() {
    }

    static String trim(String value) {
        return value == null ? null : value.trim();
    }

    static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static String normalizeEmail(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
