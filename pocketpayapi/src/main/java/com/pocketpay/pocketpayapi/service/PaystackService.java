package com.pocketpay.pocketpayapi.service;

import com.pocketpay.pocketpayapi.dto.response.TransactionResponse;
import com.pocketpay.pocketpayapi.entity.Transaction;
import com.pocketpay.pocketpayapi.entity.User;
import com.pocketpay.pocketpayapi.entity.Wallet;
import com.pocketpay.pocketpayapi.enums.TransactionStatus;
import com.pocketpay.pocketpayapi.enums.TransactionType;
import com.pocketpay.pocketpayapi.paystack.InitiatePaymentResponse;
import com.pocketpay.pocketpayapi.paystack.VerifyPaymentResponse;
import com.pocketpay.pocketpayapi.repository.TransactionRepository;
import com.pocketpay.pocketpayapi.repository.UserRepository;
import com.pocketpay.pocketpayapi.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaystackService {

    @Value("${paystack.secret.key}")
    private String paystackSecretKey;

    @Value("${paystack.base.url}")
    private String paystackBaseUrl;

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    // Step 1 — initiate a payment, get a payment link back
    @Transactional
    public Map<String, Object> initiatePayment(BigDecimal amount) {
        User user = getCurrentUser();
        Wallet wallet = getWalletByUser(user);

        long amountInKobo = amount
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        String reference = generateReference();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("email", user.getEmail());
        requestBody.put("amount", amountInKobo);
        requestBody.put("currency", "NGN");
        requestBody.put("reference", reference);

        ResponseEntity<InitiatePaymentResponse> response =
                makePaystackRequest(
                        "/transaction/initialize",
                        HttpMethod.POST,
                        requestBody,
                        InitiatePaymentResponse.class);

        InitiatePaymentResponse responseBody = response.getBody();

        if (responseBody == null || !responseBody.isStatus()) {
            throw new RuntimeException("Failed to initiate payment");
        }

        InitiatePaymentResponse.PaymentData data = responseBody.getData();

        // Save a PENDING transaction tied to this user's wallet
        Transaction pendingTransaction = Transaction.builder()
                .reference(reference)
                .amount(amount)
                .type(TransactionType.CREDIT)
                .status(TransactionStatus.PENDING)
                .description("Wallet funding via Paystack — pending")
                .receiverWallet(wallet)
                .build();

        transactionRepository.save(pendingTransaction);

        return Map.of(
                "authorizationUrl", data.getAuthorizationUrl(),
                "reference", data.getReference(),
                "amount", amount
        );
    }

    // Step 2 — verify payment and credit wallet if successful
    @Transactional
    public TransactionResponse verifyPayment(String reference) {
        User user = getCurrentUser();
        Wallet wallet = getWalletByUser(user);

        // Find the pending transaction we created at initiate time
        Transaction transaction = transactionRepository
                .findByReference(reference)
                .orElseThrow(() ->
                        new RuntimeException("Transaction not found"));

        // Make sure THIS user owns this transaction
        if (!transaction.getReceiverWallet().getId()
                .equals(wallet.getId())) {
            throw new RuntimeException(
                    "You don't have access to this transaction");
        }

        // Check it hasn't already been processed
        if (transaction.getStatus() == TransactionStatus.SUCCESS) {
            throw new RuntimeException("Transaction already verified");
        }

        // Verify with Paystack
        ResponseEntity<VerifyPaymentResponse> response =
                makePaystackRequest(
                        "/transaction/verify/" + reference,
                        HttpMethod.GET,
                        null,
                        VerifyPaymentResponse.class);

        VerifyPaymentResponse responseBody = response.getBody();

        if (responseBody == null || !responseBody.isStatus()) {
            throw new RuntimeException("Payment verification failed");
        }

        VerifyPaymentResponse.VerifyData data = responseBody.getData();

        if (!"success".equals(data.getStatus())) {
            // Mark as failed
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new RuntimeException(
                    "Payment was not successful: " + data.getStatus());
        }

        BigDecimal amount = BigDecimal.valueOf(data.getAmount())
                .divide(BigDecimal.valueOf(100));

        // Credit the wallet
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        // Update the existing transaction from PENDING → SUCCESS
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription("Wallet funding via Paystack");
        transactionRepository.save(transaction);

        return mapToTransactionResponse(transaction, wallet);
    }

    // ── Helpers ──────────────────────────────────────────

    // Generic helper for any Paystack API call
    private <T> ResponseEntity<T> makePaystackRequest(
            String endpoint,
            HttpMethod method,
            Map<String, Object> body,
            Class<T> responseType) {

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(paystackSecretKey);

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, headers);

        return restTemplate.exchange(
                paystackBaseUrl + endpoint,
                method,
                entity,
                responseType);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
    }

    private Wallet getWalletByUser(User user) {
        return walletRepository.findByUser_Id(user.getId())
                .orElseThrow(() ->
                        new RuntimeException("Wallet not found"));
    }

    private TransactionResponse mapToTransactionResponse(
            Transaction transaction, Wallet wallet) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .reference(transaction.getReference())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .receiverWalletNumber(wallet.getWalletNumber())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    private String generateReference() {
        return "PKT-PS-" + Instant.now().toEpochMilli()
                + "-" + UUID.randomUUID()
                .toString()
                .substring(0, 4)
                .toUpperCase();
    }
}