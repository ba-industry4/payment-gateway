package com.ba.payment.gateway.service;

import com.ba.payment.gateway.dto.PaymentRequest;
import com.ba.payment.gateway.entity.Merchant;
import com.ba.payment.gateway.entity.Transaction;
import com.ba.payment.gateway.provider.PaymentProviderFactory;
import com.ba.payment.gateway.provider.PaymentProviderStrategy;
import com.ba.payment.gateway.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment Service following Single Responsibility Principle (SOLID)
 * Handles payment processing operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final PaymentProviderFactory paymentProviderFactory;

    @Transactional
    public Transaction processPayment(PaymentRequest request, Merchant merchant) {
        // Get the appropriate payment provider using Factory pattern
        PaymentProviderStrategy provider = paymentProviderFactory.getProvider(request.getProvider());

        // Validate currency support
        if (!provider.supportsCurrency(request.getCurrency())) {
            throw new IllegalArgumentException("Currency not supported by provider: " + request.getCurrency());
        }

        // Determine redirect URLs - use request URLs or fallback to merchant defaults
        String redirectUrl = determineRedirectUrl(request.getRedirectUrl(), merchant.getDefaultRedirectUrl());
        String successUrl = determineRedirectUrl(request.getSuccessUrl(), merchant.getDefaultRedirectUrl());
        String failureUrl = determineRedirectUrl(request.getFailureUrl(), merchant.getDefaultRedirectUrl());

        // Create transaction record
        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .merchant(merchant)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status("PENDING")
                .type("PAYMENT")
                .provider(request.getProvider())
                .description(request.getDescription())
                .customerEmail(request.getCustomerEmail())
                .customerName(request.getCustomerName())
                .redirectUrl(redirectUrl)
                .successUrl(successUrl)
                .failureUrl(failureUrl)
                .build();

        transaction = transactionRepository.save(transaction);

        try {
            // Process payment through provider
            String providerTransactionId = provider.processPayment(
                    request.getAmount(),
                    request.getCurrency(),
                    request.getPaymentMethodToken(),
                    request.getMetadata()
            );

            // Update transaction with provider details
            transaction.setProviderTransactionId(providerTransactionId);
            transaction.setStatus("COMPLETED");
            transaction.setProcessedAt(LocalDateTime.now());

            log.info("Payment processed successfully: {}", transaction.getTransactionId());

        } catch (Exception e) {
            transaction.setStatus("FAILED");
            transaction.setFailureReason("Payment processing failed");
            log.error("Payment processing failed for transaction {}: {}", transaction.getTransactionId(), e.getClass().getSimpleName());
            // Log detailed error separately for debugging (should be logged to secure storage)
            log.debug("Payment processing error details", e);
        }

        return transactionRepository.save(transaction);
    }

    /**
     * Determines the redirect URL to use, preferring the request URL over the merchant default
     */
    private String determineRedirectUrl(String requestUrl, String merchantDefaultUrl) {
        if (requestUrl != null && !requestUrl.isEmpty()) {
            return requestUrl;
        }
        return merchantDefaultUrl;
    }
}
