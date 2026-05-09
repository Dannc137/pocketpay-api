package com.pocketpay.pocketpayapi.service;

import com.pocketpay.pocketpayapi.dto.request.LoginRequest;
import com.pocketpay.pocketpayapi.dto.request.RegisterRequest;
import com.pocketpay.pocketpayapi.dto.response.AuthResponse;
import com.pocketpay.pocketpayapi.dto.response.UserResponse;
import com.pocketpay.pocketpayapi.entity.User;
import com.pocketpay.pocketpayapi.entity.Wallet;
import com.pocketpay.pocketpayapi.repository.UserRepository;
import com.pocketpay.pocketpayapi.repository.WalletRepository;
import com.pocketpay.pocketpayapi.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

        private final UserRepository userRepository;
        private final WalletRepository walletRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtUtil jwtUtil;
        private final AuthenticationManager authenticationManager;

        @Transactional
        public AuthResponse register(RegisterRequest request) {

                // 1 — check if email already exists
                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new RuntimeException("Email already registered");
                }

                // 2 — create and save the user
                User user = User.builder()
                                .fullName(request.getFullName())
                                .email(request.getEmail())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .phoneNumber(request.getPhoneNumber())
                                .role(User.Role.USER)
                                .build();

                User savedUser = userRepository.save(user);

                // 3 — automatically create a wallet for the new user
                Wallet wallet = Wallet.builder()
                                .walletNumber(generateWalletNumber())
                                .user(savedUser)
                                .build();

                walletRepository.save(wallet);

                // 4 — generate JWT token
                String token = jwtUtil.generateToken(savedUser.getEmail());

                // 5 — build and return the response
                return AuthResponse.builder()
                                .token(token)
                                .tokenType("Bearer")
                                .user(mapToUserResponse(savedUser))
                                .build();
        }

        public AuthResponse login(LoginRequest request) {

                // 1 — authenticate email and password
                // throws BadCredentialsException automatically if wrong
                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                request.getEmail(),
                                                request.getPassword()));

                // 2 — load the user
                User user = userRepository.findByEmail(request.getEmail())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // 3 — generate token and return response
                String token = jwtUtil.generateToken(user.getEmail());

                return AuthResponse.builder()
                                .token(token)
                                .tokenType("Bearer")
                                .user(mapToUserResponse(user))
                                .build();
        }

        // Converts User entity to UserResponse DTO
        private UserResponse mapToUserResponse(User user) {
                return UserResponse.builder()
                                .id(user.getId())
                                .fullName(user.getFullName())
                                .email(user.getEmail())
                                .phoneNumber(user.getPhoneNumber())
                                .createdAt(user.getCreatedAt())
                                .build();
        }

        // Generates a unique wallet number like PKT-A1B2C3D4
        private String generateWalletNumber() {
                return "PKT-" + UUID.randomUUID()
                                .toString()
                                .replace("-", "")
                                .substring(0, 8)
                                .toUpperCase();
        }
}