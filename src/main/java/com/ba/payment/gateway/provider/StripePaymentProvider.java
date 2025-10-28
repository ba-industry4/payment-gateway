package com.ba.payment.gateway.provider;

import com.ba.payment.gateway.exception.PaymentException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class StripePaymentProvider implements PaymentProviderStrategy {

    private static final List<String> SUPPORTED_CURRENCIES = Arrays.asList(
            "USD", "EUR", "GBP", "CAD", "AUD", "JPY", "CHF", "SEK", "NOK", "DKK"
    );

    @Value("${payment.stripe.api-key}")
    private String apiKey;

    public StripePaymentProvider(@Value("${payment.stripe.api-key}") String apiKey) {
        this.apiKey = apiKey;
        Stripe.apiKey = apiKey;
    }

    @Override
    public String processPayment(BigDecimal amount, String currency, String paymentMethodToken, Map<String, String> metadata) {
        try {
            // Convert amount to cents/smallest currency unit
            long amountInCents = amount.multiply(new BigDecimal("100")).longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency.toLowerCase())
                    .setPaymentMethod(paymentMethodToken)
                    .setConfirm(true)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(false)
                                    .build()
                    )
                    .putAllMetadata(metadata)
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);
            log.info("Stripe payment created: {}", paymentIntent.getId());
            return paymentIntent.getId();

        } catch (StripeException e) {
            log.error("Stripe payment failed: {}", e.getMessage());
            throw new PaymentException("Failed to process Stripe payment: " + e.getMessage(), e);
        }
    }

    @Override
    public String processRefund(String transactionId, BigDecimal amount, String reason) {
        try {
            long amountInCents = amount.multiply(new BigDecimal("100")).longValue();

            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(transactionId)
                    .setAmount(amountInCents)
                    .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                    .build();

            Refund refund = Refund.create(params);
            log.info("Stripe refund created: {}", refund.getId());
            return refund.getId();

        } catch (StripeException e) {
            log.error("Stripe refund failed: {}", e.getMessage());
            throw new PaymentException("Failed to process Stripe refund: " + e.getMessage(), e);
        }
    }

    @Override
    public String getTransactionStatus(String transactionId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(transactionId);
            return paymentIntent.getStatus();
        } catch (StripeException e) {
            log.error("Failed to retrieve Stripe transaction status: {}", e.getMessage());
            throw new PaymentException("Failed to retrieve transaction status: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "STRIPE";
    }

    @Override
    public boolean supportsCurrency(String currency) {
        return SUPPORTED_CURRENCIES.contains(currency.toUpperCase());
    }
}
