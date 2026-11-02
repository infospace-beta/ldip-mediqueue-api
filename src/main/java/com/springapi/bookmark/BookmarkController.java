package com.springapi.bookmark;

import java.net.URI;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.springapi.bookmark.dto.BookmarkResponse;
import com.springapi.bookmark.dto.CreateBookmarkRequest;
import com.springapi.bookmark.dto.UpdateBookmarkRequest;
import com.springapi.common.dto.PageResponse;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/bookmarks")
@Tag(name = "Bookmarks")
public class BookmarkController {

	private final BookmarkService bookmarkService;

	public BookmarkController(BookmarkService bookmarkService) {
		this.bookmarkService = bookmarkService;
	}

	@GetMapping
	public PageResponse<BookmarkResponse> findAll(
			@Parameter(description = "Only return bookmarks of this user")
			@RequestParam(required = false) Long userId,
			@ParameterObject @SortDefault("id") Pageable pageable) {
		return bookmarkService.findAll(userId, pageable);
	}

	@GetMapping("/{id}")
	public BookmarkResponse findById(@PathVariable Long id) {
		return bookmarkService.findById(id);
	}

	@PostMapping
	public ResponseEntity<BookmarkResponse> create(@Valid @RequestBody CreateBookmarkRequest request) {
		BookmarkResponse created = bookmarkService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}").buildAndExpand(created.id()).toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	public BookmarkResponse update(@PathVariable Long id, @Valid @RequestBody UpdateBookmarkRequest request) {
		return bookmarkService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		bookmarkService.delete(id);
	}

}
