package com.luispuchol.selfbill.selfbill_api.shared;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice(basePackages = "com.luispuchol.selfbill.selfbill_api")
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("[{}] {}", errorCode, ex.getMessage(), ex.getCause());
        return problem(errorCode.getHttpStatus(), errorCode, ex.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        error -> String.valueOf(error.getDefaultMessage()), (first, second) -> first));
        return handleExceptionInternal(ex, validationProblem(errors), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBeanResults().stream()
                .flatMap(result -> result.getFieldErrors().stream())
                .forEach(error -> errors.putIfAbsent(error.getField(), String.valueOf(error.getDefaultMessage())));
        ex.getValueResults().forEach(result -> errors.putIfAbsent(result.getMethodParameter().getParameterName(),
                String.valueOf(result.getResolvableErrors().get(0).getDefaultMessage())));
        return handleExceptionInternal(ex, validationProblem(errors), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = Map.of(ex.getParameterName(), "must not be null");
        return handleExceptionInternal(ex, validationProblem(errors), headers, status, request);
    }

    // The only upload the API takes is the logo
    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ErrorCode errorCode = ErrorCode.USER_PROFILE_LOGO_TOO_LARGE;
        log.warn("[{}] {}", errorCode, ex.getMessage());
        return handleExceptionInternal(ex, problem(errorCode.getHttpStatus(), errorCode, errorCode.format()), headers,
                errorCode.getHttpStatus(), request);
    }

    // Sorting by a property the entity lacks reaches here as the repository's own exception, usually wrapped
    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail handleUnknownProperty(PropertyReferenceException ex) {
        return validationProblem(Map.of("sort", String.valueOf(ex.getMessage())));
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ProblemDetail handleInvalidDataAccessApiUsage(InvalidDataAccessApiUsageException ex,
            HttpServletRequest request) {
        for (Throwable cause = ex.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof PropertyReferenceException unknownProperty) {
                return handleUnknownProperty(unknownProperty);
            }
        }
        return handleUnexpected(ex, request);
    }

    // A unique index lost a race (two requests took the same code or number): the user can just retry.
    // Any other database error is a bug and stays a 500.
    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail handleDataAccess(DataAccessException ex, HttpServletRequest request) {
        if (isUniqueViolation(ex)) {
            ErrorCode errorCode = ErrorCode.DATA_CONFLICT;
            log.warn("[{}] {}", errorCode, ex.getMessage());
            return problem(errorCode.getHttpStatus(), errorCode, errorCode.format());
        }
        return handleUnexpected(ex, request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}", request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.format());
    }

    private static boolean isUniqueViolation(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLiteException sqlite
                    && sqlite.getResultCode() == SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE) {
                return true;
            }
            if (cause instanceof ConstraintViolationException violation
                    && violation.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE) {
                return true;
            }
        }
        return false;
    }

    private static ProblemDetail validationProblem(Map<String, String> errors) {
        log.warn("[{}] {}", ErrorCode.VALIDATION_ERROR, errors);
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                ErrorCode.VALIDATION_ERROR.format());
        problem.setProperty("errors", errors);
        return problem;
    }

    private static ProblemDetail problem(HttpStatusCode status, ErrorCode errorCode, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("errorCode", errorCode.name());
        return problem;
    }
}
