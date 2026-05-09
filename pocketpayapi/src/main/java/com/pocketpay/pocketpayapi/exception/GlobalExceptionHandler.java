package com.pocketpay.pocketpayapi.exception;

import com.pocketpay.pocketpayapi.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

        // Handles our RuntimeExceptions — insufficient balance, not found etc
        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<ApiResponse<Void>> handleRuntimeException(
                        RuntimeException ex) {
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(ex.getMessage()));
        }

        // Handles wrong email/password on login
        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ApiResponse<Void>> handleBadCredentials(
                        BadCredentialsException ex) {
                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponse.error("Invalid email or password"));
        }

        // Handles @Valid validation failures — @NotBlank, @Email etc
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
                        MethodArgumentNotValidException ex) {

                Map<String, String> errors = new HashMap<>();
                ex.getBindingResult()
                                .getAllErrors()
                                .forEach(error -> {
                                        String field = ((FieldError) error).getField();
                                        String message = error.getDefaultMessage();
                                        errors.put(field, message);
                                });

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.<Map<String, String>>builder()
                                                .success(false)
                                                .message("Validation failed")
                                                .data(errors)
                                                .build());
        }

        // Catches anything we didn't anticipate
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleGenericException(
                        Exception ex) {
                ex.printStackTrace(); // Log the stack trace for debugging
                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.error(
                                                "Something went wrong. Please try again."));
        }
}