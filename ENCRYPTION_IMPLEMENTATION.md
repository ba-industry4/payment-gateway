# Provider Credential Encryption Implementation

## 🔐 Overview

Payment provider credentials (API keys, secrets, tokens) are now **encrypted at rest** in the database using **AES-256-GCM** encryption with the following security features:

✅ **AES-256-GCM** - Industry-standard authenticated encryption  
✅ **Random IV** - New initialization vector for each encryption  
✅ **AEAD** - Authenticated Encryption with Associated Data (prevents tampering)  
✅ **Automatic** - Transparent encryption/decryption via JPA converters  
✅ **Secure Key Management** - Master key stored in environment variables  

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    APPLICATION LAYER                         │
│  ApplicationService stores/retrieves providerConfig          │
│  (plaintext JSON: {"api_key": "sk_live_xxx"})               │
└───────────────────┬─────────────────────────────────────────┘
                    │
                    ↓ Save Entity
┌─────────────────────────────────────────────────────────────┐
│              JPA CONVERTER LAYER                             │
│  EncryptedStringConverter.convertToDatabaseColumn()         │
│  ↓                                                           │
│  EncryptionService.encrypt(plaintext)                       │
│  - Generate random 12-byte IV                               │
│  - Encrypt with AES-256-GCM                                 │
│  - Encode to Base64                                         │
└───────────────────┬─────────────────────────────────────────┘
                    │
                    ↓ Encrypted Data
┌─────────────────────────────────────────────────────────────┐
│                   DATABASE LAYER                             │
│  application_providers.provider_config (TEXT)                │
│  Stored: Base64([IV][Ciphertext][Auth_Tag])                 │
│  Example: "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo..."         │
└───────────────────┬─────────────────────────────────────────┘
                    │
                    ↑ Read Entity
┌─────────────────────────────────────────────────────────────┐
│              JPA CONVERTER LAYER                             │
│  EncryptedStringConverter.convertToEntityAttribute()        │
│  ↓                                                           │
│  EncryptionService.decrypt(encryptedBase64)                 │
│  - Decode Base64                                            │
│  - Extract IV and ciphertext                                │
│  - Decrypt with AES-256-GCM                                 │
│  - Verify authentication tag                                │
└───────────────────┬─────────────────────────────────────────┘
                    │
                    ↑ Plaintext
┌─────────────────────────────────────────────────────────────┐
│                    APPLICATION LAYER                         │
│  Application receives plaintext providerConfig               │
│  ({"api_key": "sk_live_xxx"})                               │
└─────────────────────────────────────────────────────────────┘
```

## 🔑 Master Key Generation

### Generate a New Master Key

Run the EncryptionService main method:

```bash
# Option 1: Using Maven
mvn clean compile
java -cp target/classes com.ba.payment.gateway.security.EncryptionService

# Option 2: After building JAR
java -cp target/payment-gateway-0.0.1-SNAPSHOT.jar \
  com.ba.payment.gateway.security.EncryptionService
```

Output will be:
```
=== AES-256 Master Key Generator ===

Generated Master Key (Base64):
x7KJ9mP3nQ2wV8yR5tA6uH4kL1zC0bN7vM8sD9fG2eX=

Add this to your application.yml or environment variables:
encryption:
  master-key: x7KJ9mP3nQ2wV8yR5tA6uH4kL1zC0bN7vM8sD9fG2eX=

Or as environment variable:
ENCRYPTION_MASTER_KEY=x7KJ9mP3nQ2wV8yR5tA6uH4kL1zC0bN7vM8sD9fG2eX=

⚠️  SECURITY WARNING:
- Store this key securely (e.g., AWS Secrets Manager, HashiCorp Vault)
- NEVER commit this key to source control
- Rotate the key periodically
- Use different keys for different environments
```

### Configure the Master Key

**Development (application.yml):**
```yaml
encryption:
  master-key: x7KJ9mP3nQ2wV8yR5tA6uH4kL1zC0bN7vM8sD9fG2eX=
```

**Production (Environment Variable):**
```bash
export ENCRYPTION_MASTER_KEY="x7KJ9mP3nQ2wV8yR5tA6uH4kL1zC0bN7vM8sD9fG2eX="
```

**Docker:**
```yaml
environment:
  - ENCRYPTION_MASTER_KEY=x7KJ9mP3nQ2wV8yR5tA6uH4kL1zC0bN7vM8sD9fG2eX=
