
package com.springapi.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.springapi.hospital.Hospital;
import com.springapi.security.CustomUserDetailsService;
import com.springapi.security.JwtService;
import com.springapi.security.SupabaseIdentity;
import com.springapi.security.SupabaseJwtVerifier;
import com.springapi.user.User;
import com.springapi.user.UserRepository;
import com.springapi.user.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtException;

import com.springapi.auth.dto.AuthResponse;

class AuthServiceTest {

    private static final long EXPIRATION_MS = 900_000L;

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private JwtService jwtService;
    private SupabaseJwtVerifier supabaseJwtVerifier;
    private CustomUserDetailsService customUserDetailsService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authenticationManager = mock(AuthenticationManager.class);
        jwtService = mock(JwtService.class);
        supabaseJwtVerifier = mock(SupabaseJwtVerifier.class);
        customUserDetailsService = mock(CustomUserDetailsService.class);

        authService = new AuthService(
                userRepository,
                passwordEncoder,
                authenticationManager,
                jwtService,
                EXPIRATION_MS,
                supabaseJwtVerifier,
                customUserDetailsService);
    }

    @Test
    void exchangesVerifiedSupabaseUserForExistingMediQueueJwt() {
        UUID supabaseUserId = UUID.randomUUID();
        SupabaseIdentity identity = identity(supabaseUserId);

        Hospital hospital = new Hospital();
        User user = mappedUser(supabaseUserId, true);
        user.setRole(UserRole.DOCTOR);
        user.setHospital(hospital);

        UserDetails userDetails =
                org.springframework.security.core.userdetails.User
                        .withUsername(user.getUsername())
                        .password("existing-password-hash")
                        .roles(UserRole.DOCTOR.name())
                        .build();

        when(supabaseJwtVerifier.verifyAccessTokenIdentity(
                "supabase-access-token"))
                .thenReturn(identity);

        when(userRepository.findBySupabaseUserId(supabaseUserId))
                .thenReturn(Optional.of(user));

        when(customUserDetailsService.loadUserByUsername(user.getUsername()))
                .thenReturn(userDetails);

        when(jwtService.generateToken(userDetails))
                .thenReturn("mediqueue-jwt");

        AuthResponse response =
                authService.exchangeSupabaseToken("supabase-access-token");

        assertEquals("mediqueue-jwt", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(EXPIRATION_MS / 1000, response.expiresIn());
        assertEquals(UserRole.DOCTOR, user.getRole());
        assertSame(hospital, user.getHospital());
        assertEquals(supabaseUserId, user.getSupabaseUserId());

        verify(userRepository).findBySupabaseUserId(supabaseUserId);
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(customUserDetailsService)
                .loadUserByUsername(user.getUsername());
        verify(jwtService).generateToken(userDetails);
    }

    @Test
    void translatesInvalidSupabaseJwtToAuthenticationFailure() {
        when(supabaseJwtVerifier.verifyAccessTokenIdentity(
                "invalid-access-token"))
                .thenThrow(new JwtException("verifier detail"));

        assertThrows(BadCredentialsException.class,
                () -> authService.exchangeSupabaseToken(
                        "invalid-access-token"));

        verify(userRepository, never())
                .findBySupabaseUserId(any(UUID.class));
    }

    @Test
    void leavesSupabaseConfigurationFailureAsServerError() {
        when(supabaseJwtVerifier.verifyAccessTokenIdentity(
                "supabase-access-token"))
                .thenThrow(new IllegalStateException(
                        "Supabase JWT issuer and audience must be configured"));

        assertThrows(IllegalStateException.class,
                () -> authService.exchangeSupabaseToken(
                        "supabase-access-token"));

        verify(userRepository, never())
                .findBySupabaseUserId(any(UUID.class));
    }

    @Test
    void provisionsPatientForUnknownSupabaseUuid() {
        UUID supabaseUserId = UUID.randomUUID();
        SupabaseIdentity identity = identity(supabaseUserId);

        String username = "mq_" +
                supabaseUserId.toString().replace("-", "");

        when(supabaseJwtVerifier.verifyAccessTokenIdentity(
                "supabase-access-token"))
                .thenReturn(identity);

        when(userRepository.findBySupabaseUserId(supabaseUserId))
                .thenReturn(Optional.empty());

        when(userRepository.findByEmail(identity.email()))
                .thenReturn(Optional.empty());

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.empty());

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserDetails userDetails =
                org.springframework.security.core.userdetails.User
                        .withUsername(username)
                        .password("")
                        .roles(UserRole.PATIENT.name())
                        .build();

        when(customUserDetailsService.loadUserByUsername(username))
                .thenReturn(userDetails);

        when(jwtService.generateToken(userDetails))
                .thenReturn("new-patient-jwt");

        AuthResponse response =
                authService.exchangeSupabaseToken("supabase-access-token");

        assertEquals("new-patient-jwt", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(EXPIRATION_MS / 1000, response.expiresIn());

        verify(userRepository).findBySupabaseUserId(supabaseUserId);
        verify(userRepository).findByEmail(identity.email());
        verify(userRepository).findByUsername(username);
        verify(userRepository).save(any(User.class));
        verify(customUserDetailsService).loadUserByUsername(username);
        verify(jwtService).generateToken(userDetails);
    }

    @Test
    void rejectsDisabledMappedUserWithoutIssuingJwt() {
        UUID supabaseUserId = UUID.randomUUID();

        when(supabaseJwtVerifier.verifyAccessTokenIdentity(
                "supabase-access-token"))
                .thenReturn(identity(supabaseUserId));

        when(userRepository.findBySupabaseUserId(supabaseUserId))
                .thenReturn(Optional.of(mappedUser(supabaseUserId, false)));

        assertThrows(BadCredentialsException.class,
                () -> authService.exchangeSupabaseToken(
                        "supabase-access-token"));

        verify(customUserDetailsService, never())
                .loadUserByUsername(anyString());
        verify(jwtService, never()).generateToken(any(UserDetails.class));
    }

    private SupabaseIdentity identity(UUID userId) {
        return new SupabaseIdentity(
                userId,
                "patient@example.com",
                "Test Patient");
    }

    private User mappedUser(UUID supabaseUserId, boolean enabled) {
        User user = new User();
        user.setUsername("mapped-user");
        user.setEmail("mapped-user@example.com");
        user.setSupabaseUserId(supabaseUserId);
        user.setEnabled(enabled);
        return user;
    }
}
