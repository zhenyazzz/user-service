package com.innowise.internship.userservice.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.innowise.internship.userservice.dto.response.ErrorResponse;
import com.innowise.internship.userservice.exception.card.CardLimitExceededException;
import com.innowise.internship.userservice.exception.card.PaymentCardAlreadyExistsException;
import com.innowise.internship.userservice.exception.card.PaymentCardNotFoundException;
import com.innowise.internship.userservice.exception.security.SecurityContextException;
import com.innowise.internship.userservice.exception.user.UserAlreadyExistsException;
import com.innowise.internship.userservice.exception.user.UserNotFoundException;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({UserNotFoundException.class, PaymentCardNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), null);
    }

    @ExceptionHandler(CardLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleCardLimitExceeded(CardLimitExceededException ex) {
        log.warn("Card limit exceeded: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "CARD_LIMIT_EXCEEDED", ex.getMessage(), null);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        log.warn("User already exists: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", "User with this email already exists", null);
    }

    @ExceptionHandler(PaymentCardAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCardAlreadyExists(PaymentCardAlreadyExistsException ex) {
        log.warn("Card already exists: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "CARD_ALREADY_EXISTS", ex.getMessage(), null);
    }

    /**
     * Handles race condition: two concurrent requests create a card with the same number.
     * Both pass existsByNumber() check, one commits first, the second hits DB unique constraint
     * and Hibernate throws DataIntegrityViolationException. Without this handler the client gets 500.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "CONFLICT", "Resource already exists", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED", ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors().forEach(error -> {
            String key = "global";
            String msg = error.getDefaultMessage();
            if (errors.containsKey(key)) {
                errors.put(key, errors.get(key) + "; " + msg);
            } else {
                errors.put(key, msg);
            }
        });
        log.warn("Validation failed: {}", errors);
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Invalid request parameters", errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(v ->
                errors.put(v.getPropertyPath().toString(), v.getMessage()));
        log.warn("Parameter validation failed: {}", errors);
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Invalid request parameters", errors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        log.warn("Missing required parameter: {}", ex.getParameterName());
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "Required parameter '" + ex.getParameterName() + "' is missing", null);
    }

    @ExceptionHandler(SecurityContextException.class)
    public ResponseEntity<ErrorResponse> handleSecurityContext(SecurityContextException ex) {
        log.warn("Security context error: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "SECURITY_ERROR", ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = ex.getRequiredType() != null && ex.getRequiredType().getName().contains("UUID")
                ? "Invalid UUID format: expected 36 characters (e.g. a0000001-0000-0000-0000-000000000001)"
                : "Invalid parameter format: " + (ex.getMessage() != null ? ex.getMessage() : ex.getName());
        log.warn("Parameter type mismatch: {} = {}", ex.getName(), ex.getValue());
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", message, null);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleThrowable(Throwable ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", ex.getMessage() != null ? ex.getMessage() : "Internal server error", null);
    }

    private static ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message, Map<String, String> details) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(code, message, Instant.now(), details));
    }

}
