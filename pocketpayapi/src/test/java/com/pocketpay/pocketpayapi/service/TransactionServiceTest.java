package com.pocketpay.pocketpayapi.service;

import com.pocketpay.pocketpayapi.dto.response.TransactionResponse;
import com.pocketpay.pocketpayapi.entity.Transaction;
import com.pocketpay.pocketpayapi.entity.User;
import com.pocketpay.pocketpayapi.entity.Wallet;
import com.pocketpay.pocketpayapi.enums.TransactionStatus;
import com.pocketpay.pocketpayapi.enums.TransactionType;
import com.pocketpay.pocketpayapi.repository.TransactionRepository;
import com.pocketpay.pocketpayapi.repository.UserRepository;
import com.pocketpay.pocketpayapi.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Wallet userWallet;
    private Wallet otherWallet;
    private Transaction userTransaction;
    private Transaction otherUsersTransaction;

    @BeforeEach
    void setUp() {
        // Fake the logged-in user in the security context
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("daniel@gmail.com");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);

        SecurityContextHolder.setContext(context);

        testUser = User.builder()
                .id(1L)
                .email("daniel@gmail.com")
                .fullName("Daniel")
                .build();

        userWallet = Wallet.builder()
                .id(1L)
                .walletNumber("PKT-USER0001")
                .balance(new BigDecimal("5000"))
                .user(testUser)
                .active(true)
                .build();

        otherWallet = Wallet.builder()
                .id(99L)
                .walletNumber("PKT-OTHER999")
                .balance(new BigDecimal("3000"))
                .active(true)
                .build();

        // A transaction that BELONGS to Daniel
        userTransaction = Transaction.builder()
                .id(1L)
                .reference("PKT-TXN-OWNED")
                .amount(new BigDecimal("1000"))
                .type(TransactionType.CREDIT)
                .status(TransactionStatus.SUCCESS)
                .description("Daniel's transaction")
                .receiverWallet(userWallet)
                .build();

        // A transaction that does NOT belong to Daniel
        otherUsersTransaction = Transaction.builder()
                .id(2L)
                .reference("PKT-TXN-STRANGER")
                .amount(new BigDecimal("500"))
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .description("Some stranger's transaction")
                .senderWallet(otherWallet)
                .receiverWallet(otherWallet)
                .build();
    }

    // ── getMyTransactions ──

    @Test
    void getMyTransactions_returns_paginated_results_for_current_user() {
        // ARRANGE
        when(userRepository.findByEmail("daniel@gmail.com"))
                .thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser_Id(1L))
                .thenReturn(Optional.of(userWallet));

        Page<Transaction> mockPage = new PageImpl<>(List.of(userTransaction));
        when(transactionRepository.findBySenderWallet_IdOrReceiverWallet_Id(
                eq(1L), eq(1L), any(Pageable.class)))
                .thenReturn(mockPage);

        // ACT
        Page<TransactionResponse> result =
                transactionService.getMyTransactions(0, 10, null);

        // ASSERT
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("PKT-TXN-OWNED", result.getContent().get(0).getReference());
        assertEquals(new BigDecimal("1000"), result.getContent().get(0).getAmount());
    }

    // ── getByReference ──

    @Test
    void getByReference_returns_transaction_when_user_owns_it() {
        // ARRANGE
        when(userRepository.findByEmail("daniel@gmail.com"))
                .thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser_Id(1L))
                .thenReturn(Optional.of(userWallet));
        when(transactionRepository.findByReference("PKT-TXN-OWNED"))
                .thenReturn(Optional.of(userTransaction));

        // ACT
        TransactionResponse result =
                transactionService.getByReference("PKT-TXN-OWNED");

        // ASSERT
        assertNotNull(result);
        assertEquals("PKT-TXN-OWNED", result.getReference());
        assertEquals(new BigDecimal("1000"), result.getAmount());
    }

    @Test
    void getByReference_fails_when_user_does_not_own_transaction() {
        // ARRANGE — Daniel tries to access a transaction that isn't his
        when(userRepository.findByEmail("daniel@gmail.com"))
                .thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser_Id(1L))
                .thenReturn(Optional.of(userWallet));
        when(transactionRepository.findByReference("PKT-TXN-STRANGER"))
                .thenReturn(Optional.of(otherUsersTransaction));

        // ACT + ASSERT
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> transactionService.getByReference("PKT-TXN-STRANGER"));

        assertEquals("You don't have access to this transaction",
                exception.getMessage());
    }

    @Test
    void getByReference_fails_when_transaction_not_found() {
        // ARRANGE
        when(userRepository.findByEmail("daniel@gmail.com"))
                .thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser_Id(1L))
                .thenReturn(Optional.of(userWallet));
        when(transactionRepository.findByReference("PKT-TXN-NOPE"))
                .thenReturn(Optional.empty());

        // ACT + ASSERT
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> transactionService.getByReference("PKT-TXN-NOPE"));

        assertEquals("Transaction not found", exception.getMessage());
    }
}