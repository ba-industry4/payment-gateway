package com.ba.payment.gateway.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Maps client applications to their configured payment providers.
 * Each client business (Application) can have their own provider configuration.
 *
 * Examples:
 * - Client A's Application → Stripe with their API keys
 * - Client B's Application → PayPal with their credentials
 * - Client C's Application → Multiple providers (Stripe + PayPal) for redundancy
 *
 * This enables multi-tenancy where each client uses different payment providers.
 */
@Entity
@Table(name = "application_providers",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_application_provider",
        columnNames = {"application_id", "payment_provider_id"}
    ),
    indexes = {
        @Index(name = "idx_app_providers_application_id", columnList = "application_id"),
        @Index(name = "idx_app_providers_provider_id", columnList = "payment_provider_id"),
        @Index(name = "idx_app_providers_default", columnList = "is_default,active")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, foreignKey = @ForeignKey(name = "fk_app_providers_application_id"))
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_provider_id", nullable = false, foreignKey = @ForeignKey(name = "fk_app_providers_provider_id"))
    private PaymentProvider paymentProvider;

    @Column(nullable = false)
    private Integer priority = 0; // Higher priority providers are tried first

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false; // Only one provider should be default per application

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "provider_config", columnDefinition = "TEXT")
    @Convert(converter = com.ba.payment.gateway.security.EncryptedStringConverter.class)
    private String providerConfig; // Encrypted! Application-specific provider configuration (API keys, secrets, etc.)

    @Column(name = "supported_currencies", columnDefinition = "TEXT")
    private String supportedCurrencies; // Comma-separated list of currencies enabled for this app-provider

    @Column(name = "min_amount", precision = 19, scale = 4)
    private BigDecimal minAmount;

    @Column(name = "max_amount", precision = 19, scale = 4)
    private BigDecimal maxAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

