package com.ba.payment.gateway.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "merchants", indexes = {
    @Index(name = "idx_merchants_user_id", columnList = "user_id"),
    @Index(name = "idx_merchants_business_email", columnList = "business_email")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_merchants_user_id"))
    private User user;

    @Column(name = "business_name", nullable = false, length = 255)
    private String businessName;

    @Column(name = "business_email", nullable = false, length = 255)
    private String businessEmail;

    @Column(name = "business_phone", length = 50)
    private String businessPhone;

    @Column(name = "business_address", columnDefinition = "TEXT")
    private String businessAddress;

    @Column(name = "business_type", length = 100)
    private String businessType;

    @Column(name = "tax_id", length = 100)
    private String taxId;

    @Column(nullable = false, length = 50)
    private String status = "PENDING";

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "application_type", length = 100)
    private String applicationType;

    @Column(name = "default_redirect_url", length = 500)
    private String defaultRedirectUrl;

    @Column(name = "default_callback_url", length = 500)
    private String defaultCallbackUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
