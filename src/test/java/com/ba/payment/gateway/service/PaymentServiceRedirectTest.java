package com.ba.payment.gateway.service;

import com.ba.payment.gateway.dto.PaymentRequest;
import com.ba.payment.gateway.entity.Merchant;
import com.ba.payment.gateway.entity.Transaction;
import com.ba.payment.gateway.entity.User;
import com.ba.payment.gateway.provider.PaymentProviderFactory;
import com.ba.payment.gateway.provider.PaymentProviderStrategy;
import com.ba.payment.gateway.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test class for PaymentService redirect URL functionality
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceRedirectTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PaymentProviderFactory paymentProviderFactory;

    @Mock
    private PaymentProviderStrategy paymentProvider;

    @InjectMocks
    private PaymentService paymentService;

    private Merchant merchant;
    private PaymentRequest paymentRequest;

    @BeforeEach
    void setUp() {
        // Setup merchant with default redirect URLs
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        merchant = Merchant.builder()
                .id(1L)
                .user(user)
                .businessName("Test Business")
                .businessEmail("business@example.com")
                .applicationType("WEB")
                .defaultRedirectUrl("https://example.com/default/redirect")
                .defaultCallbackUrl("https://example.com/default/callback")
                .build();

        // Setup payment request
        paymentRequest = PaymentRequest.builder()
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .provider("STRIPE")
                .paymentMethodToken("pm_test_token")
                .description("Test payment")
                .customerEmail("customer@example.com")
                .customerName("John Doe")
                .metadata(new HashMap<>())
                .build();

        // Mock provider
        when(paymentProviderFactory.getProvider(anyString())).thenReturn(paymentProvider);
        when(paymentProvider.supportsCurrency(anyString())).thenReturn(true);
        when(paymentProvider.processPayment(any(), anyString(), anyString(), any()))
                .thenReturn("provider_txn_123");
    }

    @Test
    void testProcessPayment_WithRequestRedirectUrl() {
        // Given: Payment request with specific redirect URL
        paymentRequest.setRedirectUrl("https://example.com/custom/redirect");
        paymentRequest.setSuccessUrl("https://example.com/custom/success");
        paymentRequest.setFailureUrl("https://example.com/custom/failure");

        Transaction savedTransaction = Transaction.builder()
                .id(1L)
                .transactionId("txn_123")
                .status("PENDING")
                .redirectUrl("https://example.com/custom/redirect")
                .successUrl("https://example.com/custom/success")
                .failureUrl("https://example.com/custom/failure")
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        // When: Processing payment
        Transaction result = paymentService.processPayment(paymentRequest, merchant);

        // Then: Transaction should have request redirect URLs
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, atLeastOnce()).save(transactionCaptor.capture());
        
        Transaction capturedTransaction = transactionCaptor.getValue();
        assertEquals("https://example.com/custom/redirect", capturedTransaction.getRedirectUrl());
        assertEquals("https://example.com/custom/success", capturedTransaction.getSuccessUrl());
        assertEquals("https://example.com/custom/failure", capturedTransaction.getFailureUrl());
    }

    @Test
    void testProcessPayment_WithoutRequestRedirectUrl_UseMerchantDefault() {
        // Given: Payment request without redirect URL
        // paymentRequest doesn't have redirect URLs set

        Transaction savedTransaction = Transaction.builder()
                .id(1L)
                .transactionId("txn_123")
                .status("PENDING")
                .redirectUrl("https://example.com/default/redirect")
                .successUrl("https://example.com/default/redirect")
                .failureUrl("https://example.com/default/redirect")
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        // When: Processing payment
        Transaction result = paymentService.processPayment(paymentRequest, merchant);

        // Then: Transaction should use merchant default redirect URL
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, atLeastOnce()).save(transactionCaptor.capture());
        
        Transaction capturedTransaction = transactionCaptor.getValue();
        assertEquals("https://example.com/default/redirect", capturedTransaction.getRedirectUrl());
    }

    @Test
    void testProcessPayment_MixedRedirectUrls() {
        // Given: Payment request with only success URL, should fall back to merchant default for others
        paymentRequest.setSuccessUrl("https://example.com/custom/success");

        Transaction savedTransaction = Transaction.builder()
                .id(1L)
                .transactionId("txn_123")
                .status("PENDING")
                .redirectUrl("https://example.com/default/redirect")
                .successUrl("https://example.com/custom/success")
                .failureUrl("https://example.com/default/redirect")
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        // When: Processing payment
        Transaction result = paymentService.processPayment(paymentRequest, merchant);

        // Then: Transaction should have mixed URLs
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, atLeastOnce()).save(transactionCaptor.capture());
        
        Transaction capturedTransaction = transactionCaptor.getValue();
        assertEquals("https://example.com/default/redirect", capturedTransaction.getRedirectUrl());
        assertEquals("https://example.com/custom/success", capturedTransaction.getSuccessUrl());
        assertEquals("https://example.com/default/redirect", capturedTransaction.getFailureUrl());
    }

    @Test
    void testProcessPayment_NoRedirectUrls() {
        // Given: Neither request nor merchant has redirect URLs
        merchant.setDefaultRedirectUrl(null);

        Transaction savedTransaction = Transaction.builder()
                .id(1L)
                .transactionId("txn_123")
                .status("PENDING")
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        // When: Processing payment
        Transaction result = paymentService.processPayment(paymentRequest, merchant);

        // Then: Transaction should have null redirect URLs
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, atLeastOnce()).save(transactionCaptor.capture());
        
        Transaction capturedTransaction = transactionCaptor.getValue();
        assertNull(capturedTransaction.getRedirectUrl());
        assertNull(capturedTransaction.getSuccessUrl());
        assertNull(capturedTransaction.getFailureUrl());
    }
}
