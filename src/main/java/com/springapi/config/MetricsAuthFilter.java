package com.springapi.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class MetricsAuthFilter extends OncePerRequestFilter {

    private static final String METRICS_PATH = "/actuator/prometheus";

    private final String scrapeToken;

    public MetricsAuthFilter(
            @Value("${app.metrics.scrape-token:}") String scrapeToken) {
        this.scrapeToken = scrapeToken;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        if (!METRICS_PATH.equals(request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader("Authorization");

        if (scrapeToken == null || scrapeToken.isBlank()
                || authorization == null
                || !authorization.startsWith("Bearer ")) {
            rejectRequest(response);
            return;
        }

        String suppliedToken = authorization.substring(7).trim();

        boolean validToken = MessageDigest.isEqual(
                scrapeToken.getBytes(StandardCharsets.UTF_8),
                suppliedToken.getBytes(StandardCharsets.UTF_8));

        if (!validToken) {
            rejectRequest(response);
            return;
        }

        var authentication = new UsernamePasswordAuthenticationToken(
                "grafana-metrics",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_METRICS_SCRAPER")));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private void rejectRequest(HttpServletResponse response)
            throws IOException {
        SecurityContextHolder.clearContext();
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
