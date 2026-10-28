package com.springapi.bookmark;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.springapi.book.Book;
import com.springapi.book.BookService;
import com.springapi.bookmark.dto.BookmarkResponse;
import com.springapi.bookmark.dto.CreateBookmarkRequest;
import com.springapi.bookmark.dto.UpdateBookmarkRequest;
import com.springapi.common.dto.PageResponse;
import com.springapi.common.exception.ConflictException;
import com.springapi.common.exception.ResourceNotFoundException;
import com.springapi.user.User;
import com.springapi.user.UserService;

@Service
@Transactional(readOnly = true)
public class BookmarkService {

	private final BookmarkRepository bookmarkRepository;

	private final UserService userService;

	private final BookService bookService;

	public BookmarkService(BookmarkRepository bookmarkRepository, UserService userService, BookService bookService) {
		this.bookmarkRepository = bookmarkRepository;
		this.userService = userService;
		this.bookService = bookService;
	}

	/**
	 * Lists bookmarks, optionally only those belonging to {@code userId}.
	 */
	public PageResponse<BookmarkResponse> findAll(Long userId, Pageable pageable) {
		Page<Bookmark> bookmarks = (userId == null)
				? bookmarkRepository.findAll(pageable)
				: bookmarkRepository.findByUserId(userId, pageable);
		return PageResponse.from(bookmarks.map(BookmarkResponse::from));
	}

	public BookmarkResponse findById(Long id) {
		return BookmarkResponse.from(getBookmarkEntity(id));
	}

	@Transactional
	public BookmarkResponse create(CreateBookmarkRequest request) {
		// Both lookups throw 404 if the user or book does not exist.
		User user = userService.getUserEntity(request.userId());
		Book book = bookService.getBookEntity(request.bookId());
		if (bookmarkRepository.existsByUserIdAndBookId(user.getId(), book.getId())) {
			throw new ConflictException("User %d has already bookmarked book %d".formatted(user.getId(), book.getId()));
		}
		Bookmark bookmark = new Bookmark();
		bookmark.setUser(user);
		bookmark.setBook(book);
		bookmark.setNote(request.note());
		return BookmarkResponse.from(bookmarkRepository.save(bookmark));
	}

	@Transactional
	public BookmarkResponse update(Long id, UpdateBookmarkRequest request) {
		Bookmark bookmark = getBookmarkEntity(id);
		bookmark.setNote(request.note());
		// Flush so updatedAt is refreshed before we build the response.
		return BookmarkResponse.from(bookmarkRepository.saveAndFlush(bookmark));
	}

	@Transactional
	public void delete(Long id) {
		bookmarkRepository.delete(getBookmarkEntity(id));
	}

	private Bookmark getBookmarkEntity(Long id) {
		return bookmarkRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Bookmark", id));
	}

}
