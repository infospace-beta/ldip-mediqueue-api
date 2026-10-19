package com.springapi.bookmark;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

	@Override
	@EntityGraph(attributePaths = "book")
	Page<Bookmark> findAll(Pageable pageable);

	@EntityGraph(attributePaths = "book")
	Page<Bookmark> findByUserId(Long userId, Pageable pageable);

	boolean existsByUserIdAndBookId(Long userId, Long bookId);

}
