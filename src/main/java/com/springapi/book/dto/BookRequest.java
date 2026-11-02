package com.springapi.book.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating and updating a book.
 */
public record BookRequest(

		@NotBlank
		@Size(max = 255)
		String title,

		@NotBlank
		@Pattern(regexp = "^[0-9-]{10,17}$", message = "must be an ISBN-10 or ISBN-13 (digits and hyphens)")
		String isbn,

		@Size(max = 2000)
		String description,

		@Min(0)
		@Max(9999)
		Integer publishedYear,

		@NotNull
		Long authorId) {
}
