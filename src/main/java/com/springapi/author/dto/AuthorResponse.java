package com.springapi.author.dto;

import java.time.Instant;

import com.springapi.author.Author;

public record AuthorResponse(
		Long id,
		String name,
		String bio,
		Instant createdAt,
		Instant updatedAt) {

	public static AuthorResponse from(Author author) {
		return new AuthorResponse(
				author.getId(),
				author.getName(),
				author.getBio(),
				author.getCreatedAt(),
				author.getUpdatedAt());
	}

}
