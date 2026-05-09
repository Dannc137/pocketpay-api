package com.pocketpay.pocketpayapi.service;

import com.pocketpay.pocketpayapi.dto.request.FundWalletRequest;
import com.pocketpay.pocketpayapi.dto.request.TransferRequest;
import com.pocketpay.pocketpayapi.dto.response.WalletResponse;
import com.pocketpay.pocketpayapi.entity.Transaction;
import com.pocketpay.pocketpayapi.entity.User;
import com.pocketpay.pocketpayapi.entity.Wallet;
import com.pocketpay.pocketpayapi.enums.TransactionStatus;
import com.pocketpay.pocketpayapi.enums.TransactionType;
import com.pocketpay.pocketpayapi.repository.TransactionRepository;
import com.pocketpay.pocketpayapi.repository.UserRepository;
import com.pocketpay.pocketpayapi.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    // Get the currently logged in user's wallet
    public WalletResponse getMyWallet() {
        User user = getCurrentUser();
        Wallet wallet = getWalletByUser(user);
        return mapToWalletResponse(wallet);
    }

    // Fund wallet — simulate a deposit
    @Transactional
    public WalletResponse fundWallet(FundWalletRequest request) {
        User user = getCurrentUser();
        Wallet wallet = getWalletByUser(user);

        // Add the amount to the wallet balance
        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        walletRepository.save(wallet);

        // Log the transaction
        Transaction transaction = Transaction.builder()
                .reference(generateReference())
                .amount(request.getAmount())
                .type(TransactionType.CREDIT)
                .status(TransactionStatus.SUCCESS)
                .description(request.getDescription() != null
                        ? request.getDescription()
                        : "Wallet funding")
                .receiverWallet(wallet)
                .build();

        transactionRepository.save(transaction);

        return mapToWalletResponse(wallet);
    }

    // Transfer funds between two wallets
    @Transactional
    public WalletResponse transfer(TransferRequest request) {
        User sender = getCurrentUser();
        Wallet senderWallet = getWalletByUser(sender);

        // 1 — find receiver wallet
        Wallet receiverWallet = walletRepository
                .findByWalletNumber(request.getReceiverWalletNumber())
                .orElseThrow(() ->
                        new RuntimeException("Receiver wallet not found"));

        // 2 — make sure sender isn't sending to themselves
        if (senderWallet.getWalletNumber()
                .equals(receiverWallet.getWalletNumber())) {
            throw new RuntimeException(
                    "You cannot transfer to your own wallet");
        }

        // 3 — check sufficient balance
        if (senderWallet.getBalance()
                .compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        // 4 — debit sender, credit receiver
        senderWallet.setBalance(
                senderWallet.getBalance().subtract(request.getAmount()));
        receiverWallet.setBalance(
                receiverWallet.getBalance().add(request.getAmount()));

        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        // 5 — log the transaction
        String reference = generateReference();
        Transaction transaction = Transaction.builder()
                .reference(reference)
                .amount(request.getAmount())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .description(request.getDescription() != null
                        ? request.getDescription()
                        : "Transfer to " +
                          receiverWallet.getWalletNumber())
                .senderWallet(senderWallet)
                .receiverWallet(receiverWallet)
                .build();

        transactionRepository.save(transaction);

        return mapToWalletResponse(senderWallet);
    }

    // ── Helpers ──────────────────────────────────────────

    // Gets the currently authenticated user from the security context
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

    private WalletResponse mapToWalletResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .walletNumber(wallet.getWalletNumber())
                .balance(wallet.getBalance())
                .active(wallet.isActive())
                .createdAt(wallet.getCreatedAt())
                .build();
    }

    private String generateReference() {
        return "PKT-TXN-" + Instant.now().toEpochMilli()
                + "-" + UUID.randomUUID()
                .toString()
                .substring(0, 4)
                .toUpperCase();
    }
}