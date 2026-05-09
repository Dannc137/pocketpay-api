package com.pocketpay.pocketpayapi.paystack;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class VerifyPaymentResponse {
    private boolean status;
    private String message;
    private VerifyData data;

    @Data
    public static class VerifyData {
        private Long id;
        private String status;
        private String reference;
        private Integer amount;
        private String currency;
        private String channel;

        @JsonProperty("paid_at")
        private String paidAt;

        @JsonProperty("created_at")
        private String createdAt;
    }
}