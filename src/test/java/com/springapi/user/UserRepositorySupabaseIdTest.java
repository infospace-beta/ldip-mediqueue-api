package com.springapi.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import com.springapi.TestcontainersConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "JWT_SECRET=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class UserRepositorySupabaseIdTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findsUserBySupabaseUuid() {
        UUID supabaseUserId = UUID.randomUUID();
        User user = new User();
        user.setUsername("supabase-linked-user");
        user.setEmail("supabase-linked-user@example.com");
        user.setFullName("Supabase Linked User");
        user.setSupabaseUserId(supabaseUserId);
        User savedUser = userRepository.saveAndFlush(user);

        User retrievedUser = userRepository.findBySupabaseUserId(supabaseUserId)
                .orElseThrow();
        assertEquals(savedUser.getId(), retrievedUser.getId());
        assertEquals(supabaseUserId, retrievedUser.getSupabaseUserId());
    }

    @Test
    void returnsEmptyWhenSupabaseUuidIsNotMapped() {
        assertTrue(userRepository.findBySupabaseUserId(UUID.randomUUID())
                .isEmpty());
    }
}
