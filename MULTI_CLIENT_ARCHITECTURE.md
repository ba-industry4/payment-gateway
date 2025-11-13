# Multi-Client Payment Gateway - Client Isolation Architecture

## 🎯 Understanding the Architecture

### What is an "Application"?

In this payment gateway, **Application = Client Business** using your service.

```
Your Payment Gateway Service
  └── Merchant: "Your Company"
       ├── Application A = "ABC E-commerce" (Client 1) → Uses Stripe
       ├── Application B = "XYZ Retail" (Client 2) → Uses PayPal
       ├── Application C = "123 SaaS" (Client 3) → Uses Square
       └── Application D = "456 Marketplace" (Client 4) → Uses Multiple
```

Each **Application** represents:
- ✅ A different client/customer of your payment gateway
- ✅ A completely isolated business entity
- ✅ Independent payment provider configuration
- ✅ Separate API keys and credentials
- ✅ Their own transaction history

## 🏢 Real-World Example

### Your Payment Gateway Service = "PaymentGo Inc."

You have 4 clients (different businesses):

```
┌─────────────────────────────────────────────────────────────────┐
│              PaymentGo Inc. (Your Payment Gateway)               │
│                    Merchant ID: 1                                │
└─────────────────────────────────────────────────────────────────┘
                              │
            ┌─────────────────┼─────────────────┬────────────────┐
            │                 │                 │                │
    ┌───────▼──────┐  ┌───────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐
    │  Client A    │  │  Client B    │  │  Client C   │  │  Client D   │
    │ "ShopEasy"   │  │ "FoodDelivery│  │ "RidShare"  │  │ "BookStore" │
    │ (E-commerce) │  │ (Food App)"  │  │ (Transport)"│  │ (Retail)"   │
    └───────┬──────┘  └───────┬──────┘  └──────┬──────┘  └──────┬──────┘
            │                 │                 │                │
      app_shopeasy_01   app_fooddel_01    app_rideshare_01  app_bookst_01
            │                 │                 │                │
    ┌───────▼──────┐  ┌───────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐
    │   Stripe     │  │   PayPal     │  │   Square    │  │  Stripe +   │
    │   (USD)      │  │   (Multiple) │  │   (Global)  │  │  PayPal     │
    │ Their Keys   │  │ Their Keys   │  │ Their Keys  │  │(Multi-prov) │
    └──────────────┘  └──────────────┘  └─────────────┘  └─────────────┘
```

### Client Details

**Client A: ShopEasy (E-commerce Store)**
- Application Name: "ShopEasy Store"
- App Key: `app_shopeasy_2a3b4c5d`
- Provider: Stripe
- Currencies: USD, CAD
- Their Stripe API Key: `sk_live_shopeasy_xxx`

**Client B: FoodDelivery (Food Delivery App)**
- Application Name: "FoodDelivery Platform"
- App Key: `app_fooddel_3c4d5e6f`
- Provider: PayPal
- Currencies: USD, EUR, GBP
- Their PayPal Client ID: `paypal_fooddel_xxx`

**Client C: RideShare (Transportation)**
- Application Name: "RideShare Service"
- App Key: `app_rideshare_4d5e6f7g`
- Provider: Square
- Currencies: USD
- Their Square Access Token: `square_rideshare_xxx`

**Client D: BookStore (Online Retail)**
- Application Name: "BookStore Online"
- App Key: `app_bookst_5e6f7g8h`
- Providers: Stripe (Primary) + PayPal (Backup)
- Currencies: USD, EUR
- Multiple provider configurations

## 🔑 How Client Isolation Works

### 1. Onboarding a New Client

When "ShopEasy" signs up for your payment gateway:

