package com.ba.payment.gateway.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a client business/application using the payment gateway service.
 * Each Application is a different customer/client with their own payment provider configuration.
 *
 * Examples:
 * - Application A = "ABC E-commerce Store" → Uses Stripe
 * - Application B = "XYZ Retail Shop" → Uses PayPal
 * - Application C = "123 SaaS Platform" → Uses Square
 *
 * Different clients (applications) can have completely different payment provider configurations.
 */
@Entity
@Table(name = "applications", indexes = {
    @Index(name = "idx_applications_merchant_id", columnList = "merchant_id"),
    @Index(name = "idx_applications_app_key", columnList = "app_key"),
    @Index(name = "idx_applications_active", columnList = "active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_applications_merchant_id"))
    private Merchant merchant;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "app_type", nullable = false, length = 50)
    private String appType; // e.g., WEB, MOBILE, API, E_COMMERCE, SAAS

    @Column(name = "app_key", nullable = false, unique = true, length = 100)
    private String appKey; // Unique identifier for the application

    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    @Column(name = "callback_url", length = 500)
    private String callbackUrl;

    @Column(name = "allowed_origins", columnDefinition = "TEXT")
    private String allowedOrigins; // Comma-separated list of allowed origins for CORS

    @Column(nullable = false)
    private Boolean active = true;

    @Column(columnDefinition = "jsonb")
    private String settings; // JSON configuration for app-specific settings

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

