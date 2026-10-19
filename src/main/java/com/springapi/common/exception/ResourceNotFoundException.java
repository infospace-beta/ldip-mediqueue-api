package com.springapi.common.exception;

/**
 * Thrown when a requested record does not exist. Mapped to HTTP 404 by {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String resourceName, Object id) {
		super("%s with id %s not found".formatted(resourceName, id));
	}

}