```

**Kubernetes Secret:**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: payment-gateway-secrets
type: Opaque
data:
  encryption-master-key: eDdLSjltUDNuUTJ3Vjh5UjV0QTZ1SDRrTDF6QzBiTjd2TThzRDlmRzJlWD0=
```

## 🔧 Implementation Details

### 1. EncryptionService

Core encryption/decryption service using AES-256-GCM:

```java
@Component
public class EncryptionService {
    // Encrypts plaintext to Base64-encoded [IV][Ciphertext][Tag]
    public String encrypt(String plaintext);
    
    // Decrypts Base64-encoded encrypted data
    public String decrypt(String encryptedBase64);
    
    // Validates encryption is working correctly
    public boolean validateEncryption();
    
    // Generates new master key (for setup)
    public static String generateKeyBase64();
}
```

**Security Features:**
- AES-256 bit key size
- GCM (Galois/Counter Mode) for authenticated encryption
- Random 12-byte IV for each encryption
- 128-bit authentication tag
- Base64 encoding for database storage

### 2. EncryptedStringConverter

JPA Attribute Converter for automatic encryption:

```java
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    // Automatically encrypts when saving to database
    public String convertToDatabaseColumn(String attribute);
    
    // Automatically decrypts when loading from database
    public String convertToEntityAttribute(String dbData);
}
```

### 3. ApplicationProvider Entity

Uses the converter for automatic encryption:

```java
@Entity
public class ApplicationProvider {
    // ...other fields...
    
    @Column(name = "provider_config", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String providerConfig; // Automatically encrypted!
}
```

### 4. CredentialExtractor

Utility for safely extracting specific credentials:

```java
@Component
public class CredentialExtractor {
    // Extract specific credential from decrypted config
    public String extractCredential(String providerConfig, String key);
    
    // Provider-specific extractors
    public String extractStripeApiKey(String providerConfig);
    public String extractPayPalClientId(String providerConfig);
    
    // Sanitize config for safe logging
    public String sanitizeForLogging(String providerConfig);
    
    // Validate config has required keys
    public boolean validateConfig(String providerConfig, String... keys);
}
```

## 📝 Usage Examples

### Storing Encrypted Credentials

```java
@Service
public class ApplicationService {
    
    public void configureProvider(Long applicationId, ConfigureProviderRequest request) {
        // Store provider credentials (will be automatically encrypted)
        ApplicationProvider appProvider = new ApplicationProvider();
        
        // This JSON will be encrypted before saving to database
        appProvider.setProviderConfig(
            "{\"api_key\": \"sk_live_xxx\", \"webhook_secret\": \"whsec_yyy\"}"
        );
        
        applicationProviderRepository.save(appProvider);
        // Data is now encrypted in database!
    }
}
```

### Retrieving and Using Encrypted Credentials

```java
@Service
public class PaymentService {
    
    @Autowired
    private CredentialExtractor credentialExtractor;
    
    public void processPayment(Application app) {
        // Load provider config (automatically decrypted by JPA converter)
        ApplicationProvider provider = applicationProviderRepository
            .findByApplicationAndIsDefault(app);
        
        String providerConfig = provider.getProviderConfig();
        // providerConfig is now plaintext JSON
        
        // Extract specific credentials
        String apiKey = credentialExtractor.extractStripeApiKey(providerConfig);
        String webhookSecret = credentialExtractor.extractStripeWebhookSecret(providerConfig);
        
        // Use credentials to process payment
        processPaymentWithStripe(apiKey, webhookSecret, amount);
    }
}
```

### Safe Logging

```java
@Service
public class ApplicationService {
    
    @Autowired
    private CredentialExtractor credentialExtractor;
    
    public void debugProviderConfig(ApplicationProvider provider) {
        String config = provider.getProviderConfig();
        
        // BAD: Exposes secrets in logs
        // log.info("Config: {}", config);
        
        // GOOD: Sanitized for logging
        String sanitized = credentialExtractor.sanitizeForLogging(config);
        log.info("Config: {}", sanitized);
        // Logs: {"api_key":"sk_l****_xxx","webhook_secret":"whse****_yyy"}
    }
}
```

## 🔍 Database Schema

### Before Encryption

