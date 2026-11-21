package com.springapi.security;


import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.Map;

import com.sun.net.httpserver.HttpServer;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

class SupabaseJwtVerifierTest {

    private static final String EXPECTED_ISSUER =
            "https://example.supabase.co/auth/v1";
    private static final String EXPECTED_AUDIENCE = "authenticated";
    private static final String KEY_ID = "trusted-key";

    private ECKey trustedKey;
    private HttpServer jwksServer;
    private SupabaseJwtVerifier verifier;
    private String jwksUri;

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

        jwksUri = "http://127.0.0.1:"
                + jwksServer.getAddress().getPort() + "/jwks";
        verifier = verifierWithConfig(EXPECTED_ISSUER, EXPECTED_AUDIENCE);
    }

    @AfterEach
    void tearDown() {
        if (jwksServer != null) {
            jwksServer.stop(0);
        }
    }

    @Test
    void verifiesValidEs256TokenAndReturnsSubjectUuid() throws Exception {
        UUID subject = UUID.randomUUID();

        assertEquals(subject, verifier.verifyAccessToken(
                signedToken(trustedKey, JWSAlgorithm.ES256, KEY_ID,
                        EXPECTED_ISSUER, EXPECTED_AUDIENCE,
                        subject.toString(), Instant.now().plusSeconds(120),
                        null)));
    }

    @Test
    void rejectsInvalidSignature() throws Exception {
        ECKey otherKey = new ECKeyGenerator(Curve.P_256)
                .keyID(KEY_ID)
                .algorithm(JWSAlgorithm.ES256)
                .generate();
        String token = signedToken(
                otherKey, JWSAlgorithm.ES256, KEY_ID,
                EXPECTED_ISSUER, EXPECTED_AUDIENCE,
                UUID.randomUUID().toString(),
                Instant.now().plusSeconds(120), null);

        assertThrows(JwtException.class,
                () -> verifier.verifyAccessToken(token));
    }

    @Test
    void rejectsUnsupportedHs256Algorithm() throws Exception {
        String token = signedHs256Token(
                EXPECTED_ISSUER, EXPECTED_AUDIENCE,
                UUID.randomUUID().toString(),
                Instant.now().plusSeconds(120));

        assertThrows(JwtException.class,
                () -> verifier.verifyAccessToken(token));
    }

    @Test
    void rejectsWrongIssuer() throws Exception {
        String token = signedToken(
                trustedKey, JWSAlgorithm.ES256, KEY_ID,
                "https://wrong.supabase.co/auth/v1", EXPECTED_AUDIENCE,
                UUID.randomUUID().toString(),
                Instant.now().plusSeconds(120), null);

        assertThrows(JwtException.class,
                () -> verifier.verifyAccessToken(token));
    }

    @Test
    void rejectsWrongAudience() throws Exception {
        String token = signedToken(
                trustedKey, JWSAlgorithm.ES256, KEY_ID,
                EXPECTED_ISSUER, "wrong-audience",
                UUID.randomUUID().toString(),
                Instant.now().plusSeconds(120), null);

        assertThrows(JwtException.class,
                () -> verifier.verifyAccessToken(token));
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        String token = signedToken(
                trustedKey, JWSAlgorithm.ES256, KEY_ID,
                EXPECTED_ISSUER, EXPECTED_AUDIENCE,
                UUID.randomUUID().toString(),
                Instant.now().minusSeconds(120), null);

        assertThrows(JwtException.class,
                () -> verifier.verifyAccessToken(token));
    }

    @Test
    void rejectsTokenWithoutSubject() throws Exception {
        String token = signedToken(
                trustedKey, JWSAlgorithm.ES256, KEY_ID,
                EXPECTED_ISSUER, EXPECTED_AUDIENCE,
                null, Instant.now().plusSeconds(120), null);

        assertThrows(JwtException.class,
                () -> verifier.verifyAccessToken(token));
    }

    @Test
    void rejectsMalformedUuidSubject() throws Exception {
        String token = signedToken(
                trustedKey, JWSAlgorithm.ES256, KEY_ID,
                EXPECTED_ISSUER, EXPECTED_AUDIENCE,
                "not-a-uuid", Instant.now().plusSeconds(120), null);

        assertThrows(JwtException.class,
                () -> verifier.verifyAccessToken(token));
    }

    @Test
    void rejectsTokenWithUnknownKeyId() throws Exception {
        String token = signedToken(
                trustedKey, JWSAlgorithm.ES256, "unknown-key",
                EXPECTED_ISSUER, EXPECTED_AUDIENCE,
                UUID.randomUUID().toString(),
                Instant.now().plusSeconds(120), null);

        assertThrows(JwtException.class,
                () -> verifier.verifyAccessToken(token));
    }

    @Test
    void rejectsTokenBeforeNotBeforeTime() throws Exception {
        String token = signedToken(
                trustedKey, JWSAlgorithm.ES256, KEY_ID,
                EXPECTED_ISSUER, EXPECTED_AUDIENCE,
                UUID.randomUUID().toString(),
                Instant.now().plusSeconds(120),
                Instant.now().plusSeconds(120));

        assertThrows(JwtException.class,
                () -> verifier.verifyAccessToken(token));
    }

    @Test
    void missingIssuerConfigurationFailsClosed() throws Exception {
        SupabaseJwtVerifier unconfiguredVerifier =
                verifierWithConfig("", EXPECTED_AUDIENCE);
        String token = validToken();

        assertThrows(IllegalStateException.class,
                () -> unconfiguredVerifier.verifyAccessToken(token));
    }

    @Test
    void missingAudienceConfigurationFailsClosed() throws Exception {
        SupabaseJwtVerifier unconfiguredVerifier =
                verifierWithConfig(EXPECTED_ISSUER, "");
        String token = validToken();

        assertThrows(IllegalStateException.class,
                () -> unconfiguredVerifier.verifyAccessToken(token));
    }

    @Test
    void rejectsTokenWithoutExpirationClaim() throws Exception {
        String token = signedToken(
                trustedKey, JWSAlgorithm.ES256, KEY_ID,
                EXPECTED_ISSUER, EXPECTED_AUDIENCE,
                UUID.randomUUID().toString(), null, null);

        assertThrows(JwtException.class,
                () -> verifier.verifyAccessToken(token));
    }

    @Test
    void rejectsMalformedJwt() {
        assertThrows(JwtException.class,
                () -> verifier.verifyAccessToken("not-a-jwt"));
    }

    @Test
    void rejectsTokenWithoutKeyId() throws Exception {
        String token = signedToken(
                trustedKey, JWSAlgorithm.ES256, null,
                EXPECTED_ISSUER, EXPECTED_AUDIENCE,
                UUID.randomUUID().toString(),
                Instant.now().plusSeconds(120), null);

        assertThrows(JwtException.class,
                () -> verifier.verifyAccessToken(token));
    }

    @Test
     void extractsAndNormalizesVerifiedIdentity() throws Exception {
    UUID subject = UUID.randomUUID();

    String token = signedIdentityToken(
            subject.toString(),
            "  BRIAN@EXAMPLE.COM  ",
            "Brian Onyango");

    SupabaseIdentity identity =
            verifier.verifyAccessTokenIdentity(token);

    assertEquals(subject, identity.userId());
    assertEquals("brian@example.com", identity.email());
    assertEquals("Brian Onyango", identity.fullName());
}

