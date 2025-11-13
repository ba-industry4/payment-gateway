package com.ba.payment.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationProviderDTO {
    private Long id;
    private Long applicationId;
    private Long paymentProviderId;
    private String providerName;
    private String providerType;
    private Integer priority;
    private Boolean isDefault;
    private Boolean active;
    private String providerConfig;
    private String supportedCurrencies;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
}

