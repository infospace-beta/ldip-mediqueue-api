package com.springapi;

import com.springapi.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unexpectedExceptionReturnsGenericInternalServerError() {
        Exception exception = new RuntimeException("Sensitive internal database details");

        ProblemDetail response = handler.handleUnexpected(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus());
        assertEquals("Internal server error", response.getTitle());
        assertEquals("An unexpected error occurred.", response.getDetail());
        assertFalse(response.getDetail().contains("Sensitive internal database details"));
    }
}
