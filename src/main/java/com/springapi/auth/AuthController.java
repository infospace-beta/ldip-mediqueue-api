
package com.springapi.auth;

import com.springapi.auth.dto.AuthResponse;
import com.springapi.auth.dto.CurrentUserResponse;
import com.springapi.auth.dto.LoginRequest;
import com.springapi.auth.dto.RegisterRequest;
import com.springapi.auth.dto.SupabaseExchangeRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request) {

        authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Registration successful");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public CurrentUserResponse getCurrentUser(
            @AuthenticationPrincipal UserDetails currentUser) {

        return authService.getCurrentUser(currentUser.getUsername());
    }

    @PostMapping("/supabase/exchange")
    public ResponseEntity<AuthResponse> exchangeSupabaseToken(
            @Valid @RequestBody SupabaseExchangeRequest request) {

        AuthResponse response =
                authService.exchangeSupabaseToken(request.accessToken());

        return ResponseEntity.ok(response);
    }
}
