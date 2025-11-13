package com.ba.payment.gateway.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JPA Attribute Converter for automatic encryption/decryption of sensitive data.
 * Applied to database columns containing payment provider credentials.
 *
 * Usage:
 * <pre>
 * @Column(name = "provider_config", columnDefinition = "TEXT")
 * @Convert(converter = EncryptedStringConverter.class)
 * private String providerConfig;
 * </pre>
 *
 * Security:
 * - Automatically encrypts data before saving to database
 * - Automatically decrypts data when loading from database
 * - Transparent to application code
 * - Uses AES-256-GCM encryption
 */
@Slf4j
@Component
@Converter
@RequiredArgsConstructor
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final EncryptionService encryptionService;

    /**
     * Converts the entity attribute (plaintext) to database column (encrypted).
     * Called when saving entity to database.
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }

        try {
            String encrypted = encryptionService.encrypt(attribute);
            log.debug("Encrypted sensitive data for database storage (length: {} -> {})",
                    attribute.length(), encrypted.length());
            return encrypted;
        } catch (Exception e) {
            log.error("Failed to encrypt attribute for database storage", e);
            throw new RuntimeException("Failed to encrypt sensitive data", e);
        }
    }

    /**
     * Converts the database column (encrypted) to entity attribute (plaintext).
     * Called when loading entity from database.
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }

        try {
            String decrypted = encryptionService.decrypt(dbData);
            log.debug("Decrypted sensitive data from database (length: {} -> {})",
                    dbData.length(), decrypted.length());
            return decrypted;
        } catch (Exception e) {
            log.error("Failed to decrypt data from database. Data may be corrupted or key changed.", e);
            throw new RuntimeException("Failed to decrypt sensitive data", e);
        }
    }
}

