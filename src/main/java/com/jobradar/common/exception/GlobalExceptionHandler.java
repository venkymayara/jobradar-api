package com.jobradar.common.exception;

import java.util.Map;
import java.util.TreeMap;

import com.jobradar.common.api.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException exception) {
        return failure(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleJpaNotFound(EntityNotFoundException exception) {
        return failure(HttpStatus.NOT_FOUND, "The requested resource was not found");
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleMissingRoute(Exception exception) {
        return failure(HttpStatus.NOT_FOUND, "The requested resource was not found");
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException exception) {
        return failure(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler({ConflictException.class, DataIntegrityViolationException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ApiResponse<Void>> handleConflict(Exception exception) {
        String message = exception instanceof ConflictException
                ? exception.getMessage()
                : "The request conflicts with existing data";
        return failure(HttpStatus.CONFLICT, message);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException exception) {
        return failure(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException exception) {
        return failure(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleBodyValidation(
            BindException exception) {
        Map<String, String> errors = new TreeMap<>();
        exception.getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(ApiResponse.failure("Request validation failed", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolation(
            ConstraintViolationException exception) {
        Map<String, String> errors = new TreeMap<>();
        exception.getConstraintViolations().forEach(violation ->
                errors.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage()));
        return ResponseEntity.badRequest().body(ApiResponse.failure("Request validation failed", errors));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class,
            HandlerMethodValidationException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(Exception exception) {
        return failure(HttpStatus.BAD_REQUEST, "The request contains an invalid or missing value");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(
            Exception exception, HttpServletRequest request) {
        log.atError()
                .addKeyValue("path", request.getRequestURI())
                .addKeyValue("exception", exception.getClass().getSimpleName())
                .addKeyValue("causeTypes", causeTypes(exception))
                .addKeyValue("causeFrames", causeFrames(exception))
                .log("Unhandled API exception");
        return failure(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private String causeTypes(Throwable exception) {
        StringBuilder types = new StringBuilder();
        Throwable cause = exception;
        while (cause != null) {
            if (!types.isEmpty()) {
                types.append('>');
            }
            types.append(cause.getClass().getSimpleName());
            if (cause.getCause() == cause) {
                break;
            }
            cause = cause.getCause();
        }
        return types.toString();
    }

    private String causeFrames(Throwable exception) {
        Throwable cause = exception;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return java.util.Arrays.stream(cause.getStackTrace())
                .limit(8)
                .map(frame -> frame.getClassName() + "#" + frame.getMethodName() + ":" + frame.getLineNumber())
                .collect(java.util.stream.Collectors.joining(">"));
    }

    private ResponseEntity<ApiResponse<Void>> failure(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.failure(message));
    }
}
