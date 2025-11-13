# Quick Setup Guide - Provider Credential Encryption

## 🚀 5-Minute Setup

### Step 1: Generate Master Key

```bash
# Compile the project
cd /d/Projects/pg-micro-service/payment-gateway
mvn clean compile

# Generate encryption key
java -cp target/classes com.ba.payment.gateway.security.EncryptionService
```

**Output:**
```
=== AES-256 Master Key Generator ===

Generated Master Key (Base64):
x7KJ9mP3nQ2wV8yR5tA6uH4kL1zC0bN7vM8sD9fG2eX=

⚠️  SECURITY WARNING:
- Store this key securely
- NEVER commit to source control
```

### Step 2: Configure Master Key

**Option A: Environment Variable (Recommended for Production)**
```bash
export ENCRYPTION_MASTER_KEY="x7KJ9mP3nQ2wV8yR5tA6uH4kL1zC0bN7vM8sD9fG2eX="
```

**Option B: application.yml (Development Only)**
```yaml
encryption:
  master-key: x7KJ9mP3nQ2wV8yR5tA6uH4kL1zC0bN7vM8sD9fG2eX=
```

### Step 3: Run Migrations

```bash
mvn liquibase:update
```

This updates `application_providers.provider_config` column to TEXT type for encrypted storage.

### Step 4: Start Application

```bash
mvn spring-boot:run
```

**Check logs for:**
```
=================================================
Validating Security Configuration...
=================================================
✓ Encryption service is working correctly
=================================================
✓ All security validations passed
✓ System is ready to handle sensitive data securely
=================================================
```

### Step 5: Test Encryption

```bash
# Create an application
curl -X POST http://localhost:8996/api/applications \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Client",
    "appType": "E_COMMERCE"
  }'

# Configure provider with credentials
curl -X POST http://localhost:8996/api/applications/1/providers \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "paymentProviderId": 1,
    "isDefault": true,
    "providerConfig": "{\"api_key\": \"sk_live_test_xxx\", \"webhook_secret\": \"whsec_test_yyy\"}"
  }'
```

### Step 6: Verify Encryption in Database

```bash
# Connect to database
psql -U postgres -d payment_gateway

# Check encrypted data
SELECT id, application_id, 
       LEFT(provider_config, 50) as encrypted_config
FROM application_providers;
```

**You should see Base64-encrypted data:**
```
 id | application_id | encrypted_config
----+----------------+--------------------------------------------------
  1 |              1 | YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXphYmNkZWZnaGlqa...
```

✅ **If you see encrypted Base64 strings, encryption is working!**

---

## 🔐 What Just Happened?

1. ✅ **Generated** a secure 256-bit AES encryption key
2. ✅ **Configured** the key in your environment
3. ✅ **Updated** database schema for encrypted storage
4. ✅ **Validated** encryption on startup
5. ✅ **Encrypted** provider credentials automatically
6. ✅ **Verified** data is encrypted in database

## 📝 Usage in Code

### Storing Credentials (Automatic Encryption)

```java
// Create provider config
ApplicationProvider provider = new ApplicationProvider();
provider.setProviderConfig(
    "{\"api_key\": \"sk_live_xxx\", \"secret\": \"whsec_yyy\"}"
);

// Save - automatically encrypted before database insert!
applicationProviderRepository.save(provider);
```

### Retrieving Credentials (Automatic Decryption)

```java
// Load provider
ApplicationProvider provider = applicationProviderRepository.findById(1);

// Get config - automatically decrypted!
String config = provider.getProviderConfig();
// Returns: {"api_key": "sk_live_xxx", "secret": "whsec_yyy"}

// Extract specific credential
String apiKey = credentialExtractor.extractStripeApiKey(config);
```

## 🛡️ Security Checklist

- [ ] Generated unique master key
- [ ] Stored key securely (not in source control)
- [ ] Set `ENCRYPTION_MASTER_KEY` environment variable
- [ ] Verified encryption on startup (check logs)
- [ ] Tested with sample credentials
- [ ] Verified data is encrypted in database
- [ ] Used different keys for dev/staging/prod

## 🚨 Production Deployment

### AWS (Secrets Manager)

```bash
# Store key in AWS Secrets Manager
aws secretsmanager create-secret \
  --name payment-gateway/encryption-key \
  --secret-string "x7KJ9mP3nQ2wV8yR5tA6uH4kL1zC0bN7vM8sD9fG2eX="

# Reference in ECS/EKS
environment:
  - name: ENCRYPTION_MASTER_KEY
    valueFrom:
      secretRef:
        name: payment-gateway/encryption-key
```

### Kubernetes

```yaml
# Create secret
kubectl create secret generic payment-gateway-encryption \
  --from-literal=master-key='x7KJ9mP3nQ2wV8yR5tA6uH4kL1zC0bN7vM8sD9fG2eX='

# Reference in deployment
env:
  - name: ENCRYPTION_MASTER_KEY
    valueFrom:
      secretKeyRef:
        name: payment-gateway-encryption
        key: master-key
```

### Docker Compose

```yaml
services:
  payment-gateway:
    environment:
      - ENCRYPTION_MASTER_KEY=${ENCRYPTION_MASTER_KEY}
    env_file:
      - .env.production  # Contains ENCRYPTION_MASTER_KEY=...
```

## 📚 Full Documentation

See `ENCRYPTION_IMPLEMENTATION.md` for:
- Complete architecture details
- Security best practices
- Key rotation procedures
- Troubleshooting guide
- Performance considerations

---

**Encryption setup complete! Provider credentials are now secure! 🔐**

