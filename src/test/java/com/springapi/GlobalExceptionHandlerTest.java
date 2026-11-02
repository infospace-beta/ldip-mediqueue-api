package com.springapi;

import com.springapi.common.exception.ConflictException;
import com.springapi.common.exception.GlobalExceptionHandler;
import com.springapi.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void resourceNotFoundReturns404() {
        ProblemDetail response = handler.handleNotFound(
                new ResourceNotFoundException("Hospital", 999L));

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
        assertEquals("Resource not found", response.getTitle());
        assertEquals("Hospital with id 999 not found", response.getDetail());
    }

    @Test
    void conflictReturns409() {
        ProblemDetail response = handler.handleConflict(
                new ConflictException("Email already exists"));

        assertEquals(HttpStatus.CONFLICT.value(), response.getStatus());
        assertEquals("Conflict", response.getTitle());
        assertEquals("Email already exists", response.getDetail());
    }

    @Test
    void unexpectedExceptionReturnsGenericInternalServerError() {
        Exception exception =
                new RuntimeException("Sensitive internal database details");

        ProblemDetail response = handler.handleUnexpected(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus());
        assertEquals("Internal server error", response.getTitle());
        assertEquals("An unexpected error occurred.", response.getDetail());
        assertFalse(response.getDetail().contains(
                "Sensitive internal database details"));
    }
}
