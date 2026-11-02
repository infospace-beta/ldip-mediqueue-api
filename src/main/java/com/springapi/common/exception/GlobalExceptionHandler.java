
package com.springapi.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Handle resources that cannot be found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                ex.getMessage()
        );
    }

    // Handle conflicts
    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        return problem(
                HttpStatus.CONFLICT,
                "Conflict",
                ex.getMessage()
        );
    }

    // Handle database integrity violations
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {

        log.warn(
                "Data integrity violation: {}",
                ex.getMostSpecificCause().getMessage()
        );

        return problem(
                HttpStatus.CONFLICT,
                "Conflict",
                "The request conflicts with existing data (duplicate value or record still in use)."
        );
    }

    // Handle invalid sorting properties
    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail handleInvalidSort(PropertyReferenceException ex) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid sort property",
                ex.getMessage()
        );
    }

    // Handle unexpected exceptions
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {

        log.error("Unexpected error", ex);

        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "An unexpected error occurred."
        );
    }

    // Handle malformed JSON request bodies
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        log.error("Failed to read request body", ex);

        ProblemDetail body = ProblemDetail.forStatusAndDetail(
                status,
                "The request body could not be read. Check the server logs for details."
        );

        body.setTitle("Invalid request body");

        return handleExceptionInternal(
                ex,
                body,
                headers,
                status,
                request
        );
    }

    // Handle validation errors
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.putIfAbsent(
                        error.getField(),
                        error.getDefaultMessage()
                ));

        ProblemDetail body = ex.getBody();

        body.setTitle("Validation failed");
        body.setDetail("One or more fields are invalid.");
        body.setProperty("errors", errors);

        return handleExceptionInternal(
                ex,
                body,
                headers,
                status,
                request
        );
    }

    // Reusable ProblemDetail response
    private ProblemDetail problem(
            HttpStatus status,
            String title,
            String detail) {

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(status, detail);

        problem.setTitle(title);

        return problem;
    }
}
