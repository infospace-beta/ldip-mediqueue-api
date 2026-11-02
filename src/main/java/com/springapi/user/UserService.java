package com.springapi.user;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.springapi.common.dto.PageResponse;
import com.springapi.common.exception.ConflictException;
import com.springapi.common.exception.ResourceNotFoundException;
import com.springapi.user.dto.UserRequest;
import com.springapi.user.dto.UserResponse;

@Service
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public PageResponse<UserResponse> findAll(Pageable pageable) {
		return PageResponse.from(userRepository.findAll(pageable).map(UserResponse::from));
	}

	public UserResponse findById(Long id) {
		return UserResponse.from(getUserEntity(id));
	}

	@Transactional
	public UserResponse create(UserRequest request) {
		ensureUnique(request, null);
		User user = new User();
		applyRequest(user, request);
		return UserResponse.from(userRepository.save(user));
	}

	@Transactional
	public UserResponse update(Long id, UserRequest request) {
		User user = getUserEntity(id);
		ensureUnique(request, id);
		applyRequest(user, request);
		// Flush so updatedAt is refreshed before we build the response.
		return UserResponse.from(userRepository.saveAndFlush(user));
	}

	/** The user's bookmarks are removed by the database (ON DELETE CASCADE). */
	@Transactional
	public void delete(Long id) {
		userRepository.delete(getUserEntity(id));
	}

	/**
	 * Loads the entity or throws 404. Public so other features' services (e.g. bookmarks) can reuse it.
	 */
	public User getUserEntity(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User", id));
	}

	private void ensureUnique(UserRequest request, Long currentUserId) {
		userRepository.findByUsername(request.username())
				.filter(existing -> !existing.getId().equals(currentUserId))
				.ifPresent(existing -> {
					throw new ConflictException("Username '%s' is already taken".formatted(request.username()));
				});
		userRepository.findByEmail(request.email())
				.filter(existing -> !existing.getId().equals(currentUserId))
				.ifPresent(existing -> {
					throw new ConflictException("Email '%s' is already registered".formatted(request.email()));
				});
	}

	private void applyRequest(User user, UserRequest request) {
		user.setUsername(request.username());
		user.setEmail(request.email());
		user.setFullName(request.fullName());
	}

}
