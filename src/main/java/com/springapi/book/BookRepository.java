package com.springapi.book;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {

	/**
	 * {@code @EntityGraph} loads each book's author in the same query, avoiding one extra query per book
	 * (the "N+1 problem") when building the list response.
	 */
	@Override
	@EntityGraph(attributePaths = "author")
	Page<Book> findAll(Pageable pageable);

	@EntityGraph(attributePaths = "author")
	Page<Book> findByAuthorId(Long authorId, Pageable pageable);

	Optional<Book> findByIsbn(String isbn);

}
