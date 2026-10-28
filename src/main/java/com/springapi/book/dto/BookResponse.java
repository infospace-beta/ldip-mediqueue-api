package com.springapi.book.dto;

import java.time.Instant;

import com.springapi.author.Author;
import com.springapi.book.Book;

public record BookResponse(
		Long id,
		String title,
		String isbn,
		String description,
		Integer publishedYear,
		AuthorSummary author,
		Instant createdAt,
		Instant updatedAt) {

	/** A small embedded view of the author, so clients don't need a second request. */
	public record AuthorSummary(Long id, String name) {

		public static AuthorSummary from(Author author) {
			return new AuthorSummary(author.getId(), author.getName());
		}

	}

	public static BookResponse from(Book book) {
		return new BookResponse(
				book.getId(),
				book.getTitle(),
				book.getIsbn(),
				book.getDescription(),
				book.getPublishedYear(),
				AuthorSummary.from(book.getAuthor()),
				book.getCreatedAt(),
				book.getUpdatedAt());
	}

}
