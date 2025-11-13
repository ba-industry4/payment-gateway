package com.ba.payment.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateApplicationRequest {

    @NotBlank(message = "Application name is required")
    private String name;

    private String description;

    @NotBlank(message = "Application type is required")
    private String appType; // WEB, MOBILE, API, E_COMMERCE, SAAS

    private String webhookUrl;
    private String callbackUrl;
    private String allowedOrigins;
    private String settings;
}

