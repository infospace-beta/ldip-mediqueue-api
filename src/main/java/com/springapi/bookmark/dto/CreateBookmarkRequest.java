package com.springapi.bookmark.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBookmarkRequest(

		@NotNull
		Long userId,

		@NotNull
		Long bookId,

		@Size(max = 500)
		String note) {
}
