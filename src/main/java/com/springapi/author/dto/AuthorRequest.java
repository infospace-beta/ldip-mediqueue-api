package com.springapi.author.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating and updating an author.
 */
public record AuthorRequest(

		@NotBlank
		@Size(max = 150)
		String name,

		@Size(max = 2000)
		String bio) {
}
