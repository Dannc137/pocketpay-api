package com.pocketpay.pocketpayapi.service;

import com.pocketpay.pocketpayapi.dto.request.TransferRequest;
import com.pocketpay.pocketpayapi.entity.User;
import com.pocketpay.pocketpayapi.entity.Wallet;
import com.pocketpay.pocketpayapi.repository.TransactionRepository;
import com.pocketpay.pocketpayapi.repository.UserRepository;
import com.pocketpay.pocketpayapi.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private WalletService walletService;

    private User testUser;
    private Wallet senderWallet;
    private Wallet receiverWallet;

    @BeforeEach
    void setUp() {
        // Create a fake logged-in user in the security context
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("daniel@gmail.com");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);

        SecurityContextHolder.setContext(context);

        // Build test data
        testUser = User.builder()
                .id(1L)
                .email("daniel@gmail.com")
                .fullName("Daniel")
                .build();

        senderWallet = Wallet.builder()
                .id(1L)
                .walletNumber("PKT-SENDER01")
                .balance(new BigDecimal("10000"))
                .user(testUser)
                .active(true)
                .build();

        receiverWallet = Wallet.builder()
                .id(2L)
                .walletNumber("PKT-RECEIVER")
                .balance(new BigDecimal("5000"))
                .active(true)
                .build();
    }

    @Test
    void transfer_fails_when_balance_is_insufficient() {
        // ARRANGE — set up the scenario
        TransferRequest request = new TransferRequest();
        request.setReceiverWalletNumber("PKT-RECEIVER");
        request.setAmount(new BigDecimal("99999")); // way more than 10k balance
        request.setDescription("Test transfer");

        when(userRepository.findByEmail("daniel@gmail.com"))
                .thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser_Id(1L))
                .thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByWalletNumber("PKT-RECEIVER"))
                .thenReturn(Optional.of(receiverWallet));

        // ACT + ASSERT — we expect a RuntimeException
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> walletService.transfer(request));

        // Verify the error message is what we expect
        assert exception.getMessage().equals("Insufficient balance");

        // Verify NOTHING was saved (no debit, no credit, no transaction)
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_fails_when_sending_to_self() {
        // ARRANGE
        // The trick: receiverWalletNumber matches the sender's wallet number
        TransferRequest request = new TransferRequest();
        request.setReceiverWalletNumber("PKT-SENDER01"); // same as sender's wallet!
        request.setAmount(new BigDecimal("1000"));
        request.setDescription("Trying to send to myself");

        when(userRepository.findByEmail("daniel@gmail.com"))
                .thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser_Id(1L))
                .thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByWalletNumber("PKT-SENDER01"))
                .thenReturn(Optional.of(senderWallet)); // returns the SAME wallet

        // ACT + ASSERT
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> walletService.transfer(request));

        assert exception.getMessage().equals("You cannot transfer to your own wallet");

        // Verify NOTHING was saved
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_succeeds_with_valid_input() {
        // ARRANGE
        TransferRequest request = new TransferRequest();
        request.setReceiverWalletNumber("PKT-RECEIVER");
        request.setAmount(new BigDecimal("3000"));
        request.setDescription("Valid transfer");

        when(userRepository.findByEmail("daniel@gmail.com"))
                .thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser_Id(1L))
                .thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByWalletNumber("PKT-RECEIVER"))
                .thenReturn(Optional.of(receiverWallet));

        // ACT
        walletService.transfer(request);

        // ASSERT — verify the right database operations happened

        // Sender's balance should now be 7000 (10000 - 3000)
        assert senderWallet.getBalance().equals(new BigDecimal("7000"));

        // Receiver's balance should now be 8000 (5000 + 3000)
        assert receiverWallet.getBalance().equals(new BigDecimal("8000"));

        // Both wallets should have been saved
        verify(walletRepository, times(1)).save(senderWallet);
        verify(walletRepository, times(1)).save(receiverWallet);

        // A transaction record should have been saved
        verify(transactionRepository, times(1)).save(any());
    }

    @Test
    void transfer_fails_when_receiver_not_found(){

        TransferRequest request = new TransferRequest();
        request.setReceiverWalletNumber("PKT-NONEXISTENT");
        request.setAmount(new BigDecimal("1000"));
        request.setDescription("Trying to send to a non-existent wallet");

        when(userRepository.findByEmail("daniel@gmail.com"))
                .thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser_Id(1L))
                .thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByWalletNumber("PKT-NONEXISTENT"))
                .thenReturn(Optional.empty()); 


        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> walletService.transfer(request));

        assertEquals("Receiver wallet not found", exception.getMessage());

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}