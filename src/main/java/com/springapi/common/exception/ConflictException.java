package com.springapi.common.exception;

/**
 * Thrown when a request clashes with existing data, e.g. a duplicate email or deleting a record
 * that is still in use. Mapped to HTTP 409 by {@link GlobalExceptionHandler}.
 */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}

}
