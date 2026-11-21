package com.springapi.security;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private UserDetailsService userDetailsService;
    private JwtAuthenticationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                Base64.getEncoder().encodeToString(new byte[32]),
                60_000L);
        userDetailsService = mock(UserDetailsService.class);
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesEnabledUserWithValidJwt()
            throws ServletException, IOException {
        UserDetails userDetails = userDetails("patient", true);
        String jwt = jwtService.generateToken(userDetails);
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + jwt);
        when(userDetailsService.loadUserByUsername("patient"))
                .thenReturn(userDetails);

        filter.doFilterInternal(request, response, filterChain);

        var authentication =
                SecurityContextHolder.getContext().getAuthentication();
        assertTrue(authentication.isAuthenticated());
        assertSame(userDetails, authentication.getPrincipal());
        assertTrue(authentication.getAuthorities().contains(
                new SimpleGrantedAuthority("ROLE_PATIENT")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void clearsExistingAuthenticationForDisabledUserWithValidJwt()
            throws ServletException, IOException {
        UserDetails userDetails = userDetails("patient", false);
        String jwt = jwtService.generateToken(userDetails);
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + jwt);
        when(userDetailsService.loadUserByUsername("patient"))
                .thenReturn(userDetails);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "previous-user",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void invalidJwtDoesNotAuthenticateRequest()
            throws ServletException, IOException {
        UserDetails userDetails = userDetails("patient", true);
        String jwt = new JwtService(
                Base64.getEncoder().encodeToString(new byte[] {
                        1, 1, 1, 1, 1, 1, 1, 1,
                        1, 1, 1, 1, 1, 1, 1, 1,
                        1, 1, 1, 1, 1, 1, 1, 1,
                        1, 1, 1, 1, 1, 1, 1, 1
                }),
                60_000L).generateToken(userDetails);
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + jwt);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doesNotReplaceExistingAuthenticationForEnabledUser()
            throws ServletException, IOException {
        UserDetails userDetails = userDetails("patient", true);
        String jwt = jwtService.generateToken(userDetails);
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + jwt);
        when(userDetailsService.loadUserByUsername("patient"))
                .thenReturn(userDetails);
        var existingAuthentication =
                new UsernamePasswordAuthenticationToken(
                        "existing-user",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext()
                .setAuthentication(existingAuthentication);

        filter.doFilterInternal(request, response, filterChain);

        assertSame(existingAuthentication,
                SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    private UserDetails userDetails(String username, boolean enabled) {
        return org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password("password")
                .roles("PATIENT")
                .disabled(!enabled)
                .build();
    }
}
