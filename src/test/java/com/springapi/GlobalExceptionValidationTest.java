package com.springapi;

import com.springapi.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionValidationTest {

    static class TestableHandler extends GlobalExceptionHandler {
        ResponseEntity<Object> invokeValidationHandler(
                MethodArgumentNotValidException ex,
                HttpHeaders headers,
                HttpStatusCode status,
                WebRequest request) {
            return handleMethodArgumentNotValid(ex, headers, status, request);
        }
    }

    @Test
    void invalidRequestReturnsBadRequestWithFieldErrors() {
        var bindingResult = new BeanPropertyBindingResult(
                new Object(), "hospital");
        bindingResult.addError(new FieldError(
                "hospital", "name", "", false,
                null, null, "Hospital name is required"));

        var exception = new MethodArgumentNotValidException(
                null, bindingResult);

        var handler = new TestableHandler();
        var request = new ServletWebRequest(new MockHttpServletRequest());

        var response = handler.invokeValidationHandler(
                exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertInstanceOf(ProblemDetail.class, response.getBody());

        ProblemDetail body = (ProblemDetail) response.getBody();
        assertEquals("Validation failed", body.getTitle());
        assertEquals("One or more fields are invalid.", body.getDetail());

        @SuppressWarnings("unchecked")
        var errors = (java.util.Map<String, String>)
                body.getProperties().get("errors");

        assertNotNull(errors);
        assertEquals("Hospital name is required", errors.get("name"));
    }
}