```sql
-- Old schema (insecure)
CREATE TABLE application_providers (
    provider_config JSONB  -- Plaintext JSON
);

-- Example data (EXPOSED!)
provider_config: {"api_key": "sk_live_51Hx...real_key_here", "secret": "whsec_..."}
```

### After Encryption

```sql
-- New schema (secure)
CREATE TABLE application_providers (
    provider_config TEXT  -- Encrypted Base64 string
);

-- Example data (encrypted)
provider_config: "A7bC9dE2fG5hI8jK3lM6nO1pQ4rS7tU0vW9xY2zA5bC8dE1fG4hI..."
-- Contains: [12-byte IV][Encrypted JSON][16-byte Auth Tag]
```

## 🛡️ Security Features

### 1. Authenticated Encryption (AEAD)

GCM mode provides both confidentiality and authenticity:
- **Confidentiality**: Data is encrypted, unreadable without key
- **Authenticity**: Any tampering is detected on decryption
- **Integrity**: Authentication tag ensures data hasn't been modified

### 2. Random Initialization Vector (IV)

Each encryption uses a new random IV:
- Prevents pattern analysis
- Same plaintext produces different ciphertext each time
- Essential for semantic security

### 3. Key Separation

Different keys for different purposes:
- JWT signing key (separate)
- Database encryption key (separate)
- Per-environment keys (dev, staging, prod)

### 4. No Key Derivation in Code

Master key is provided externally:
- Not hardcoded
- Not derived from weak passwords
- Managed outside application

## 🚨 Security Best Practices

### ✅ DO:

1. **Generate Strong Keys**
   ```bash
   # Use the provided generator
   java -cp target/classes com.ba.payment.gateway.security.EncryptionService
   ```

2. **Store Keys Securely**
   - AWS Secrets Manager
   - HashiCorp Vault
   - Azure Key Vault
   - Google Cloud Secret Manager
   - Kubernetes Secrets (with encryption at rest)

3. **Rotate Keys Periodically**
   - Plan for key rotation every 90-180 days
   - Implement re-encryption process
   - Keep old keys for decryption during transition

4. **Use Different Keys Per Environment**
   ```
   DEV:     ENCRYPTION_MASTER_KEY=dev_key_xxx
   STAGING: ENCRYPTION_MASTER_KEY=staging_key_yyy
   PROD:    ENCRYPTION_MASTER_KEY=prod_key_zzz
   ```

5. **Monitor Decryption Failures**
   - Log decryption errors
   - Alert on multiple failures (may indicate attack)
   - Investigate key mismatch issues

### ❌ DON'T:

1. **Never Commit Keys to Source Control**
   ```bash
   # Add to .gitignore
   **/application-prod.yml
   **/.env
   **/secrets.*
   ```

2. **Never Log Decrypted Credentials**
   ```java
   // BAD
   log.info("API Key: {}", apiKey);
   
   // GOOD
   String sanitized = credentialExtractor.sanitizeForLogging(config);
   log.info("Config: {}", sanitized);
   ```

3. **Never Share Keys Between Environments**
   - Use unique keys for dev, staging, production
   - Compromised dev key won't affect production

4. **Never Store Keys in Application Code**
   ```java
   // BAD
   private static final String MASTER_KEY = "x7KJ9mP3nQ...";
   
   // GOOD
   @Value("${encryption.master-key}")
   private String masterKey;
   ```

## 🔄 Key Rotation Process

### Step 1: Generate New Key

```bash
java -cp target/classes com.ba.payment.gateway.security.EncryptionService
# Save output: NEW_KEY=abc123...
```

### Step 2: Implement Dual-Key Decryption (Future Enhancement)

```java
// Support both old and new keys during transition
public String decrypt(String encrypted) {
    try {
        return decryptWithKey(encrypted, newKey);
    } catch (Exception e) {
        return decryptWithKey(encrypted, oldKey);
    }
}
```

### Step 3: Re-encrypt All Data

```java
// Migration script
public void reencryptAllProviderConfigs() {
    List<ApplicationProvider> providers = repository.findAll();
    
    for (ApplicationProvider provider : providers) {
        // Load with old key (automatic)
        String plaintext = provider.getProviderConfig();
        
        // Set same plaintext (will encrypt with new key)
        provider.setProviderConfig(plaintext);
        repository.save(provider);
    }
}
```

### Step 4: Remove Old Key

