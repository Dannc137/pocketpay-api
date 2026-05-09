package com.pocketpay.pocketpayapi.controller;

import com.pocketpay.pocketpayapi.dto.request.FundWalletRequest;
import com.pocketpay.pocketpayapi.dto.request.TransferRequest;
import com.pocketpay.pocketpayapi.dto.response.ApiResponse;
import com.pocketpay.pocketpayapi.dto.response.WalletResponse;
import com.pocketpay.pocketpayapi.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet() {
        WalletResponse wallet = walletService.getMyWallet();
        return ResponseEntity.ok(
                ApiResponse.ok("Wallet fetched", wallet));
    }

    @PostMapping("/fund")
    public ResponseEntity<ApiResponse<WalletResponse>> fundWallet(
            @Valid @RequestBody FundWalletRequest request) {
        WalletResponse wallet = walletService.fundWallet(request);
        return ResponseEntity.ok(
                ApiResponse.ok("Wallet funded successfully", wallet));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<WalletResponse>> transfer(
            @Valid @RequestBody TransferRequest request) {
        WalletResponse wallet = walletService.transfer(request);
        return ResponseEntity.ok(
                ApiResponse.ok("Transfer successful", wallet));
    }
}