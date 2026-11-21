
package com.springapi.auth;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.springapi.auth.dto.AuthResponse;
import com.springapi.auth.dto.CurrentUserResponse;
import com.springapi.auth.dto.LoginRequest;
import com.springapi.auth.dto.RegisterRequest;
import com.springapi.common.exception.ConflictException;
import com.springapi.common.exception.ResourceNotFoundException;
import com.springapi.security.CustomUserDetailsService;
import com.springapi.security.JwtService;
import com.springapi.security.SupabaseIdentity;
import com.springapi.security.SupabaseJwtVerifier;
import com.springapi.user.User;
import com.springapi.user.UserRepository;
import com.springapi.user.UserRole;

@Service
public class AuthService {

    private static final Logger log =
            LoggerFactory.getLogger(AuthService.class);

    private static final String AUTHENTICATION_FAILED =
            "Authentication failed";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SupabaseJwtVerifier supabaseJwtVerifier;
    private final CustomUserDetailsService customUserDetailsService;
    private final long accessTokenExpirationMs;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            @Value("${app.jwt.expiration-ms}") long accessTokenExpirationMs,
            SupabaseJwtVerifier supabaseJwtVerifier,
            CustomUserDetailsService customUserDetailsService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.supabaseJwtVerifier = supabaseJwtVerifier;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Transactional
    public User register(RegisterRequest request) {

        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.findByUsername(username).isPresent()) {
            throw new ConflictException("Username is already taken");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("Email is already registered");
        }

        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(
                    "Password must not exceed 72 bytes");
        }

        User user = new User();

        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(request.fullName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.PATIENT);
        user.setEnabled(true);

        return userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.usernameOrEmail(),
                                request.password()
                        )
                );

        String token = jwtService.generateToken(
                (UserDetails) authentication.getPrincipal()
        );

        return new AuthResponse(
                token,
                "Bearer",
                accessTokenExpirationMs / 1000
        );
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", username));

        return CurrentUserResponse.from(user);
    }

    /**
     * Verifies a Supabase access token, provisions a local patient
     * account when necessary, and issues a MediQueue JWT.
     */
    @Transactional
    public AuthResponse exchangeSupabaseToken(String accessToken) {

        final SupabaseIdentity identity;

        // Verify the Supabase token and extract its trusted identity.
        try {
            identity = supabaseJwtVerifier.verifyAccessTokenIdentity(
                    accessToken
            );
        } catch (JwtException exception) {

            log.warn(
                    "Supabase JWT verification failed: {}",
                    exception.getMessage()
            );

            throw new BadCredentialsException(AUTHENTICATION_FAILED);
        }

        // Reuse the existing local account for this Supabase UUID.
        User user = userRepository.findBySupabaseUserId(identity.userId())
                .orElseGet(() -> createSupabasePatient(identity));

        if (!user.isEnabled()) {
            log.warn(
                    "Supabase exchange rejected: local account is disabled"
            );

            throw new BadCredentialsException(AUTHENTICATION_FAILED);
        }

        UserDetails userDetails =
                customUserDetailsService.loadUserByUsername(
                        user.getUsername()
                );

        if (!userDetails.isEnabled()) {
            log.warn(
                    "Supabase exchange rejected: UserDetails account is disabled"
            );

            throw new BadCredentialsException(AUTHENTICATION_FAILED);
        }

        // Issue the MediQueue JWT after successful verification.
        String token = jwtService.generateToken(userDetails);

        log.info("Supabase identity successfully exchanged for MediQueue JWT");

        return new AuthResponse(
                token,
                "Bearer",
                accessTokenExpirationMs / 1000
        );
    }

    /**
     * Creates a local patient account for a verified Supabase identity.
     * Existing accounts are never linked using email alone.
     */
    private User createSupabasePatient(SupabaseIdentity identity) {

        // Never link an unrelated existing account just because its email matches.
        if (userRepository.findByEmail(identity.email()).isPresent()) {

            log.warn(
                    "Supabase exchange rejected: email already belongs to another local account"
            );

            throw new BadCredentialsException(AUTHENTICATION_FAILED);
        }

        // Generate a stable username from the verified Supabase UUID.
        String username = "mq_" +
                identity.userId().toString().replace("-", "");

        if (userRepository.findByUsername(username).isPresent()) {

            log.warn(
                    "Supabase exchange rejected: generated username already exists"
            );

            throw new BadCredentialsException(AUTHENTICATION_FAILED);
        }

        String fullName = identity.fullName().trim();

        // Match the maximum length of the users.full_name database column.
        if (fullName.isEmpty() || fullName.length() > 100) {

            log.warn(
                    "Supabase exchange rejected: invalid full name"
            );

            throw new BadCredentialsException(AUTHENTICATION_FAILED);
        }

        User user = new User();

        user.setUsername(username);
        user.setEmail(identity.email());
        user.setFullName(fullName);
        user.setSupabaseUserId(identity.userId());

        // Supabase manages the password, not the local Spring Boot backend.
        user.setPasswordHash(null);

        // Public Supabase registration must never assign privileged roles.
        user.setRole(UserRole.PATIENT);
        user.setEnabled(true);
        user.setOnDuty(false);
        user.setHospital(null);

        return userRepository.save(user);
    }
}


