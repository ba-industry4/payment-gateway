package com.ba.payment.gateway.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Utility for safely handling provider credentials from encrypted configuration.
 * Provides helper methods to extract specific credentials without exposing the entire config.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CredentialExtractor {

    private final ObjectMapper objectMapper;

    /**
     * Extracts a specific credential from the provider configuration JSON.
     *
     * @param providerConfig Decrypted JSON configuration string
     * @param credentialKey The key to extract (e.g., "api_key", "client_id")
     * @return The credential value, or null if not found
     */
    public String extractCredential(String providerConfig, String credentialKey) {
        if (providerConfig == null || providerConfig.isEmpty()) {
            log.warn("Provider config is null or empty");
            return null;
        }

        try {
            JsonNode configNode = objectMapper.readTree(providerConfig);
            JsonNode credentialNode = configNode.get(credentialKey);

            if (credentialNode != null && !credentialNode.isNull()) {
                return credentialNode.asText();
            }

            log.debug("Credential key '{}' not found in provider config", credentialKey);
            return null;

        } catch (JsonProcessingException e) {
            log.error("Failed to parse provider config JSON", e);
            return null;
        }
    }

    /**
     * Extracts Stripe API key from provider configuration.
     */
    public String extractStripeApiKey(String providerConfig) {
        return extractCredential(providerConfig, "api_key");
    }

    /**
     * Extracts Stripe webhook secret from provider configuration.
     */
    public String extractStripeWebhookSecret(String providerConfig) {
        return extractCredential(providerConfig, "webhook_secret");
    }

    /**
     * Extracts PayPal client ID from provider configuration.
     */
    public String extractPayPalClientId(String providerConfig) {
        return extractCredential(providerConfig, "client_id");
    }

    /**
     * Extracts PayPal client secret from provider configuration.
     */
    public String extractPayPalClientSecret(String providerConfig) {
        return extractCredential(providerConfig, "client_secret");
    }

    /**
     * Extracts Square access token from provider configuration.
     */
    public String extractSquareAccessToken(String providerConfig) {
        return extractCredential(providerConfig, "access_token");
    }

    /**
     * Creates a sanitized version of the config for logging (replaces sensitive values).
     * Use this when you need to log configuration for debugging.
     */
    public String sanitizeForLogging(String providerConfig) {
        if (providerConfig == null || providerConfig.isEmpty()) {
            return providerConfig;
        }

        try {
            JsonNode configNode = objectMapper.readTree(providerConfig);
            ObjectNode sanitized = objectMapper.createObjectNode();

            configNode.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                String value = entry.getValue().asText();

                // Replace sensitive values with masked version
                if (isSensitiveKey(key)) {
                    sanitized.put(key, maskSensitiveValue(value));
                } else {
                    sanitized.put(key, value);
                }
            });

            return objectMapper.writeValueAsString(sanitized);

        } catch (JsonProcessingException e) {
            log.error("Failed to sanitize config for logging", e);
            return "[FAILED_TO_SANITIZE]";
        }
    }

    /**
     * Checks if a key represents sensitive data.
     */
    private boolean isSensitiveKey(String key) {
        String lowerKey = key.toLowerCase();
        return lowerKey.contains("key")
            || lowerKey.contains("secret")
            || lowerKey.contains("token")
            || lowerKey.contains("password")
            || lowerKey.contains("credential");
    }

    /**
     * Masks a sensitive value for safe logging.
     * Shows first 4 and last 4 characters, masks the rest.
     */
    private String maskSensitiveValue(String value) {
        if (value == null || value.length() <= 8) {
            return "***MASKED***";
        }

        int showChars = 4;
        String start = value.substring(0, showChars);
        String end = value.substring(value.length() - showChars);
        String masked = "*".repeat(value.length() - (showChars * 2));

        return start + masked + end;
    }

    /**
     * Validates that provider config contains required credentials.
     *
     * @param providerConfig The configuration to validate
     * @param requiredKeys Keys that must be present
     * @return true if all required keys are present
     */
    public boolean validateConfig(String providerConfig, String... requiredKeys) {
        if (providerConfig == null || providerConfig.isEmpty()) {
            log.warn("Provider config is null or empty");
            return false;
        }

        try {
            JsonNode configNode = objectMapper.readTree(providerConfig);

            for (String key : requiredKeys) {
                if (!configNode.has(key) || configNode.get(key).isNull()) {
                    log.warn("Required credential key '{}' is missing from provider config", key);
                    return false;
                }
            }

            return true;

        } catch (JsonProcessingException e) {
            log.error("Failed to validate provider config", e);
            return false;
        }
    }
}

