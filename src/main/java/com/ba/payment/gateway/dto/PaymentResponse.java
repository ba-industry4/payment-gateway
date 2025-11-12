package com.ba.payment.gateway.dto;

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
public class PaymentResponse {
    private String transactionId;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String provider;
    private String providerTransactionId;
    private String description;
    private String redirectUrl;
    private String failureReason;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
}
