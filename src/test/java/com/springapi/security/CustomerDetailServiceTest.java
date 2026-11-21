package com.springapi.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.springapi.user.User;
import com.springapi.user.UserRepository;
import com.springapi.user.UserRole;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void handlesSupabaseUserWithNullPasswordHash() {
        User user = createUser(null);

        when(userRepository.findByUsername("brian"))
                .thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("brian");

        assertEquals("", result.getPassword());
        assertEquals("brian", result.getUsername());
        assertTrue(result.isEnabled());
    }

    @Test
    void preservesTraditionalUserPasswordHash() {
        String passwordHash = "$2a$10$exampleExistingHash";

        User user = createUser(passwordHash);

        when(userRepository.findByUsername("brian"))
                .thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("brian");

        assertEquals(passwordHash, result.getPassword());
    }

    private User createUser(String passwordHash) {
        User user = new User();

        user.setUsername("brian");
        user.setEmail("brian@example.com");
        user.setFullName("Brian Onyango");
        user.setPasswordHash(passwordHash);
        user.setRole(UserRole.PATIENT);
        user.setEnabled(true);

        return user;
    }
}