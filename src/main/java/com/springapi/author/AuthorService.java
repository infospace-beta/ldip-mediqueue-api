package com.springapi.author;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.springapi.author.dto.AuthorRequest;
import com.springapi.author.dto.AuthorResponse;
import com.springapi.common.dto.PageResponse;
import com.springapi.common.exception.ConflictException;
import com.springapi.common.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class AuthorService {

	private final AuthorRepository authorRepository;

	public AuthorService(AuthorRepository authorRepository) {
		this.authorRepository = authorRepository;
	}

	public PageResponse<AuthorResponse> findAll(Pageable pageable) {
		return PageResponse.from(authorRepository.findAll(pageable).map(AuthorResponse::from));
	}

	public AuthorResponse findById(Long id) {
		return AuthorResponse.from(getAuthorEntity(id));
	}

	@Transactional
	public AuthorResponse create(AuthorRequest request) {
		Author author = new Author();
		applyRequest(author, request);
		return AuthorResponse.from(authorRepository.save(author));
	}

	@Transactional
	public AuthorResponse update(Long id, AuthorRequest request) {
		Author author = getAuthorEntity(id);
		applyRequest(author, request);
		// Flush so updatedAt is refreshed before we build the response.
		return AuthorResponse.from(authorRepository.saveAndFlush(author));
	}

	/**
	 * An author who still has books cannot be deleted: the books table references the author.
	 * The author feature does not depend on the book feature, so we let the database enforce this.
	 */
	@Transactional
	public void delete(Long id) {
		Author author = getAuthorEntity(id);
		try {
			authorRepository.delete(author);
			// Run the DELETE now (instead of at commit) so a foreign-key violation is caught here.
			authorRepository.flush();
		}
		catch (DataIntegrityViolationException ex) {
			throw new ConflictException("Author %d still has books. Delete or reassign them first.".formatted(id));
		}
	}

	/**
	 * Loads the entity or throws 404. Public so other features' services (e.g. books) can reuse it.
	 */
	public Author getAuthorEntity(Long id) {
		return authorRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Author", id));
	}

	private void applyRequest(Author author, AuthorRequest request) {
		author.setName(request.name());
		author.setBio(request.bio());
	}

}
