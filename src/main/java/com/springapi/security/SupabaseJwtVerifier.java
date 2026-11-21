
package com.springapi.security;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Verifies Supabase access tokens signed with the project's ES256 key.
 */
@Component
public class SupabaseJwtVerifier {

    private static final String DEFAULT_JWKS_URI =
            "https://qbknxkcvazjrjkuugogk.supabase.co/auth/v1/.well-known/jwks.json";

    private final JwtDecoder jwtDecoder;
    private final String issuer;
    private final String audience;

    @Autowired
    public SupabaseJwtVerifier(
            @Value("${SUPABASE_JWKS_URL:" + DEFAULT_JWKS_URI + "}") String jwksUri,
            @Value("${SUPABASE_JWT_ISSUER:}") String issuer,
            @Value("${SUPABASE_JWT_AUDIENCE:}") String audience) {

        this(createDecoder(jwksUri), issuer, audience);
    }

    SupabaseJwtVerifier(
            JwtDecoder jwtDecoder,
            String issuer,
            String audience) {

        this.jwtDecoder = jwtDecoder;
        this.issuer = issuer;
        this.audience = audience;
    }

    private static JwtDecoder createDecoder(String jwksUri) {
        if (jwksUri == null || jwksUri.isBlank()) {
            throw new IllegalStateException(
                    "Supabase JWKS URL must be configured");
        }

        return NimbusJwtDecoder.withJwkSetUri(jwksUri)
                .jwsAlgorithm(SignatureAlgorithm.ES256)
                .build();
    }

    /**
     * Verifies the token and returns the Supabase user UUID.
     * Does not require an email claim.
     */
    public UUID verifyAccessToken(String accessToken) {
        Jwt jwt = decodeAndValidateAccessToken(accessToken);
        return extractUserId(jwt);
    }

    /**
     * Verifies the token and extracts the user's identity.
     */
    public SupabaseIdentity verifyAccessTokenIdentity(String accessToken) {
        Jwt jwt = decodeAndValidateAccessToken(accessToken);

        UUID userId = extractUserId(jwt);

        String email = jwt.getClaimAsString("email");

        if (email == null || email.isBlank()
                || email.length() > 255
                || !email.contains("@")
                || email.endsWith("@")) {
            throw new JwtException(
                    "Supabase JWT email is missing or invalid");
        }

        email = email.trim().toLowerCase(Locale.ROOT);

        String fullName = null;
        Object metadata = jwt.getClaims().get("user_metadata");

        if (metadata instanceof Map<?, ?> userMetadata) {
            Object name = userMetadata.get("full_name");

            if (name instanceof String value && !value.isBlank()) {
                fullName = value.trim();
            }
        }

        if (fullName == null || fullName.isBlank()
                || fullName.length() > 100) {

            fullName = email.substring(0, email.indexOf("@"));

            if (fullName.length() > 100) {
                fullName = "MediQueue User";
            }
        }

        return new SupabaseIdentity(userId, email, fullName);
    }

    /**
     * Extracts the UUID from an already verified JWT.
     */
    private UUID extractUserId(Jwt jwt) {
        String subject = jwt.getSubject();

        if (subject == null || subject.isBlank()) {
            throw new JwtException(
                    "Supabase JWT subject is missing");
        }

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException exception) {
            throw new JwtException(
                    "Supabase JWT subject is not a UUID",
                    exception);
        }
    }

    /**
     * Performs JWT signature and security-claim validation.
     */
    private Jwt decodeAndValidateAccessToken(String accessToken) {

        if (issuer == null || issuer.isBlank()
                || audience == null || audience.isBlank()) {
            throw new IllegalStateException(
                    "Supabase JWT issuer and audience must be configured");
        }

        Jwt jwt = jwtDecoder.decode(accessToken);

        Object keyId = jwt.getHeaders().get("kid");

        if (!(keyId instanceof String id) || id.isBlank()) {
            throw new JwtException(
                    "Supabase JWT must contain a key ID");
        }

        if (jwt.getIssuer() == null
                || !issuer.equals(jwt.getIssuer().toString())) {
            throw new JwtException(
                    "Supabase JWT issuer is invalid");
        }

        if (jwt.getAudience() == null
                || !jwt.getAudience().contains(audience)) {
            throw new JwtException(
                    "Supabase JWT audience is invalid");
        }

        Instant now = Instant.now();

        if (jwt.getExpiresAt() == null
                || !jwt.getExpiresAt().isAfter(now)) {
            throw new JwtException(
                    "Supabase JWT is expired or has no expiry");
        }

        if (jwt.getNotBefore() != null
                && jwt.getNotBefore().isAfter(now)) {
            throw new JwtException(
                    "Supabase JWT is not yet valid");
        }

        if (jwt.getSubject() == null
                || jwt.getSubject().isBlank()) {
            throw new JwtException(
                    "Supabase JWT subject is missing");
        }

        return jwt;
    }
}


