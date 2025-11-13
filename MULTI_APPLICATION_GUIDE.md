# Multi-Application Payment Provider Management

## Overview

This payment gateway now supports **multi-application, multi-provider architecture**. Different applications (e.g., web apps, mobile apps, e-commerce platforms) can use the same payment gateway service while being configured with different payment providers (Stripe, PayPal, etc.).

## Architecture

### Core Entities

1. **Application** - Represents a client application using the payment gateway
   - Each application has a unique `app_key`
   - Can be of different types: WEB, MOBILE, API, E_COMMERCE, SAAS
   - Belongs to a Merchant

2. **ApplicationProvider** - Maps applications to payment providers
   - Links an Application to one or more PaymentProviders
   - Contains application-specific provider configuration (API keys, credentials)
   - Supports priority-based selection
   - One provider can be marked as default per application

3. **PaymentProvider** - Generic payment provider definition
   - Examples: Stripe, PayPal, Square, etc.
   - Contains global provider settings

### Database Schema

```
merchants
  └── applications (1:N)
       └── application_providers (N:M)
            └── payment_providers

api_keys
  └── application_id (links API key to specific application)

transactions
  └── application_id (tracks which application initiated the transaction)
```

## Use Cases

### Scenario 1: E-commerce Platform with Regional Differences

**Setup:**
- Application A (US Region) → Uses Stripe
- Application B (EU Region) → Uses PayPal
- Application C (Asia Region) → Uses both Stripe and PayPal

```sql
-- Application A configuration
INSERT INTO applications (name, app_type, merchant_id) 
VALUES ('US E-Commerce', 'E_COMMERCE', 1);

INSERT INTO application_providers (application_id, payment_provider_id, is_default, provider_config)
VALUES (1, 1, true, '{"api_key": "stripe_us_key", "webhook_secret": "whsec_us"}');

-- Application B configuration
INSERT INTO applications (name, app_type, merchant_id)
VALUES ('EU E-Commerce', 'E_COMMERCE', 1);

INSERT INTO application_providers (application_id, payment_provider_id, is_default, provider_config)
VALUES (2, 2, true, '{"client_id": "paypal_eu_client", "client_secret": "paypal_eu_secret"}');
```

### Scenario 2: SaaS Platform with Multiple Tenants

Each tenant (customer of your SaaS) gets their own application with their own payment provider credentials:

```
Tenant A → Application 1 → Their Stripe Account
Tenant B → Application 2 → Their PayPal Account
Tenant C → Application 3 → Multiple providers (Stripe + Square)
```

### Scenario 3: Mobile App with Fallback Providers

Mobile application configured with multiple providers for redundancy:

```
Mobile App → Provider 1 (Stripe, Priority 10, Default)
          → Provider 2 (PayPal, Priority 5, Backup)
          → Provider 3 (Square, Priority 1, Last Resort)
```

## API Usage

### 1. Create an Application

```http
POST /api/applications
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "name": "My E-Commerce Store",
  "description": "Online retail store",
  "appType": "E_COMMERCE",
  "webhookUrl": "https://mystore.com/webhooks/payment",
  "callbackUrl": "https://mystore.com/payment/callback",
  "allowedOrigins": "https://mystore.com,https://www.mystore.com"
}
```

**Response:**
```json
{
  "id": 1,
  "name": "My E-Commerce Store",
  "appKey": "app_a1b2c3d4e5f6g7h8i9j0k1l2",
  "appType": "E_COMMERCE",
  "active": true,
  "createdAt": "2025-11-13T10:00:00",
  "providers": []
}
```

### 2. Configure Payment Provider for Application

```http
POST /api/applications/1/providers
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "paymentProviderId": 1,
  "priority": 10,
  "isDefault": true,
  "providerConfig": "{\"api_key\": \"sk_live_xxx\", \"webhook_secret\": \"whsec_xxx\"}",
  "supportedCurrencies": "USD,EUR,GBP",
  "minAmount": 1.00,
  "maxAmount": 10000.00
}
```

**Response:**
```json
{
  "id": 1,
  "applicationId": 1,
  "paymentProviderId": 1,
  "providerName": "STRIPE",
  "providerType": "CARD",
  "priority": 10,
  "isDefault": true,
  "active": true,
  "supportedCurrencies": "USD,EUR,GBP",
  "minAmount": 1.00,
  "maxAmount": 10000.00
}
```

### 3. Process Payment with Application Context

```http
POST /api/payments/process
Authorization: Bearer {jwt_token}
X-App-Key: app_a1b2c3d4e5f6g7h8i9j0k1l2
Content-Type: application/json

{
  "amount": 99.99,
  "currency": "USD",
  "provider": "STRIPE",  // Optional - will use default if not specified
  "paymentMethodToken": "tok_visa",
  "customerEmail": "customer@example.com",
  "customerName": "John Doe",
  "description": "Order #12345"
}
```