```sql
-- Step 1: Create Application (Client)
INSERT INTO applications (
    merchant_id,  -- Your payment gateway company
    name,
    app_type,
    app_key,
    webhook_url,
    active
) VALUES (
    1,  -- Your merchant ID
    'ShopEasy Store',
    'E_COMMERCE',
    'app_shopeasy_2a3b4c5d',
    'https://shopeasy.com/payment/webhook',
    true
);

-- Step 2: Configure their Payment Provider
INSERT INTO application_providers (
    application_id,
    payment_provider_id,  -- Stripe
    is_default,
    provider_config,  -- ShopEasy's Stripe API keys
    supported_currencies,
    min_amount,
    max_amount
) VALUES (
    (SELECT id FROM applications WHERE app_key = 'app_shopeasy_2a3b4c5d'),
    1,  -- Stripe provider
    true,
    '{"api_key": "sk_live_shopeasy_xxx", "webhook_secret": "whsec_shopeasy"}',
    'USD,CAD',
    5.00,
    50000.00
);

-- Step 3: Create API Key for ShopEasy
INSERT INTO api_keys (
    user_id,
    application_id,  -- Link to ShopEasy's application
    key_value,
    name,
    active
) VALUES (
    1,
    (SELECT id FROM applications WHERE app_key = 'app_shopeasy_2a3b4c5d'),
    'sk_test_shopeasy_apikey_xxx',
    'ShopEasy Production Key',
    true
);
```

### 2. Client Makes a Payment

When ShopEasy processes a payment:

```http
POST /api/payments/process
Authorization: Bearer {shopeasy_jwt}
X-App-Key: app_shopeasy_2a3b4c5d

{
  "amount": 99.99,
  "currency": "USD",
  "customerEmail": "customer@example.com"
}
```

**What happens:**
1. ✅ System validates JWT token
2. ✅ System looks up Application by `app_key`
3. ✅ System retrieves ShopEasy's provider configuration (Stripe)
4. ✅ System uses **ShopEasy's Stripe API keys** (not yours!)
5. ✅ Transaction is recorded with `application_id` = ShopEasy's ID
6. ✅ ShopEasy receives webhook callback

### 3. Complete Isolation

**Client A (ShopEasy):**
- Cannot see Client B's transactions
- Cannot use Client B's provider credentials
- Has their own API keys
- Has their own webhook URLs
- Has their own provider configuration

**Client B (FoodDelivery):**
- Cannot see Client A's transactions
- Cannot use Client A's provider credentials
- Has their own API keys
- Has their own webhook URLs
- Has their own provider configuration

## 📊 Database Structure for Multi-Client

```sql
-- Your Payment Gateway Company
merchants
  id: 1
  business_name: "PaymentGo Inc."

-- Your Clients (as Applications)
applications
  id: 1, merchant_id: 1, name: "ShopEasy", app_key: "app_shopeasy_xxx"
  id: 2, merchant_id: 1, name: "FoodDelivery", app_key: "app_fooddel_xxx"
  id: 3, merchant_id: 1, name: "RideShare", app_key: "app_rideshare_xxx"
  id: 4, merchant_id: 1, name: "BookStore", app_key: "app_bookst_xxx"

-- Each Client's Provider Configuration
application_providers
  id: 1, app_id: 1, provider: Stripe, config: "{ShopEasy's Stripe keys}"
  id: 2, app_id: 2, provider: PayPal, config: "{FoodDelivery's PayPal keys}"
  id: 3, app_id: 3, provider: Square, config: "{RideShare's Square keys}"
  id: 4, app_id: 4, provider: Stripe, config: "{BookStore's Stripe keys}"
  id: 5, app_id: 4, provider: PayPal, config: "{BookStore's PayPal keys}"

-- Each Client's Transactions
transactions
  id: 1, application_id: 1 (ShopEasy), amount: 99.99, ...
  id: 2, application_id: 2 (FoodDelivery), amount: 25.50, ...
  id: 3, application_id: 1 (ShopEasy), amount: 149.00, ...
  id: 4, application_id: 3 (RideShare), amount: 15.75, ...
```

## 🔐 Security & Isolation

### 1. API Key Isolation
```java
// API key is linked to specific application
ApiKey apiKey = apiKeyRepository.findByKeyValue(key);
Application clientApp = apiKey.getApplication();

// This client can ONLY process payments for their application
// They CANNOT process payments for other clients
```

