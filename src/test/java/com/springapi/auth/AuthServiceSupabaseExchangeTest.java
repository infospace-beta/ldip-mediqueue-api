package com.springapi.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import com.springapi.auth.dto.AuthResponse;
import com.springapi.security.CustomUserDetailsService;
import com.springapi.security.JwtService;
import com.springapi.security.SupabaseJwtVerifier;
import com.springapi.user.User;
import com.springapi.user.UserRepository;
import com.sun.net.httpserver.HttpServer;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.mockito.Mockito.times;

import org.mockito.ArgumentCaptor;

import com.springapi.user.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceSupabaseExchangeTest {

    private static final String EXPECTED_ISSUER =
            "https://example.supabase.co/auth/v1";
    private static final String EXPECTED_AUDIENCE = "authenticated";
    private static final String KEY_ID = "local-test-key";
    private static final long EXPIRATION_MS = 900_000L;

    private ECKey trustedKey;
    private HttpServer jwksServer;
    private AuthService authService;
    private UserRepository userRepository;
    private CustomUserDetailsService customUserDetailsService;
    private JwtService jwtService;

    @BeforeEach
    void setUp() throws IOException, JOSEException {
        trustedKey = new ECKeyGenerator(Curve.P_256)
                .keyID(KEY_ID)
                .algorithm(JWSAlgorithm.ES256)
                .generate();

        byte[] jwks = new JWKSet(trustedKey.toPublicJWK())
                .toString()
                .getBytes(StandardCharsets.UTF_8);
        jwksServer = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);
        jwksServer.createContext("/jwks", exchange -> {
            exchange.getResponseHeaders().set(
                    "Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jwks.length);
            try (var output = exchange.getResponseBody()) {
                output.write(jwks);
            }
        });
        jwksServer.start();

        String jwksUri = "http://127.0.0.1:"
                + jwksServer.getAddress().getPort() + "/jwks";
        SupabaseJwtVerifier verifier = new SupabaseJwtVerifier(
                jwksUri, EXPECTED_ISSUER, EXPECTED_AUDIENCE);

        userRepository = mock(UserRepository.class);
        customUserDetailsService = mock(CustomUserDetailsService.class);
        jwtService = new JwtService(
                Base64.getEncoder().encodeToString(new byte[32]),
                EXPIRATION_MS);
        authService = new AuthService(
                userRepository,
                mock(PasswordEncoder.class),
                mock(AuthenticationManager.class),
                jwtService,
                EXPIRATION_MS,
                verifier,
                customUserDetailsService);
    }

    @AfterEach
    void tearDown() {
        if (jwksServer != null) {
            jwksServer.stop(0);
        }
    }

    @Test
    void exchangesValidLocallySignedSupabaseToken() throws Exception {
        UUID supabaseUserId = UUID.randomUUID();
        User user = mappedUser(supabaseUserId);
        UserDetails userDetails =
                org.springframework.security.core.userdetails.User
                        .withUsername(user.getUsername())
                        .password("test-password-hash")
                        .roles("PATIENT")
                        .build();
        when(userRepository.findBySupabaseUserId(supabaseUserId))
                .thenReturn(Optional.of(user));
        when(customUserDetailsService.loadUserByUsername(user.getUsername()))
                .thenReturn(userDetails);

        AuthResponse response = authService.exchangeSupabaseToken(
                signedToken(trustedKey, supabaseUserId));

        assertTrue(response.accessToken() != null
                && !response.accessToken().isBlank());
        assertEquals("Bearer", response.tokenType());
        assertEquals(EXPIRATION_MS / 1000, response.expiresIn());
        assertEquals(user.getUsername(),
                jwtService.extractUsername(response.accessToken()));
        verify(userRepository).findBySupabaseUserId(supabaseUserId);
    }

    @Test
void createsPatientAccountForFirstSupabaseExchange() throws Exception {
    UUID supabaseUserId = UUID.randomUUID();

    String email = "new.patient@example.com";
    String username = "mq_" +
            supabaseUserId.toString().replace("-", "");

    when(userRepository.findBySupabaseUserId(supabaseUserId))
            .thenReturn(Optional.empty());

    when(userRepository.findByEmail(email))
            .thenReturn(Optional.empty());

    when(userRepository.findByUsername(username))
            .thenReturn(Optional.empty());

    when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    UserDetails userDetails =
            org.springframework.security.core.userdetails.User
                    .withUsername(username)
                    .password("")
                    .roles("PATIENT")
                    .build();

    when(customUserDetailsService.loadUserByUsername(username))
            .thenReturn(userDetails);

    AuthResponse response = authService.exchangeSupabaseToken(
            signedToken(
                    trustedKey,
                    supabaseUserId,
                    "New.Patient@Example.com",
                    "New Patient"));

    ArgumentCaptor<User> userCaptor =
            ArgumentCaptor.forClass(User.class);

    verify(userRepository).save(userCaptor.capture());

    User savedUser = userCaptor.getValue();

    assertEquals(username, savedUser.getUsername());
    assertEquals(email, savedUser.getEmail());
    assertEquals("New Patient", savedUser.getFullName());
    assertEquals(supabaseUserId, savedUser.getSupabaseUserId());
    assertEquals(UserRole.PATIENT, savedUser.getRole());
    assertTrue(savedUser.isEnabled());
    assertFalse(savedUser.isOnDuty());
    assertNull(savedUser.getPasswordHash());
    assertNull(savedUser.getHospital());

    assertTrue(response.accessToken() != null
            && !response.accessToken().isBlank());
}

@Test
void reusesExistingAccountOnRepeatedSupabaseExchange() throws Exception {
    UUID supabaseUserId = UUID.randomUUID();
        String username = "mq_" + supabaseUserId.toString().replace("-", "");

    User user = mappedUser(supabaseUserId);
        user.setUsername(username);

    when(userRepository.findBySupabaseUserId(supabaseUserId))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(user));

    when(userRepository.findByEmail("mapped-user@example.com"))
            .thenReturn(Optional.empty());

    when(userRepository.findByUsername(
            username))
            .thenReturn(Optional.empty());

    when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    UserDetails userDetails =
            org.springframework.security.core.userdetails.User
                    .withUsername(username)
                    .password("")
                    .roles("PATIENT")
                    .build();

    when(customUserDetailsService.loadUserByUsername(userDetails.getUsername()))
            .thenReturn(userDetails);

    String token = signedToken(trustedKey, supabaseUserId);

    AuthResponse firstResponse =
            authService.exchangeSupabaseToken(token);

    AuthResponse secondResponse =
            authService.exchangeSupabaseToken(token);

    assertTrue(firstResponse.accessToken() != null
            && !firstResponse.accessToken().isBlank());

    assertTrue(secondResponse.accessToken() != null
            && !secondResponse.accessToken().isBlank());

    verify(userRepository, times(1)).save(any(User.class));

    verify(userRepository, times(2))
            .findBySupabaseUserId(supabaseUserId);
}

