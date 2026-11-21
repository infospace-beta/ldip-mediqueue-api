package com.springapi.security;

import java.util.UUID;

/**
 * Identity information extracted from a verified Supabase access token.
 */
public record SupabaseIdentity(
        UUID userId,
        String email,
        String fullName
) {
}