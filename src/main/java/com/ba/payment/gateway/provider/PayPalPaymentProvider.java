package com.ba.payment.gateway.provider;

import com.ba.payment.gateway.exception.PaymentException;
import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PayPalPaymentProvider implements PaymentProviderStrategy {

    private static final List<String> SUPPORTED_CURRENCIES = Arrays.asList(
            "USD", "EUR", "GBP", "CAD", "AUD", "JPY"
    );

    private final APIContext apiContext;

    public PayPalPaymentProvider(
            @Value("${payment.paypal.client-id}") String clientId,
            @Value("${payment.paypal.client-secret}") String clientSecret,
            @Value("${payment.paypal.mode}") String mode
    ) {
        this.apiContext = new APIContext(clientId, clientSecret, mode);
    }

    @Override
    public String processPayment(BigDecimal amount, String currency, String paymentMethodToken, Map<String, String> metadata) {
        try {
            Amount paymentAmount = new Amount();
            paymentAmount.setCurrency(currency);
            paymentAmount.setTotal(amount.toString());

            Transaction transaction = new Transaction();
            transaction.setAmount(paymentAmount);
            transaction.setDescription(metadata.getOrDefault("description", "Payment"));

            List<Transaction> transactions = new ArrayList<>();
            transactions.add(transaction);

            Payer payer = new Payer();
            payer.setPaymentMethod("paypal");

            Payment payment = new Payment();
            payment.setIntent("sale");
            payment.setPayer(payer);
            payment.setTransactions(transactions);

            RedirectUrls redirectUrls = new RedirectUrls();
            redirectUrls.setCancelUrl(metadata.getOrDefault("cancelUrl", "https://example.com/cancel"));
            redirectUrls.setReturnUrl(metadata.getOrDefault("returnUrl", "https://example.com/success"));
            payment.setRedirectUrls(redirectUrls);

            Payment createdPayment = payment.create(apiContext);
            log.info("PayPal payment created: {}", createdPayment.getId());
            return createdPayment.getId();

        } catch (PayPalRESTException e) {
            log.error("PayPal payment failed: {}", e.getMessage());
            throw new PaymentException("Failed to process PayPal payment: " + e.getMessage(), e);
        }
    }

    @Override
    public String processRefund(String transactionId, BigDecimal amount, String reason) {
        try {
            Amount refundAmount = new Amount();
            refundAmount.setCurrency("USD");
            refundAmount.setTotal(amount.toString());

            RefundRequest refundRequest = new RefundRequest();
            refundRequest.setAmount(refundAmount);

            Sale sale = new Sale();
            sale.setId(transactionId);

            DetailedRefund refund = sale.refund(apiContext, refundRequest);
            log.info("PayPal refund created: {}", refund.getId());
            return refund.getId();

        } catch (PayPalRESTException e) {
            log.error("PayPal refund failed: {}", e.getMessage());
            throw new PaymentException("Failed to process PayPal refund: " + e.getMessage(), e);
        }
    }

    @Override
    public String getTransactionStatus(String transactionId) {
        try {
            Payment payment = Payment.get(apiContext, transactionId);
            return payment.getState();
        } catch (PayPalRESTException e) {
            log.error("Failed to retrieve PayPal transaction status: {}", e.getMessage());
            throw new PaymentException("Failed to retrieve transaction status: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "PAYPAL";
    }

    @Override
    public boolean supportsCurrency(String currency) {
        return SUPPORTED_CURRENCIES.contains(currency.toUpperCase());
    }
}
