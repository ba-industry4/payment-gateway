package com.ba.payment.gateway.provider;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Payment provider interface following the Strategy pattern (SOLID - Open/Closed Principle)
 * This allows easy addition of new payment providers without modifying existing code
 */
public interface PaymentProviderStrategy {
    
    /**
     * Process a payment transaction
     * @param amount The amount to charge
     * @param currency The currency code (e.g., USD, EUR)
     * @param paymentMethodToken The payment method token from the provider
     * @param metadata Additional metadata for the transaction
     * @return The provider transaction ID
     */
    String processPayment(BigDecimal amount, String currency, String paymentMethodToken, Map<String, String> metadata);
    
    /**
     * Process a refund
     * @param transactionId The original transaction ID
     * @param amount The amount to refund
     * @param reason The reason for the refund
     * @return The provider refund ID
     */
    String processRefund(String transactionId, BigDecimal amount, String reason);
    
    /**
     * Retrieve the status of a transaction
     * @param transactionId The transaction ID
     * @return The current status of the transaction
     */
    String getTransactionStatus(String transactionId);
    
    /**
     * Get the provider name
     * @return The name of the payment provider
     */
    String getProviderName();
    
    /**
     * Check if the provider supports a specific currency
     * @param currency The currency code
     * @return true if supported, false otherwise
     */
    boolean supportsCurrency(String currency);
}
