package com.pocketpay.pocketpayapi.dto.response;

import com.pocketpay.pocketpayapi.enums.TransactionStatus;
import com.pocketpay.pocketpayapi.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private String reference;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private String senderWalletNumber;
    private String receiverWalletNumber;
    private LocalDateTime createdAt;
}