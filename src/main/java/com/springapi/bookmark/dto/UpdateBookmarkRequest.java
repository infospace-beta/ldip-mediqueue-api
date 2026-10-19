package com.springapi.bookmark.dto;

import jakarta.validation.constraints.Size;

/**
 * Only the note can change. To bookmark a different book, create a new bookmark.
 */
public record UpdateBookmarkRequest(

		@Size(max = 500)
		String note) {
}