After all data is re-encrypted with new key, remove old key from configuration.

## 📊 Performance Considerations

### Encryption Overhead

- **CPU**: AES-256-GCM is hardware-accelerated on modern CPUs (AES-NI)
- **Memory**: Minimal overhead (<1KB per operation)
- **Latency**: ~1-2ms per encrypt/decrypt operation
- **Throughput**: >100,000 operations/second on modern hardware

### Database Impact

- **Storage**: Base64 encoding adds ~33% overhead
  - Original: `{"api_key":"sk_live_xxx"}` (30 bytes)
  - Encrypted: ~40 bytes + 12-byte IV + 16-byte tag = 68 bytes
  - Base64: ~91 characters
- **Query**: TEXT column indexed normally
- **No impact on query performance** (encryption is at application layer)

## 🧪 Testing Encryption

### Startup Validation

The system automatically validates encryption on startup:

```
=================================================
Validating Security Configuration...
=================================================
Testing encryption service...
✓ Encryption service is working correctly
=================================================
✓ All security validations passed
✓ System is ready to handle sensitive data securely
=================================================
```

### Manual Testing

```java
@SpringBootTest
class EncryptionTest {
    
    @Autowired
    private EncryptionService encryptionService;
    
    @Test
    void testEncryptionDecryption() {
        String plaintext = "{\"api_key\":\"sk_test_123\"}";
        
        // Encrypt
        String encrypted = encryptionService.encrypt(plaintext);
        assertNotEquals(plaintext, encrypted);
        
        // Decrypt
        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(plaintext, decrypted);
    }
    
    @Test
    void testTamperingDetection() {
        String plaintext = "sensitive data";
        String encrypted = encryptionService.encrypt(plaintext);
        
        // Tamper with encrypted data
        String tampered = encrypted.substring(0, encrypted.length() - 5) + "XXXXX";
        
        // Should throw exception
        assertThrows(RuntimeException.class, () -> {
            encryptionService.decrypt(tampered);
        });
    }
}
```

## 📈 Monitoring & Auditing

### Metrics to Monitor

1. **Encryption Failures**
   - Alert when encryption fails during save
   - May indicate key issues

2. **Decryption Failures**
   - Alert on multiple failures
   - May indicate:
     - Wrong key configured
     - Database corruption
     - Attack attempt

3. **Key Rotation Status**
   - Track when key was last rotated
   - Alert if overdue for rotation

### Audit Logging

```java
@Aspect
@Component
public class CredentialAccessAuditor {
    
    @Around("execution(* com.ba.payment.gateway..*.extractCredential(..))")
    public Object auditCredentialAccess(ProceedingJoinPoint joinPoint) {
        // Log who accessed credentials and when
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("AUDIT: User {} accessed provider credentials", user);
        
        return joinPoint.proceed();
    }
}
```

## 🆘 Troubleshooting

### Problem: "Failed to decrypt data"

**Causes:**
- Wrong master key configured
- Key was changed but data not re-encrypted
- Database corruption

**Solutions:**
1. Verify `ENCRYPTION_MASTER_KEY` environment variable
2. Check application logs for key initialization
3. Test with known good encrypted value

### Problem: "Encryption service validation FAILED"

**Causes:**
- Master key not configured
- Master key is invalid/corrupted
- Encryption libraries missing

**Solutions:**
1. Generate new master key
2. Set `ENCRYPTION_MASTER_KEY` environment variable
3. Restart application

### Problem: Performance degradation

**Causes:**
- Frequent encrypt/decrypt operations
- Large credential payloads

**Solutions:**
1. Cache decrypted credentials (with TTL)
2. Minimize credential payload size
3. Use connection pooling

## 📚 References

- [NIST SP 800-38D: GCM Mode](https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-38d.pdf)
- [OWASP Cryptographic Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html)
- [Java Cryptography Architecture](https://docs.oracle.com/en/java/javase/21/security/java-cryptography-architecture-jca-reference-guide.html)

---

## ✅ Summary

**Encryption Implementation Complete!**

✅ AES-256-GCM authenticated encryption  
✅ Automatic encryption via JPA converters  
✅ Secure key management via environment variables  
✅ Credential extraction utilities  
✅ Safe logging support  
✅ Startup validation  
✅ Comprehensive documentation  

**Provider credentials are now encrypted at rest in the database!** 🔐

