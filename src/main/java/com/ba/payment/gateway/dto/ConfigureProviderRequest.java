package com.ba.payment.gateway.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigureProviderRequest {

    @NotNull(message = "Payment provider ID is required")
    private Long paymentProviderId;

    private Integer priority = 0;

    private Boolean isDefault = false;

    private String providerConfig; // JSON string with API keys, secrets, etc.

    private String supportedCurrencies; // Comma-separated list: "USD,EUR,GBP"

    private BigDecimal minAmount;
    private BigDecimal maxAmount;
}

