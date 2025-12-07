package com.dAdK.dubAI.dto.errorreponse;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Validation error response DTO with field-specific errors.
 * Used when request validation fails with detailed field-level errors.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {

    /**
     * HTTP status code (typically 400)
     */
    private int status;

    /**
     * Error type (e.g., "VALIDATION_ERROR")
     */
    private String error;

    /**
     * Overall validation error message
     */
    private String message;

    /**
     * Map of field names to their specific error messages
     * Example: {"email": "must be a valid email", "age": "must be greater than 0"}
     */
    private Map<String, String> fieldErrors;

    /**
     * API endpoint path where validation failed
     */
    private String path;

    /**
     * Timestamp when error occurred
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * Constructor for validation errors without path
     */
    public ValidationErrorResponse(int status, String error, String message, Map<String, String> fieldErrors) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.fieldErrors = fieldErrors;
        this.timestamp = LocalDateTime.now();
    }

    public ValidationErrorResponse(int status, String error, String message,
                                   Map<String, String> fieldErrors, String path) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.fieldErrors = fieldErrors;
        this.path = path;
        this.timestamp = LocalDateTime.now();
    }
}
