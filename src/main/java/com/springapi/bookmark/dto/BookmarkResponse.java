package com.springapi.bookmark.dto;

import java.time.Instant;

import com.springapi.book.Book;
import com.springapi.bookmark.Bookmark;

public record BookmarkResponse(
		Long id,
		Long userId,
		BookSummary book,
		String note,
		Instant createdAt,
		Instant updatedAt) {

	public record BookSummary(Long id, String title) {

		public static BookSummary from(Book book) {
			return new BookSummary(book.getId(), book.getTitle());
		}

	}

	public static BookmarkResponse from(Bookmark bookmark) {
		return new BookmarkResponse(
				bookmark.getId(),
				bookmark.getUser().getId(),
				BookSummary.from(bookmark.getBook()),
				bookmark.getNote(),
				bookmark.getCreatedAt(),
				bookmark.getUpdatedAt());
	}

}
