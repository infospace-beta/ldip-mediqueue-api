package com.springapi.user;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.springapi.common.dto.PageResponse;
import com.springapi.common.exception.ConflictException;
import com.springapi.common.exception.ResourceNotFoundException;
import com.springapi.hospital.Hospital;
import com.springapi.hospital.HospitalRepository;
import com.springapi.user.dto.UserRequest;
import com.springapi.user.dto.UserResponse;

@Service
@Transactional(readOnly = true)
public class UserService {

	private static final List<UserRole> STAFF_ROLES = List.of(
			UserRole.RECEPTIONIST,
			UserRole.MEDICAL_STAFF,
			UserRole.RESPONDER);

	private final UserRepository userRepository;
	private final HospitalRepository hospitalRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(
			UserRepository userRepository,
			HospitalRepository hospitalRepository,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.hospitalRepository = hospitalRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public PageResponse<UserResponse> findAll(
			UserRole role,
			Boolean enabled,
			Long hospitalId,
			String search,
			Pageable pageable) {
		return PageResponse.from(userRepository.findFiltered(
						role, enabled, hospitalId, normalizeSearch(search), pageable)
				.map(UserResponse::from));
	}

	public UserResponse findById(Long id) {
		return UserResponse.from(getUserEntity(id));
	}

	public PageResponse<UserResponse> findStaff(
			Long hospitalId,
			UserRole role,
			Pageable pageable) {
		List<UserRole> roles = role == null
				? STAFF_ROLES
				: STAFF_ROLES.contains(role)
						? List.of(role)
						: throwInvalidStaffFilter();
		return PageResponse.from(userRepository
				.findAllByHospitalIdAndRoleIn(hospitalId, roles, pageable)
				.map(UserResponse::from));
	}

	public PageResponse<UserResponse> findDoctors(
			Long hospitalId,
			Boolean onDuty,
			Pageable pageable) {
		return PageResponse.from(userRepository
				.findDoctors(hospitalId, onDuty, pageable)
				.map(UserResponse::from));
	}

	public UserResponse findDoctor(Long hospitalId, Long doctorId) {
		return userRepository.findByIdAndHospitalIdAndRole(
						doctorId, hospitalId, UserRole.DOCTOR)
				.map(UserResponse::from)
				.orElseThrow(() ->
						new ResourceNotFoundException("Doctor", doctorId));
	}

	@Transactional
	public UserResponse create(UserRequest request) {
		ensureUnique(request, null);
		validatePassword(request.password());
		User user = new User();
		applyRequest(user, request);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		return UserResponse.from(userRepository.save(user));
	}

	@Transactional
	public UserResponse update(Long id, UserRequest request) {
		User user = getUserEntity(id);
		ensureUnique(request, id);
		applyRequest(user, request);
		if (request.password() != null) {
			validatePassword(request.password());
			user.setPasswordHash(passwordEncoder.encode(request.password()));
		}
		// Flush so updatedAt is refreshed before we build the response.
		return UserResponse.from(userRepository.saveAndFlush(user));
	}

	@Transactional
	public UserResponse createStaff(Long hospitalId, UserRequest request) {
		requireStaffRole(request.role());
		return create(withHospitalId(request, hospitalId));
	}

	@Transactional
	public UserResponse updateStaff(
			Long hospitalId,
			Long userId,
			UserRequest request) {
		requireStaffRole(request.role());
		userRepository.findByIdAndHospitalIdAndRoleIn(
				userId, hospitalId, STAFF_ROLES)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Staff member", userId));
		return update(userId, withHospitalId(request, hospitalId));
	}

	@Transactional
	public void deactivateStaff(Long hospitalId, Long userId) {
		User user = userRepository.findByIdAndHospitalIdAndRoleIn(
				userId, hospitalId, STAFF_ROLES)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Staff member", userId));
		user.setEnabled(false);
		userRepository.save(user);
	}

	@Transactional
	public UserResponse createDoctor(Long hospitalId, UserRequest request) {
		requireDoctorRole(request.role());
		return create(withHospitalId(request, hospitalId));
	}

	@Transactional
	public UserResponse updateDoctor(
			Long hospitalId,
			Long doctorId,
			UserRequest request) {
		requireDoctorRole(request.role());
		userRepository.findByIdAndHospitalIdAndRole(
				doctorId, hospitalId, UserRole.DOCTOR)
				.orElseThrow(() ->
						new ResourceNotFoundException("Doctor", doctorId));
		return update(doctorId, withHospitalId(request, hospitalId));
	}

	@Transactional
	public UserResponse setDoctorOnDuty(
			Long hospitalId,
			Long doctorId,
			boolean onDuty) {
		User doctor = userRepository.findByIdAndHospitalIdAndRole(
				doctorId, hospitalId, UserRole.DOCTOR)
				.orElseThrow(() ->
						new ResourceNotFoundException("Doctor", doctorId));
		if (onDuty && !doctor.isEnabled()) {
			throw new ConflictException(
					"An inactive doctor cannot be marked on duty.");
		}
		doctor.setOnDuty(onDuty);
		return UserResponse.from(userRepository.saveAndFlush(doctor));
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
		String username = request.username().trim();
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		userRepository.findByUsername(username)
				.filter(existing -> currentUserId == null
						|| !Objects.equals(existing.getId(), currentUserId))
				.ifPresent(existing -> {
					throw new ConflictException("Username '%s' is already taken".formatted(username));
				});
		userRepository.findByEmail(email)
				.filter(existing -> currentUserId == null
						|| !Objects.equals(existing.getId(), currentUserId))
				.ifPresent(existing -> {
					throw new ConflictException("Email '%s' is already registered".formatted(email));
				});
	}

	private void applyRequest(User user, UserRequest request) {
		UserRole previousRole = user.getRole();
		user.setUsername(request.username().trim());
		user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
		user.setFullName(request.fullName().trim());
		user.setRole(request.role());
		user.setEnabled(request.enabled());

		if (Boolean.TRUE.equals(request.onDuty())
				&& request.role() != UserRole.DOCTOR) {
			throw new IllegalArgumentException(
					"Only doctors can be marked on duty.");
		}
		if (request.role() != UserRole.DOCTOR) {
			user.setOnDuty(false);
		} else if (request.onDuty() != null) {
			user.setOnDuty(request.onDuty());
		}

		Long hospitalId = request.hospitalId();
		if (hospitalId == null
				&& previousRole == request.role()
				&& user.getHospital() != null) {
			hospitalId = user.getHospital().getId();
		}
		if (request.role() == UserRole.SYSTEM_ADMIN) {
			if (hospitalId != null) {
				throw new IllegalArgumentException(
						"System administrators cannot be assigned to a hospital.");
			}
			user.setHospital(null);
			return;
		}
		if (requiresHospital(request.role()) && hospitalId == null) {
			throw new IllegalArgumentException(
					"Hospital ID is required for this user role.");
		}
		Long assignedHospitalId = hospitalId;
		Hospital hospital = assignedHospitalId == null
				? null
				: hospitalRepository.findById(assignedHospitalId)
						.orElseThrow(() -> new ResourceNotFoundException(
								"Hospital", assignedHospitalId));
		user.setHospital(hospital);
	}

	private UserRequest withHospitalId(UserRequest request, Long hospitalId) {
		if (request.hospitalId() != null
				&& !request.hospitalId().equals(hospitalId)) {
			throw new IllegalArgumentException(
					"Hospital ID in the request must match the URL.");
		}
		return new UserRequest(
				request.username(),
				request.email(),
				request.fullName(),
				request.role(),
				request.enabled(),
				hospitalId,
				request.password(),
				request.onDuty());
	}

	private void requireStaffRole(UserRole role) {
		if (!STAFF_ROLES.contains(role)) {
			throw new IllegalArgumentException(
					"Role must be RECEPTIONIST, MEDICAL_STAFF, or RESPONDER.");
		}
	}

	private List<UserRole> throwInvalidStaffFilter() {
		throw new IllegalArgumentException(
				"Staff role filter must be RECEPTIONIST, MEDICAL_STAFF, or RESPONDER.");
	}

	private void requireDoctorRole(UserRole role) {
		if (role != UserRole.DOCTOR) {
			throw new IllegalArgumentException("Role must be DOCTOR.");
		}
	}

	private boolean requiresHospital(UserRole role) {
		return role == UserRole.HOSPITAL_ADMIN
				|| role == UserRole.DOCTOR
				|| STAFF_ROLES.contains(role);
	}

	private String normalizeSearch(String search) {
		return search == null || search.isBlank() ? null : search.trim();
	}

	private void validatePassword(String password) {
		if (password == null || password.isBlank()) {
			throw new IllegalArgumentException(
					"Password is required when creating a user.");
		}
		if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new IllegalArgumentException(
					"Password must not exceed 72 bytes.");
		}
		if (password.length() < 8) {
			throw new IllegalArgumentException(
					"Password must be at least 8 characters.");
		}
	}

}
