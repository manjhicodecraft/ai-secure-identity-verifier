package com.codex.identity_verifier.controller;

import com.codex.identity_verifier.dto.LoginRequest;
import com.codex.identity_verifier.dto.LoginResponse;
import com.codex.identity_verifier.service.AuthService;
import com.codex.identity_verifier.util.InputValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        try {
            // Validate input
            if (loginRequest == null || loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
                return ResponseEntity.badRequest()
                    .body(LoginResponse.builder()
                        .token(null)
                        .username(null)
                        .role(null)
                        .expiresIn(null)
                        .build());
            }

            // Validate username and password
            InputValidator.ValidationResult usernameValidation = InputValidator.validateUsername(loginRequest.getUsername());
            if (!usernameValidation.isValid()) {
                return ResponseEntity.badRequest()
                    .body(LoginResponse.builder()
                        .token(null)
                        .username(null)
                        .role(null)
                        .expiresIn(null)
                        .build());
            }

            // Sanitize inputs
            String sanitizedUsername = InputValidator.sanitizeInput(loginRequest.getUsername());
            String sanitizedPassword = InputValidator.sanitizeInput(loginRequest.getPassword());
            
            // Create sanitized login request
            LoginRequest sanitizedRequest = LoginRequest.builder()
                .username(sanitizedUsername)
                .password(sanitizedPassword)
                .build();

            LoginResponse response = authService.authenticateUser(sanitizedRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(LoginResponse.builder()
                    .token(null)
                    .username(null)
                    .role(null)
                    .expiresIn(null)
                    .build());
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader("Authorization") String token) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            boolean isValid = authService.validateToken(token);
            String username = isValid ? authService.getUsernameFromToken(token) : null;
            String role = isValid ? authService.getRoleFromToken(token) : null;
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", isValid);
            response.put("username", username);
            response.put("role", role);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("username", null);
            response.put("role", null);
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Authentication Service");
        return ResponseEntity.ok(response);
    }
}
