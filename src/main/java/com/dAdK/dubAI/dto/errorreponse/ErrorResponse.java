package com.dAdK.dubAI.dto.errorreponse;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard error response DTO for API errors.
 * Provides consistent error structure across all endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /**
     * HTTP status code
     */
    private int status;

    /**
     * Error type/category (e.g., "VALIDATION_ERROR", "RESOURCE_NOT_FOUND")
     */
    private String error;

    /**
     * Human-readable error message
     */
    private String message;

    /**
     * API endpoint path where error occurred
     */
    private String path;

    /**
     * Timestamp when error occurred
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * Constructor for simple error responses without path
     */
    public ErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor with all fields
     */
    public ErrorResponse(int status, String error, String message, String path) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.timestamp = LocalDateTime.now();
    }
}