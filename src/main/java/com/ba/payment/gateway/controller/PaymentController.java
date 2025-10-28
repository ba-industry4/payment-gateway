package com.ba.payment.gateway.controller;

import com.ba.payment.gateway.dto.PaymentRequest;
import com.ba.payment.gateway.entity.Merchant;
import com.ba.payment.gateway.entity.Transaction;
import com.ba.payment.gateway.repository.MerchantRepository;
import com.ba.payment.gateway.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Transaction> processPayment(
            @PathVariable Long merchantId,
            @RequestBody PaymentRequest request) {
        
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));

        Transaction transaction = paymentService.processPayment(request, merchant);
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MERCHANT', 'API_USER')")
    public ResponseEntity<Transaction> getTransaction(@PathVariable String transactionId) {
        // This would be implemented with proper service layer
        return ResponseEntity.ok(null);
    }
}
