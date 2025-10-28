package com.ba.payment.gateway.provider;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory pattern implementation for managing payment providers (SOLID - Dependency Inversion Principle)
 * Clients depend on abstractions (PaymentProviderStrategy) rather than concrete implementations
 */
@Service
@RequiredArgsConstructor
public class PaymentProviderFactory {

    private final List<PaymentProviderStrategy> paymentProviders;

    /**
     * Get a payment provider by name
     * @param providerName The name of the provider (e.g., "STRIPE", "PAYPAL")
     * @return The payment provider strategy
     * @throws IllegalArgumentException if provider not found
     */
    public PaymentProviderStrategy getProvider(String providerName) {
        return paymentProviders.stream()
                .filter(provider -> provider.getProviderName().equalsIgnoreCase(providerName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Payment provider not found: " + providerName));
    }

    /**
     * Get all available payment providers
     * @return Map of provider names to strategies
     */
    public Map<String, PaymentProviderStrategy> getAllProviders() {
        Map<String, PaymentProviderStrategy> providers = new HashMap<>();
        paymentProviders.forEach(provider -> 
            providers.put(provider.getProviderName(), provider)
        );
        return providers;
    }
}
