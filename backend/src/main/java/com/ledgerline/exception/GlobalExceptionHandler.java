package com.ledgerline.exception;
import org.springframework.security.core.AuthenticationException;
import com.ledgerline.dto.ApiError;
import jakarta.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> business(BusinessException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiError.of(ex.getStatus().value(), ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(Instant.now(), 400, "VALIDATION_FAILED", "Validation failed", fields));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> unreadable(Exception ex) {
        return ResponseEntity.badRequest().body(ApiError.of(400, "BAD_REQUEST", "Malformed request"));
    }

    @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
    public ResponseEntity<ApiError> badCredentials(AuthenticationException ex) {
        // One message for wrong password, unknown email and disabled user: no account enumeration.
        return ResponseEntity.status(401).body(ApiError.of(401, "INVALID_CREDENTIALS", "Invalid email or password"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> denied(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(ApiError.of(403, "FORBIDDEN", "You are not allowed to do this"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(409).body(ApiError.of(409, "CONFLICT", "Request conflicts with existing data"));
    }

    /** Framework errors (405, 404, missing header ...) keep their own status instead of becoming 500. */
    @ExceptionHandler(ServletException.class)
    public ResponseEntity<ApiError> servlet(ServletException ex) {
        int status = (ex instanceof ErrorResponse er) ? er.getStatusCode().value() : 500;
        return ResponseEntity.status(status).body(ApiError.of(status, "REQUEST_ERROR", status == 500 ? "Something went wrong" : ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> other(Exception ex) {
        log.error("Unhandled error", ex);
        return ResponseEntity.status(500).body(ApiError.of(500, "INTERNAL_ERROR", "Something went wrong"));
    }
}
