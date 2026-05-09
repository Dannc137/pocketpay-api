package com.pocketpay.pocketpayapi.controller;

import com.pocketpay.pocketpayapi.dto.response.ApiResponse;
import com.pocketpay.pocketpayapi.dto.response.TransactionResponse;
import com.pocketpay.pocketpayapi.enums.TransactionType;
import com.pocketpay.pocketpayapi.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>>
            getMyTransactions(
                    @RequestParam(defaultValue = "0") int page,
                    @RequestParam(defaultValue = "10") int size,
                    @RequestParam(required = false) TransactionType type) {

        Page<TransactionResponse> transactions =
                transactionService.getMyTransactions(page, size, type);
        return ResponseEntity.ok(
                ApiResponse.ok("Transactions fetched", transactions));
    }

    @GetMapping("/{reference}")
    public ResponseEntity<ApiResponse<TransactionResponse>>
            getByReference(@PathVariable String reference) {

        TransactionResponse transaction =
                transactionService.getByReference(reference);
        return ResponseEntity.ok(
                ApiResponse.ok("Transaction fetched", transaction));
    }
}