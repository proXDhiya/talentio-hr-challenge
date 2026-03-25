package me.dhiya.hr.exception;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.core.Ordered;

import me.dhiya.hr.dto.common.CustomFieldError;
import me.dhiya.hr.dto.common.CustomApiError;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<CustomFieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map((org.springframework.validation.FieldError e) ->
                        CustomFieldError.builder()
                                .field(e.getField())
                                .message(e.getDefaultMessage())
                                .build())
                .toList();

        return ResponseEntity.badRequest().body(
                CustomApiError.builder()
                        .message("Validation failed")
                        .errors(errors)
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<CustomApiError> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(
                CustomApiError.builder()
                        .message(ex.getReason())
                        .errors(List.of())
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<CustomApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                CustomApiError.builder()
                        .message("An employee with this email already exists")
                        .errors(List.of(CustomFieldError.builder()
                                .field("email")
                                .message("Email is already in use")
                                .build()))
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CustomApiError> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(
                CustomApiError.builder()
                        .message("Invalid request format")
                        .errors(List.of())
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomApiError> handleGeneric(Exception ex) {
        return ResponseEntity.internalServerError().body(
                CustomApiError.builder()
                        .message("An unexpected error occurred. Please try again later.")
                        .errors(List.of())
                        .timestamp(Instant.now())
                        .build());
    }
}
