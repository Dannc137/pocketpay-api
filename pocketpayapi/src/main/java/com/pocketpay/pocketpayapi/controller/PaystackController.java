package com.pocketpay.pocketpayapi.controller;

import com.pocketpay.pocketpayapi.dto.response.ApiResponse;
import com.pocketpay.pocketpayapi.dto.response.TransactionResponse;
import com.pocketpay.pocketpayapi.service.PaystackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/paystack")
@RequiredArgsConstructor
public class PaystackController {

    private final PaystackService paystackService;

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initiatePayment(
            @RequestParam BigDecimal amount) {

        Map<String, Object> response =
                paystackService.initiatePayment(amount);
        return ResponseEntity.ok(
                ApiResponse.ok("Payment initiated", response));
    }

    @GetMapping("/verify/{reference}")
    public ResponseEntity<ApiResponse<TransactionResponse>> verifyPayment(
            @PathVariable String reference) {

        TransactionResponse transaction =
                paystackService.verifyPayment(reference);
        return ResponseEntity.ok(
                ApiResponse.ok("Payment verified successfully",
                        transaction));
    }
}