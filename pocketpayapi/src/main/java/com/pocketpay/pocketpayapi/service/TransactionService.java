package com.pocketpay.pocketpayapi.service;

import com.pocketpay.pocketpayapi.dto.response.TransactionResponse;
import com.pocketpay.pocketpayapi.entity.Transaction;
import com.pocketpay.pocketpayapi.entity.User;
import com.pocketpay.pocketpayapi.entity.Wallet;
import com.pocketpay.pocketpayapi.enums.TransactionType;
import com.pocketpay.pocketpayapi.repository.TransactionRepository;
import com.pocketpay.pocketpayapi.repository.UserRepository;
import com.pocketpay.pocketpayapi.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    // Get all transactions for the logged in user — paginated
    @Transactional
    public Page<TransactionResponse> getMyTransactions(
            int page, int size, TransactionType type) {

        User user = getCurrentUser();
        Wallet wallet = getWalletByUser(user);

        Pageable pageable = PageRequest.of(
                page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Transaction> transactions;

        if (type != null) {
            transactions = transactionRepository
                    .findBySenderWallet_IdOrReceiverWallet_IdAndType(
                            wallet.getId(),
                            wallet.getId(),
                            type,
                            pageable);
        } else {
            transactions = transactionRepository
                    .findBySenderWallet_IdOrReceiverWallet_Id(
                            wallet.getId(),
                            wallet.getId(),
                            pageable);
        }

        return transactions.map(this::mapToTransactionResponse);
    }

    // Get a single transaction by reference
    @Transactional
    public TransactionResponse getByReference(String reference) {
        User user = getCurrentUser();
        Wallet wallet = getWalletByUser(user);

        Transaction transaction = transactionRepository
                .findByReference(reference)
                .orElseThrow(() ->
                        new RuntimeException("Transaction not found"));

        // Make sure the transaction belongs to this user
        boolean isSender = transaction.getSenderWallet() != null &&
                transaction.getSenderWallet().getId()
                        .equals(wallet.getId());
        boolean isReceiver = transaction.getReceiverWallet() != null &&
                transaction.getReceiverWallet().getId()
                        .equals(wallet.getId());

        if (!isSender && !isReceiver) {
            throw new RuntimeException(
                    "You don't have access to this transaction");
        }

        return mapToTransactionResponse(transaction);
    }

    // ── Helpers ──────────────────────────────────────────

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
            Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .reference(transaction.getReference())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .senderWalletNumber(
                        transaction.getSenderWallet() != null
                                ? transaction.getSenderWallet()
                                        .getWalletNumber()
                                : null)
                .receiverWalletNumber(
                        transaction.getReceiverWallet() != null
                                ? transaction.getReceiverWallet()
                                        .getWalletNumber()
                                : null)
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}