package com.job_platfrom.demo.controller;

import com.job_platfrom.demo.dto.AuthResponse;
import com.job_platfrom.demo.dto.LoginRequest;
import com.job_platfrom.demo.dto.RegisterRequest;
import com.job_platfrom.demo.dto.VerifyCodeRequest;
import com.job_platfrom.demo.entity.User;
import com.job_platfrom.demo.service.AuthService;
import com.job_platfrom.demo.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        try {
            User user = User.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .password(request.getPassword())
                    .role(request.getRole())
                    .build();

            authService.register(user);
            return ResponseEntity.ok("Verification code sent to email. Verify before login.");
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verify(@RequestBody VerifyCodeRequest request) {
        try {
            authService.verifyUser(request.getEmail(), request.getCode());
            return ResponseEntity.ok("User verified successfully");
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            User user = authService.login(request.getEmail(), request.getPassword());
            String token = jwtUtil.generateToken(user);
            return ResponseEntity.ok(new AuthResponse("Login successful", user.getEmail(), token));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