@Test
void rejectsSupabaseExchangeWhenEmailBelongsToAnotherAccount()
        throws Exception {

    UUID supabaseUserId = UUID.randomUUID();

    User existingUser = mappedUser(UUID.randomUUID());

    when(userRepository.findBySupabaseUserId(supabaseUserId))
            .thenReturn(Optional.empty());

    when(userRepository.findByEmail("mapped-user@example.com"))
            .thenReturn(Optional.of(existingUser));

    assertThrows(
            BadCredentialsException.class,
            () -> authService.exchangeSupabaseToken(
                    signedToken(trustedKey, supabaseUserId)));

    verify(userRepository, never()).save(any(User.class));

    verify(customUserDetailsService, never())
            .loadUserByUsername(any(String.class));
}

    @Test
    void rejectsTokenSignedByUntrustedLocalKey() throws Exception {
        ECKey untrustedKey = new ECKeyGenerator(Curve.P_256)
                .keyID(KEY_ID)
                .algorithm(JWSAlgorithm.ES256)
                .generate();

        assertThrows(BadCredentialsException.class,
                () -> authService.exchangeSupabaseToken(
                        signedToken(untrustedKey, UUID.randomUUID())));

        verify(userRepository, never())
                .findBySupabaseUserId(any(UUID.class));
    }

    private User mappedUser(UUID supabaseUserId) {
        User user = new User();
        user.setUsername("mapped-user");
        user.setEmail("mapped-user@example.com");
        user.setSupabaseUserId(supabaseUserId);
        user.setEnabled(true);
        return user;
    }

   private String signedToken(ECKey signingKey, UUID subject)
        throws Exception {

    return signedToken(
            signingKey,
            subject,
            "mapped-user@example.com",
            "Mapped User");
}

private String signedToken(
        ECKey signingKey,
        UUID subject,
        String email,
        String fullName) throws Exception {

    JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
            .issuer(EXPECTED_ISSUER)
            .audience(EXPECTED_AUDIENCE)
            .subject(subject.toString())
            .issueTime(new Date())
            .expirationTime(
                    new Date(System.currentTimeMillis() + 120_000))
            .claim("email", email);

    if (fullName != null) {
        claims.claim(
                "user_metadata",
                Map.of("full_name", fullName));
    }

    SignedJWT jwt = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(KEY_ID)
                    .build(),
            claims.build());

    jwt.sign(new ECDSASigner(signingKey.toECPrivateKey()));

    return jwt.serialize();
}
}
