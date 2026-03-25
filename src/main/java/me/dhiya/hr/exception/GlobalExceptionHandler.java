package me.dhiya.hr.exception;

import me.dhiya.hr.dto.response.CustomApiError;
import me.dhiya.hr.dto.response.CustomFieldError;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

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
