package com.atl.ticketmgmt.common.api;

import java.util.List;

/**
 * API error body per spec/api-contract.md.
 */
public record ErrorResponse(
        String code,
        String message,
        List<FieldError> fieldErrors
) {

    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, List.of());
    }

    public static ErrorResponse of(String code, String message, List<FieldError> fieldErrors) {
        return new ErrorResponse(code, message, fieldErrors);
    }
}