**Note:** The service will automatically:
- Validate the provider is configured for the application
- Check currency support for this app-provider combination
- Validate amount limits
- Use application-specific provider credentials

### 4. List All Applications

```http
GET /api/applications/merchant/1
Authorization: Bearer {jwt_token}
```

### 5. Get Application Providers

```http
GET /api/applications/1/providers
Authorization: Bearer {jwt_token}
```

## Implementation Details

### Provider Selection Logic

The `PaymentService.processPaymentForApplication()` method implements intelligent provider selection:

1. **Explicit Provider**: If client specifies a provider, validate it's configured for the application
2. **Default Provider**: If no provider specified, use the application's default provider
3. **Fallback**: (Future enhancement) Try providers in priority order if primary fails

### Security Considerations

1. **API Key Isolation**: Each API key is linked to a specific application
2. **Provider Credentials**: Application-specific provider credentials stored in `provider_config` (should be encrypted)
3. **Origin Validation**: `allowed_origins` field for CORS security
4. **App Key Authentication**: Unique app keys for identifying applications

### Configuration Storage

Provider-specific configuration (API keys, secrets) is stored in `application_providers.provider_config` as JSONB:

```json
{
  "api_key": "sk_live_...",
  "webhook_secret": "whsec_...",
  "account_id": "acct_...",
  "environment": "production"
}
```

## Benefits

1. **Multi-Tenancy**: Support multiple clients with isolated configurations
2. **Flexibility**: Different applications can use different payment providers
3. **Scalability**: Easy to add new applications without code changes
4. **Regional Compliance**: Use region-specific payment providers
5. **Cost Optimization**: Route payments through providers with better rates
6. **Redundancy**: Configure multiple providers for failover
7. **Isolation**: Each application's transactions and configurations are isolated

## Future Enhancements

1. **Smart Routing**: Automatically select provider based on:
   - Currency
   - Amount
   - Customer location
   - Provider availability
   - Transaction costs

2. **Load Balancing**: Distribute transactions across multiple providers

3. **Provider Health Monitoring**: Automatically failover to backup providers

4. **Dynamic Fee Calculation**: Per-application fee structures

5. **Rate Limiting**: Per-application transaction limits

6. **Analytics Dashboard**: Per-application transaction metrics

## Migration Guide

### For Existing Implementations

1. **Create Application**: Create an application entity for your existing service
2. **Link API Keys**: Update existing API keys with `application_id`
3. **Configure Providers**: Set up provider configurations for the application
4. **Update Payment Flow**: Use `processPaymentForApplication()` instead of `processPayment()`

### Example Migration Script

```java
// 1. Create application for existing service
Application app = Application.builder()
    .merchant(existingMerchant)
    .name("Legacy Application")
    .appType("API")
    .appKey(generateAppKey())
    .active(true)
    .build();
applicationRepository.save(app);

// 2. Link existing providers
ApplicationProvider stripeConfig = ApplicationProvider.builder()
    .application(app)
    .paymentProvider(stripeProvider)
    .isDefault(true)
    .priority(10)
    .active(true)
    .providerConfig(currentStripeConfig)
    .build();
applicationProviderRepository.save(stripeConfig);

// 3. Update API keys
existingApiKeys.forEach(apiKey -> {
    apiKey.setApplication(app);
    apiKeyRepository.save(apiKey);
});
```

## Database Migrations

All necessary database migrations are provided in:
- `011-create-applications-table.yaml`
- `012-create-application-providers-table.yaml`
- `013-add-application-id-to-api-keys.yaml`
- `014-add-application-id-to-transactions.yaml`

Run migrations with:
```bash
mvn liquibase:update
```

## Testing

### Unit Test Example

```java
@Test
public void testApplicationSpecificProvider() {
    // Create application
    Application app = createTestApplication();
    
    // Configure Stripe for this application
    ApplicationProvider stripeConfig = configureProvider(app, stripeProvider, true);
    
    // Process payment - should use application's Stripe config
    PaymentRequest request = PaymentRequest.builder()
        .amount(new BigDecimal("100.00"))
        .currency("USD")
        .build();
    
    Transaction tx = paymentService.processPaymentForApplication(request, merchant, app);
    
    assertEquals("COMPLETED", tx.getStatus());
    assertEquals(app.getId(), tx.getApplication().getId());
}
```

## Monitoring and Logging

All payment operations log the application context:

```
Payment processed successfully for application My E-Commerce Store: transaction txn_123456
```

This enables:
- Per-application transaction tracking
- Application-specific error monitoring
- Usage analytics by application
- Audit trails

---

For questions or support, refer to the main README.md or contact the development team.

