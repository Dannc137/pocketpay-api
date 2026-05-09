package com.pocketpay.pocketpayapi.controller;

import com.pocketpay.pocketpayapi.dto.request.LoginRequest;
import com.pocketpay.pocketpayapi.dto.request.RegisterRequest;
import com.pocketpay.pocketpayapi.dto.response.ApiResponse;
import com.pocketpay.pocketpayapi.dto.response.AuthResponse;
import com.pocketpay.pocketpayapi.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(
                ApiResponse.ok("Registration successful", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(
                ApiResponse.ok("Login successful", response));
    }
}