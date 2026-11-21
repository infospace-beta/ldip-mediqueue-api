package com.springapi.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import com.springapi.common.exception.GlobalExceptionHandler;
import com.springapi.hospital.HospitalRepository;
import com.springapi.user.dto.UserResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.bind.support.WebDataBinderFactory;

class CurrentUserControllerTest {

	private UserRepository userRepository;
	private UserService userService;
	private CurrentUserController controller;
	private MockMvc mockMvc;
	private UserDetails principal;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		userService = new UserService(
				userRepository,
				mock(HospitalRepository.class),
				mock(PasswordEncoder.class));
		controller = new CurrentUserController(userService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.setCustomArgumentResolvers(new CurrentUserPrincipalResolver())
				.build();
	}

	@Test
	void patientCanRetrieveTheirOwnProfile() throws Exception {
		User user = user("patient-user", UserRole.PATIENT);
		authenticateAs("patient-user", UserRole.PATIENT);
		when(userRepository.findByUsername("patient-user"))
				.thenReturn(Optional.of(user));

		mockMvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("patient-user"))
				.andExpect(jsonPath("$.role").value("PATIENT"))
				.andExpect(jsonPath("$.fullName").value("Patient User"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.supabaseUserId").doesNotExist());

		verify(userRepository).findByUsername("patient-user");
	}

	@Test
	void responderCanRetrieveTheirOwnProfile() throws Exception {
		User user = user("responder-user", UserRole.RESPONDER);
		authenticateAs("responder-user", UserRole.RESPONDER);
		when(userRepository.findByUsername("responder-user"))
				.thenReturn(Optional.of(user));

		mockMvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("responder-user"))
				.andExpect(jsonPath("$.role").value("RESPONDER"));

		verify(userRepository).findByUsername("responder-user");
	}

	@Test
	void ignoresClientIdentityFieldsAndCannotAddressAnotherUsersProfile()
			throws Exception {
		User user = user("patient-user", UserRole.PATIENT);
		authenticateAs("patient-user", UserRole.PATIENT);
		when(userRepository.findByUsername("patient-user"))
				.thenReturn(Optional.of(user));

		mockMvc.perform(get("/api/v1/users/me")
						.param("userId", "999")
						.param("username", "other-user")
						.param("role", "SYSTEM_ADMIN")
						.param("hospitalId", "88"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("patient-user"))
				.andExpect(jsonPath("$.role").value("PATIENT"));

		mockMvc.perform(get("/api/v1/users/me/999"))
				.andExpect(status().isNotFound());

		verify(userRepository).findByUsername("patient-user");
		verify(userRepository, never()).findById(anyLong());
	}

	@Test
	void returnsNotFoundWhenPrincipalHasNoDatabaseUser() throws Exception {
		authenticateAs("missing-user", UserRole.PATIENT);
		when(userRepository.findByUsername("missing-user"))
				.thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value(
						"Current user with id profile not found"));
	}

	@Test
	void existingUserControllerRemainsRestrictedToSystemAdmins()
			throws Exception {
		PreAuthorize restriction =
				UserController.class.getAnnotation(PreAuthorize.class);

		assertNotNull(restriction);
		assertEquals("hasRole('SYSTEM_ADMIN')", restriction.value());
	}

	@Test
	void currentUserEndpointRequiresAuthentication()
			throws NoSuchMethodException {
		PreAuthorize restriction = CurrentUserController.class
				.getMethod("getCurrentUser", UserDetails.class)
				.getAnnotation(PreAuthorize.class);

		assertNotNull(restriction);
		assertEquals("isAuthenticated()", restriction.value());
	}

	private void authenticateAs(String username, UserRole role) {
		principal = org.springframework.security.core.userdetails.User
				.withUsername(username)
				.password("")
				.roles(role.name())
				.build();
	}

	private User user(String username, UserRole role) {
		User user = new User();
		user.setUsername(username);
		user.setEmail(username + "@example.com");
		user.setFullName(role == UserRole.PATIENT
				? "Patient User"
				: "Responder User");
		user.setRole(role);
		user.setEnabled(true);
		return user;
	}

	private class CurrentUserPrincipalResolver
			implements HandlerMethodArgumentResolver {

		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return parameter.hasParameterAnnotation(
							AuthenticationPrincipal.class)
					&& UserDetails.class.isAssignableFrom(
							parameter.getParameterType());
		}

		@Override
		public Object resolveArgument(
				MethodParameter parameter,
				ModelAndViewContainer mavContainer,
				NativeWebRequest webRequest,
				WebDataBinderFactory binderFactory) {
			return principal;
		}
	}
}
