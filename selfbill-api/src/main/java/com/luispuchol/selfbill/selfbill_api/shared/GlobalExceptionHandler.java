package com.luispuchol.selfbill.selfbill_api.shared;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

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

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}", request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.format());
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
