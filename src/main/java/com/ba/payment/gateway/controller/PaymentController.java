package com.ba.payment.gateway.controller;

import com.ba.payment.gateway.dto.PaymentRequest;
import com.ba.payment.gateway.dto.PaymentResponse;
import com.ba.payment.gateway.entity.Merchant;
import com.ba.payment.gateway.entity.Transaction;
import com.ba.payment.gateway.repository.MerchantRepository;
import com.ba.payment.gateway.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Payment Controller following Single Responsibility Principle (SOLID)
 * Handles HTTP requests related to payment processing
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final MerchantRepository merchantRepository;

    @PostMapping("/{merchantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MERCHANT', 'API_USER')")
    public ResponseEntity<PaymentResponse> processPayment(
            @PathVariable Long merchantId,
            @RequestBody PaymentRequest request) {
        
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));

        Transaction transaction = paymentService.processPayment(request, merchant);
        
        // Convert to response DTO with appropriate redirect URL based on status
        PaymentResponse response = convertToResponse(transaction);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MERCHANT', 'API_USER')")
    public ResponseEntity<Transaction> getTransaction(@PathVariable String transactionId) {
        // TODO: Implement transaction retrieval logic using TransactionService
        throw new UnsupportedOperationException("Transaction retrieval not yet implemented");
    }

    /**
     * Converts Transaction entity to PaymentResponse DTO with appropriate redirect URL
     */
    private PaymentResponse convertToResponse(Transaction transaction) {
        String redirectUrl = determineRedirectUrl(transaction);
        
        return PaymentResponse.builder()
                .transactionId(transaction.getTransactionId())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .provider(transaction.getProvider())
                .providerTransactionId(transaction.getProviderTransactionId())
                .description(transaction.getDescription())
                .redirectUrl(redirectUrl)
                .failureReason(transaction.getFailureReason())
                .processedAt(transaction.getProcessedAt())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    /**
     * Determines the appropriate redirect URL based on transaction status
     */
    private String determineRedirectUrl(Transaction transaction) {
        String baseUrl;
        
        if ("COMPLETED".equals(transaction.getStatus())) {
            // Use success URL if available, otherwise use generic redirect URL
            baseUrl = transaction.getSuccessUrl() != null 
                    ? transaction.getSuccessUrl() 
                    : transaction.getRedirectUrl();
        } else if ("FAILED".equals(transaction.getStatus())) {
            // Use failure URL if available, otherwise use generic redirect URL
            baseUrl = transaction.getFailureUrl() != null 
                    ? transaction.getFailureUrl() 
                    : transaction.getRedirectUrl();
        } else {
            // For PENDING or other statuses, use generic redirect URL
            baseUrl = transaction.getRedirectUrl();
        }
        
        // If we have a redirect URL, append transaction details as query parameters
        if (baseUrl != null && !baseUrl.isEmpty()) {
            return UriComponentsBuilder.fromUriString(baseUrl)
                    .queryParam("transactionId", transaction.getTransactionId())
                    .queryParam("status", transaction.getStatus())
                    .queryParam("amount", transaction.getAmount())
                    .queryParam("currency", transaction.getCurrency())
                    .build()
                    .toUriString();
        }
        
        return null;
    }
}
