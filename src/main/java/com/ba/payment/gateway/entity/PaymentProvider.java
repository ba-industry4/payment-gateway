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

@Entity
@Table(name = "payment_providers", indexes = {
    @Index(name = "idx_payment_providers_name", columnList = "name"),
    @Index(name = "idx_payment_providers_type", columnList = "type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(columnDefinition = "jsonb")
    private String configuration;

    @Column(name = "supported_currencies", columnDefinition = "TEXT")
    private String supportedCurrencies;

    @Column(name = "supported_payment_methods", columnDefinition = "TEXT")
    private String supportedPaymentMethods;

    @Column(name = "fee_percentage", precision = 5, scale = 2)
    private BigDecimal feePercentage;

    @Column(name = "fee_fixed", precision = 19, scale = 4)
    private BigDecimal feeFixed;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
