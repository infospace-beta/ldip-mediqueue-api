package com.springapi.book;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.springapi.author.AuthorService;
import com.springapi.book.dto.BookRequest;
import com.springapi.book.dto.BookResponse;
import com.springapi.common.dto.PageResponse;
import com.springapi.common.exception.ConflictException;
import com.springapi.common.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class BookService {

	private final BookRepository bookRepository;

	private final AuthorService authorService;

	public BookService(BookRepository bookRepository, AuthorService authorService) {
		this.bookRepository = bookRepository;
		this.authorService = authorService;
	}

	/**
	 * Lists books, optionally only those written by {@code authorId}.
	 */
	public PageResponse<BookResponse> findAll(Long authorId, Pageable pageable) {
		Page<Book> books = (authorId == null)
				? bookRepository.findAll(pageable)
				: bookRepository.findByAuthorId(authorId, pageable);
		return PageResponse.from(books.map(BookResponse::from));
	}

	public BookResponse findById(Long id) {
		return BookResponse.from(getBookEntity(id));
	}

	@Transactional
	public BookResponse create(BookRequest request) {
		ensureIsbnUnique(request.isbn(), null);
		Book book = new Book();
		applyRequest(book, request);
		return BookResponse.from(bookRepository.save(book));
	}

	@Transactional
	public BookResponse update(Long id, BookRequest request) {
		Book book = getBookEntity(id);
		ensureIsbnUnique(request.isbn(), id);
		applyRequest(book, request);
		// Flush so updatedAt is refreshed before we build the response.
		return BookResponse.from(bookRepository.saveAndFlush(book));
	}

	/** Bookmarks of this book are removed by the database (ON DELETE CASCADE). */
	@Transactional
	public void delete(Long id) {
		bookRepository.delete(getBookEntity(id));
	}

	/**
	 * Loads the entity or throws 404. Public so other features' services (e.g. bookmarks) can reuse it.
	 */
	public Book getBookEntity(Long id) {
		return bookRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Book", id));
	}

	private void ensureIsbnUnique(String isbn, Long currentBookId) {
		bookRepository.findByIsbn(isbn)
				.filter(existing -> !existing.getId().equals(currentBookId))
				.ifPresent(existing -> {
					throw new ConflictException("A book with ISBN '%s' already exists".formatted(isbn));
				});
	}

	private void applyRequest(Book book, BookRequest request) {
		book.setTitle(request.title());
		book.setIsbn(request.isbn());
		book.setDescription(request.description());
		book.setPublishedYear(request.publishedYear());
		book.setAuthor(authorService.getAuthorEntity(request.authorId()));
	}

}
