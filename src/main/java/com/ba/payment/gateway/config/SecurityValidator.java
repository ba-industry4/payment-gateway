package com.ba.payment.gateway.config;

import com.ba.payment.gateway.security.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Validates critical security configurations on application startup.
 * Ensures encryption service is properly configured before processing any data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityValidator {

    private final EncryptionService encryptionService;

    @EventListener(ApplicationReadyEvent.class)
    public void validateOnStartup() {
        log.info("=================================================");
        log.info("Validating Security Configuration...");
        log.info("=================================================");

        boolean allValid = true;

        // Validate Encryption Service
        log.info("Testing encryption service...");
        if (encryptionService.validateEncryption()) {
            log.info("✓ Encryption service is working correctly");
        } else {
            log.error("✗ Encryption service validation FAILED!");
            log.error("  Provider credentials will NOT be secure!");
            allValid = false;
        }

        // Check for default/weak master key
        try {
            String testData = "security-check";
            String encrypted = encryptionService.encrypt(testData);
            if (encrypted.equals(testData)) {
                log.warn("✗ Encryption appears to be bypassed or using identity function!");
                allValid = false;
            }
        } catch (Exception e) {
            log.error("✗ Encryption test failed with exception", e);
            allValid = false;
        }

        log.info("=================================================");
        if (allValid) {
            log.info("✓ All security validations passed");
            log.info("✓ System is ready to handle sensitive data securely");
        } else {
            log.error("✗ SECURITY VALIDATION FAILED!");
            log.error("✗ DO NOT USE IN PRODUCTION!");
            log.error("✗ Fix encryption configuration before proceeding");
        }
        log.info("=================================================");
    }
}

