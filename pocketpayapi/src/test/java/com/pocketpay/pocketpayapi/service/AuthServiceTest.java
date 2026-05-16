package com.pocketpay.pocketpayapi.service;

import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.pocketpay.pocketpayapi.dto.request.LoginRequest;
import com.pocketpay.pocketpayapi.dto.request.RegisterRequest;
import com.pocketpay.pocketpayapi.dto.response.AuthResponse;
import com.pocketpay.pocketpayapi.entity.User;
import com.pocketpay.pocketpayapi.entity.Wallet;
import com.pocketpay.pocketpayapi.repository.UserRepository;
import com.pocketpay.pocketpayapi.repository.WalletRepository;
import com.pocketpay.pocketpayapi.security.JwtUtil;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    WalletRepository walletRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtUtil jwtUtil;

    @Mock
    AuthenticationManager authenticationManager;

    @InjectMocks
    AuthService authService;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = User.builder()
                .id(1L)
                .email("daniel@gmail.com")
                .fullName("Daniel")
                .password("hashed_password")
                .phoneNumber("08012345678")
                .role(User.Role.USER)
                .build();
    }

    @Test
    void register_succeeds_with_valid_input() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Daniel");
        request.setEmail("daniel@gmail.com");
        request.setPassword("password");
        request.setPhoneNumber("08012345678");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(savedUser.getEmail())).thenReturn("fake_jwt_token");

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals("fake_jwt_token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("daniel@gmail.com", response.getUser().getEmail());

        verify(walletRepository, times(1)).save(any(Wallet.class));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_fails_when_email_already_exists() {

        RegisterRequest request = new RegisterRequest();
        request.setFullName("Daniel");
        request.setEmail("daniel@gmail.com");
        request.setPassword("password");
        request.setPhoneNumber("08012345678");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.register(request));

        assertEquals("Email already registered", exception.getMessage());

        verify(userRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void login_fails_when_user_not_found() {

        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@gmail.com");
        request.setPassword("password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request));

        verify(jwtUtil, never()).generateToken(any());
    }
}