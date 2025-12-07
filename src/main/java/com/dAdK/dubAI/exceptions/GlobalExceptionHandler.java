package com.dAdK.dubAI.exceptions;

import com.dAdK.dubAI.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFoundException(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Resource Not Found: " + ex.getMessage()));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAlreadyExistException(UserAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Conflict: " + ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllExceptions(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal Server Error: " + ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidFormat(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid Request Format: " + ex.getMostSpecificCause().getMessage()));
    }

    @ExceptionHandler(OtpException.class)
    public ResponseEntity<ApiResponse<Void>> handleOtpException(OtpException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("OTP Error: " + ex.getMessage()));
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidInputException(InvalidInputException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid Input: " + ex.getMessage()));
    }

    @ExceptionHandler(TtsProcessingException.class)
    public ResponseEntity<ApiResponse<Void>> handleTtsProcessingException(TtsProcessingException ex) {
        // Enhanced error handling for Gemini TTS errors
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            String causeMessage = ex.getCause().getMessage();

            // Rate limit and quota errors - HTTP 429 Too Many Requests
            if (causeMessage.contains("429") || causeMessage.contains("quota exceeded") ||
                    causeMessage.contains("RATE_LIMIT_EXCEEDED") || causeMessage.contains("QUOTA_EXCEEDED")) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(ApiResponse.error("[" + timestamp + "] Quota exceeded. Request limit reached. Please retry after the quota resets (usually after 1-2 minutes)."));
            }

            // Rate limiting without explicit quota
            if (causeMessage.contains("rate limit") || causeMessage.contains("RESOURCE_EXHAUSTED")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(ApiResponse.error("Rate limited. Please slow down your requests or upgrade your plan."));
            }

            // Authentication errors - HTTP 401 Unauthorized
            if (causeMessage.contains("PERMISSION_DENIED") || causeMessage.contains("401") ||
                    causeMessage.contains("authentication")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Authentication failed. Please check your API key configuration."));
            }

            // Invalid arguments - HTTP 400 Bad Request
            if (causeMessage.contains("INVALID_ARGUMENT") || causeMessage.contains("400") ||
                    causeMessage.contains("bad request")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Request error. Please check your input parameters."));
            }

            // Internal server errors - HTTP 500 Internal Server Error
            if (causeMessage.contains("INTERNAL") || causeMessage.contains("500") ||
                    causeMessage.contains("server error")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("Temporary error. Please try again in a few moments."));
            }

            // Service unavailable - HTTP 503 Service Unavailable
            if (causeMessage.contains("SERVICE_UNAVAILABLE") || causeMessage.contains("503")) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(ApiResponse.error("Service currently unavailable. Please try again later."));
            }
        }

        // Default case for other TTS processing errors
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Processing error: " + ex.getMessage() + ". If this persists, please contact support."));
    }

    @ExceptionHandler(AudioAnalysisException.class)
    public ResponseEntity<ApiResponse<Void>> handleAudioAnalysisException(
            AudioAnalysisException ex,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Audio Analysis Error: " + ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            fieldErrors.put(field, error.getDefaultMessage());
        });

        String combinedMessage = fieldErrors.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(" | "));

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Validation Failed: " + combinedMessage));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid Argument: " + ex.getMessage()));
    }


}
