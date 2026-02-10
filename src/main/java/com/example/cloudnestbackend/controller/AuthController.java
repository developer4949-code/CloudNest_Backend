package com.example.cloudnestbackend.controller;

import com.example.cloudnestbackend.dto.UserProfileResponse;
import com.example.cloudnestbackend.dto.SignupRequest;
import com.example.cloudnestbackend.security.JwtUtil;
import com.example.cloudnestbackend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final AuthService authService;

    public AuthController(JwtUtil jwtUtil, AuthService authService) {
        this.jwtUtil = jwtUtil;
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req) {

        String email = req.get("email");
        String password = req.get("password");

        var user = authService.authenticate(email, password);

        String token = jwtUtil.generateToken(user.getEmail());

        return ResponseEntity.ok(
                Map.of(
                        "token", token,
                        "user", Map.of(
                                "email", user.getEmail(),
                                "role", user.getRole().name())));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok("User registered successfully");
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(@RequestParam String email) {
        var user = authService.getUserByEmail(email);
        return ResponseEntity.ok(new UserProfileResponse(user.getFullName(), user.getEmail()));
    }
}
