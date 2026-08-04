package com.example.jwtdemo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler – xử lý tập trung các lỗi và trả về response JSON nhất quán.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Lỗi validation (@Valid).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            fieldErrors.put(field, message);
        });

        return ResponseEntity.badRequest().body(Map.of(
                "error", "Validation Failed",
                "details", fieldErrors,
                "timestamp", Instant.now().toString()
        ));
    }

    /**
     * Lỗi runtime (username/password sai, token không hợp lệ, v.v.).
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.warn("RuntimeException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "Unauthorized",
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }

    /**
     * Lỗi phân quyền – 403 Forbidden.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "Forbidden",
                "message", "Bạn không có quyền truy cập tài nguyên này",
                "timestamp", Instant.now().toString()
        ));
    }

    /**
     * Lỗi server không xác định.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unhandled exception: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal Server Error",
                "message", "Đã xảy ra lỗi máy chủ",
                "timestamp", Instant.now().toString()
        ));
    }
}