@Test
void extractsFullNameFromUserMetadata() throws Exception {
    String token = signedIdentityToken(
            UUID.randomUUID().toString(),
            "brian@example.com",
            "Brian Onyango");

    SupabaseIdentity identity =
            verifier.verifyAccessTokenIdentity(token);

    assertEquals("Brian Onyango", identity.fullName());
}

@Test
void fallsBackToEmailUsernameWhenFullNameIsMissing()
        throws Exception {

    String token = signedIdentityToken(
            UUID.randomUUID().toString(),
            "brian@example.com",
            null);

    SupabaseIdentity identity =
            verifier.verifyAccessTokenIdentity(token);

    assertEquals("brian", identity.fullName());
}

@Test
void rejectsIdentityWithoutValidEmail() throws Exception {
    String token = signedIdentityToken(
            UUID.randomUUID().toString(),
            "not-an-email",
            "Brian Onyango");

    assertThrows(JwtException.class,
            () -> verifier.verifyAccessTokenIdentity(token));
}

private String signedIdentityToken(
        String subject,
        String email,
        String fullName) throws Exception {

    JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
            .issuer(EXPECTED_ISSUER)
            .audience(EXPECTED_AUDIENCE)
            .subject(subject)
            .expirationTime(Date.from(
                    Instant.now().plusSeconds(120)));

    if (email != null) {
        claims.claim("email", email);
    }

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

    jwt.sign(new ECDSASigner(trustedKey.toECPrivateKey()));

    return jwt.serialize();
}

    private String validToken() throws Exception {
        return signedToken(
                trustedKey, JWSAlgorithm.ES256, KEY_ID,
                EXPECTED_ISSUER, EXPECTED_AUDIENCE,
                UUID.randomUUID().toString(),
                Instant.now().plusSeconds(120), null);
    }

    private SupabaseJwtVerifier verifierWithConfig(
        String issuer,
        String audience) {

    return new SupabaseJwtVerifier(
            NimbusJwtDecoder.withJwkSetUri(jwksUri)
                    .jwsAlgorithm(SignatureAlgorithm.ES256)
                    .build(),
            issuer,
            audience);
}
    private String signedToken(
            ECKey signingKey,
            JWSAlgorithm algorithm,
            String keyId,
            String issuer,
            String audience,
            String subject,
            Instant expiresAt,
            Instant notBefore) throws Exception {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience);
        if (expiresAt != null) {
            claims.expirationTime(Date.from(expiresAt));
        }
        if (subject != null) {
            claims.subject(subject);
        }
        if (notBefore != null) {
            claims.notBeforeTime(Date.from(notBefore));
        }

        JWSHeader.Builder header = new JWSHeader.Builder(algorithm);
        if (keyId != null) {
            header.keyID(keyId);
        }
        SignedJWT jwt = new SignedJWT(header.build(), claims.build());
        jwt.sign(new ECDSASigner(signingKey.toECPrivateKey()));
        return jwt.serialize();
    }

    private String signedHs256Token(
            String issuer,
            String audience,
            String subject,
            Instant expiresAt) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .subject(subject)
                .expirationTime(Date.from(expiresAt))
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256)
                        .keyID(KEY_ID)
                        .build(),
                claims);
        jwt.sign(new MACSigner(new byte[32]));
        return jwt.serialize();
    }
}
