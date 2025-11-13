package com.ba.payment.gateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for encrypting and decrypting sensitive data using AES-256-GCM.
 * Used to protect payment provider credentials stored in the database.
 *
 * Security features:
 * - AES-256-GCM (Galois/Counter Mode) for authenticated encryption
 * - Random IV (Initialization Vector) for each encryption
 * - AEAD (Authenticated Encryption with Associated Data)
 * - Protection against tampering
 */
@Slf4j
@Component
public class EncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int GCM_IV_LENGTH = 12; // bytes - recommended for GCM
    private static final int AES_KEY_SIZE = 256; // bits

    private final SecretKey secretKey;

    public EncryptionService(@Value("${encryption.master-key}") String masterKeyBase64) {
        try {
            if (masterKeyBase64 == null || masterKeyBase64.trim().isEmpty()) {
                log.warn("No encryption master key provided. Generating temporary key. THIS IS NOT SECURE FOR PRODUCTION!");
                this.secretKey = generateKey();
            } else {
                byte[] decodedKey = Base64.getDecoder().decode(masterKeyBase64);
                this.secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
                log.info("Encryption service initialized with provided master key");
            }
        } catch (Exception e) {
            log.error("Failed to initialize encryption service", e);
            throw new RuntimeException("Failed to initialize encryption service", e);
        }
    }

    /**
     * Encrypts plaintext using AES-256-GCM.
     * Returns Base64-encoded string: [IV][ciphertext][tag]
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }

        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // Encrypt
            byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plaintextBytes);

            // Combine IV + ciphertext (ciphertext includes authentication tag)
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            // Encode to Base64 for storage
            return Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }

    /**
     * Decrypts Base64-encoded ciphertext encrypted with AES-256-GCM.
     * Expects format: [IV][ciphertext][tag]
     */
    public String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) {
            return encryptedBase64;
        }

        try {
            // Decode from Base64
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedBase64);

            // Extract IV and ciphertext
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedBytes);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // Decrypt and verify authentication tag
            byte[] plaintextBytes = cipher.doFinal(ciphertext);

            return new String(plaintextBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Decryption failed. Data may be corrupted or tampered with.", e);
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }

    /**
     * Generates a new AES-256 key.
     * Use this to generate a master key for production:
     *
     * <pre>
     * EncryptionService service = new EncryptionService("");
     * String key = service.generateKeyBase64();
     * System.out.println("Master Key: " + key);
     * </pre>
     */
    public static SecretKey generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(AES_KEY_SIZE, new SecureRandom());
            return keyGenerator.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }

    /**
     * Generates a Base64-encoded master key suitable for configuration.
     */
    public static String generateKeyBase64() {
        SecretKey key = generateKey();
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * Validates that the encryption service is working correctly.
     */
    public boolean validateEncryption() {
        try {
            String testData = "test-encryption-" + System.currentTimeMillis();
            String encrypted = encrypt(testData);
            String decrypted = decrypt(encrypted);
            boolean valid = testData.equals(decrypted);

            if (valid) {
                log.info("Encryption service validation passed");
            } else {
                log.error("Encryption service validation failed: decrypted data doesn't match");
            }

            return valid;
        } catch (Exception e) {
            log.error("Encryption service validation failed with exception", e);
            return false;
        }
    }

    /**
     * Main method to generate a new master key for production use.
     * Run this and store the output in your environment variables.
     */
    public static void main(String[] args) {
        System.out.println("=== AES-256 Master Key Generator ===");
        System.out.println();
        System.out.println("Generated Master Key (Base64):");
        String key = generateKeyBase64();
        System.out.println(key);
        System.out.println();
        System.out.println("Add this to your application.yml or environment variables:");
        System.out.println("encryption:");
        System.out.println("  master-key: " + key);
        System.out.println();
        System.out.println("Or as environment variable:");
        System.out.println("ENCRYPTION_MASTER_KEY=" + key);
        System.out.println();
        System.out.println("⚠️  SECURITY WARNING:");
        System.out.println("- Store this key securely (e.g., AWS Secrets Manager, HashiCorp Vault)");
        System.out.println("- NEVER commit this key to source control");
        System.out.println("- Rotate the key periodically");
        System.out.println("- Use different keys for different environments");
    }
}