### 2. Provider Credential Isolation
```java
// Each client's provider credentials are stored separately
ApplicationProvider config = applicationProviderRepository
    .findByApplicationAndIsDefaultTrue(clientApp);

String clientStripeKey = config.getProviderConfig();
// This is ShopEasy's Stripe key, NOT FoodDelivery's or yours
```

### 3. Transaction Isolation
```sql
-- Client A can only query THEIR transactions
SELECT * FROM transactions 
WHERE application_id = (
    SELECT id FROM applications WHERE app_key = 'app_shopeasy_xxx'
);

-- They CANNOT see other clients' transactions
```

## 🚀 API Endpoints for Client Management

### Onboard New Client
```http
POST /api/applications
Authorization: Bearer {admin_token}

{
  "name": "New Client Business",
  "appType": "E_COMMERCE",
  "webhookUrl": "https://newclient.com/webhook"
}

Response:
{
  "id": 5,
  "name": "New Client Business",
  "appKey": "app_newclient_7g8h9i0j",  ← Give this to client
  "active": true
}
```

### Configure Client's Payment Provider
```http
POST /api/applications/5/providers
Authorization: Bearer {admin_token}

{
  "paymentProviderId": 1,  // Stripe
  "isDefault": true,
  "providerConfig": "{\"api_key\": \"sk_live_newclient_xxx\"}",  ← Client's keys
  "supportedCurrencies": "USD,EUR"
}
```

### Client Processes Payment
```http
POST /api/payments/process
Authorization: Bearer {client_jwt}
X-App-Key: app_newclient_7g8h9i0j  ← Client uses their app key

{
  "amount": 199.99,
  "currency": "USD",
  "customerEmail": "buyer@example.com"
}
```

## 📈 Scalability

With this architecture:
- ✅ Support **unlimited clients** (applications)
- ✅ Each client has **isolated configuration**
- ✅ Each client uses **their own provider credentials**
- ✅ Add new clients **without code changes**
- ✅ Each client has **separate transaction tracking**
- ✅ Each client can have **different providers**
- ✅ Complete **multi-tenancy** support

## 🎯 Benefits for Your Business

### For You (Payment Gateway Provider)
- Onboard clients quickly
- Each client brings their own provider credentials
- You don't need to manage provider accounts
- Clear transaction attribution per client
- Easy to add new clients

### For Your Clients
- Use their own payment provider accounts
- Their transactions are isolated
- Custom provider configuration
- Their own API keys
- Their own webhook URLs

## 💡 Common Patterns

### Pattern 1: Client Brings Their Own Stripe Account
```
Client: "I have a Stripe account with good rates"
You: "Great! Give us your API key"
Result: Client uses THEIR Stripe account, not yours
```

### Pattern 2: Client Wants Multiple Providers
```
Client: "I want Stripe primary, PayPal backup"
You: Configure both providers with priorities
Result: Client has redundancy
```

### Pattern 3: Client Has Regional Requirements
```
Client: "I need PayPal for EU, Stripe for US"
You: Create two applications or one with both providers
Result: Client can route based on region
```

## 📋 Quick Setup for New Client

```bash
# 1. Create application (client)
curl -X POST http://localhost:8996/api/applications \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -d '{"name": "NewClient", "appType": "E_COMMERCE"}'

# 2. Get app_key from response (e.g., app_newclient_xxx)

# 3. Configure their provider
curl -X POST http://localhost:8996/api/applications/ID/providers \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -d '{
    "paymentProviderId": 1,
    "providerConfig": "{\"api_key\": \"CLIENT_STRIPE_KEY\"}",
    "isDefault": true
  }'

# 4. Give app_key to client
# Client uses this in all API requests: X-App-Key: app_newclient_xxx
```

---

## Summary

✅ **Application = Client Business** (not your internal apps)  
✅ **Complete isolation** between clients  
✅ **Each client uses their own** provider credentials  
✅ **Multi-tenancy** fully supported  
✅ **Scalable** - add unlimited clients  
✅ **Secure** - transactions and configs isolated  

This is a **true multi-client payment gateway architecture**! 🎉

