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
    private final ApplicationService applicationService;

    /**
     * Process payment with explicit provider selection
     */
    @Transactional
    public Transaction processPayment(PaymentRequest request, Merchant merchant) {
        // Get the appropriate payment provider using Factory pattern
        PaymentProviderStrategy provider = paymentProviderFactory.getProvider(request.getProvider());

        // Validate currency support
        if (!provider.supportsCurrency(request.getCurrency())) {
            throw new IllegalArgumentException("Currency not supported by provider: " + request.getCurrency());
        }

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
     * Process payment using application-specific provider configuration.
     * This method intelligently selects the payment provider based on the application's configuration.
     */
    @Transactional
    public Transaction processPaymentForApplication(PaymentRequest request, Merchant merchant,
                                                   com.ba.payment.gateway.entity.Application application) {
        // Determine which provider to use
        String providerName;
        com.ba.payment.gateway.entity.ApplicationProvider appProvider;

        if (request.getProvider() != null && !request.getProvider().isEmpty()) {
            // Use explicitly requested provider if configured for this application
            appProvider = applicationService.getProviderByName(application, request.getProvider());
            providerName = request.getProvider();
        } else {
            // Use default provider for the application
            appProvider = applicationService.getDefaultProvider(application);
            providerName = appProvider.getPaymentProvider().getName();
        }

        // Validate currency support for this app-provider configuration
        if (appProvider.getSupportedCurrencies() != null && !appProvider.getSupportedCurrencies().isEmpty()) {
            if (!appProvider.getSupportedCurrencies().contains(request.getCurrency())) {
                throw new IllegalArgumentException(
                    "Currency " + request.getCurrency() + " not supported by " + providerName +
                    " for application " + application.getName());
            }
        }

        // Validate amount limits
        if (appProvider.getMinAmount() != null && request.getAmount().compareTo(appProvider.getMinAmount()) < 0) {
            throw new IllegalArgumentException(
                "Amount below minimum for " + providerName + " (min: " + appProvider.getMinAmount() + ")");
        }
        if (appProvider.getMaxAmount() != null && request.getAmount().compareTo(appProvider.getMaxAmount()) > 0) {
            throw new IllegalArgumentException(
                "Amount above maximum for " + providerName + " (max: " + appProvider.getMaxAmount() + ")");
        }

        // Get the provider strategy
        PaymentProviderStrategy provider = paymentProviderFactory.getProvider(providerName);

        // Create transaction record with application context
        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .merchant(merchant)
                .application(application)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status("PENDING")
                .type("PAYMENT")
                .provider(providerName)
                .description(request.getDescription())
                .customerEmail(request.getCustomerEmail())
                .customerName(request.getCustomerName())
                .build();

        transaction = transactionRepository.save(transaction);

        try {
            // Process payment through provider
            // Note: In a real implementation, you might pass appProvider.getProviderConfig()
            // to use application-specific API keys/credentials
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

            log.info("Payment processed successfully for application {}: transaction {}",
                    application.getName(), transaction.getTransactionId());

        } catch (Exception e) {
            transaction.setStatus("FAILED");
            transaction.setFailureReason("Payment processing failed");
            log.error("Payment processing failed for application {} transaction {}: {}",
                    application.getName(), transaction.getTransactionId(), e.getClass().getSimpleName());
            log.debug("Payment processing error details", e);
        }

        return transactionRepository.save(transaction);
    }
}
