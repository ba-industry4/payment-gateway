package com.ba.payment.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDTO {
    private Long id;
    private String name;
    private String description;
    private String appType;
    private String appKey;
    private String webhookUrl;
    private String callbackUrl;
    private String allowedOrigins;
    private Boolean active;
    private String settings;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ApplicationProviderDTO> providers;
}

