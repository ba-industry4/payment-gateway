# Architecture Diagram - Multi-Application Payment Gateway

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         PAYMENT GATEWAY SERVICE                       │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                ┌───────────────────┼───────────────────┐
                │                   │                   │
         ┌──────▼──────┐    ┌──────▼──────┐    ┌──────▼──────┐
         │ Application │    │ Application │    │ Application │
         │      A      │    │      B      │    │      C      │
         │  (Web App)  │    │ (Mobile App)│    │  (API SaaS) │
         └──────┬──────┘    └──────┬──────┘    └──────┬──────┘
                │                   │                   │
         app_key_aaa           app_key_bbb         app_key_ccc
                │                   │                   │
    ┌───────────┴──────┐   ┌────────┴────────┐   ┌────┴─────────┐
    │                  │   │                 │   │              │
┌───▼────┐      ┌──────▼───▼──┐      ┌──────▼───▼──┐     ┌─────▼────┐
│ Stripe │      │   PayPal    │      │   Square    │     │  Stripe  │
│ (USD)  │      │ (EUR, GBP)  │      │    (USD)    │     │ (Global) │
└────────┘      └─────────────┘      └─────────────┘     └──────────┘
Default         Priority: 10          Default             Priority: 5
                Backup: Stripe                            Backup: PayPal
```

## Database Relationships

```
┌─────────────┐
│   Merchant  │
└──────┬──────┘
       │ 1:N
       │
┌──────▼──────────┐
│  Application    │ (Web, Mobile, API, E-commerce, SaaS)
│  - app_key      │
│  - webhook_url  │
│  - callback_url │
└──────┬──────────┘
       │ 1:N
       │
┌──────▼────────────────────┐          ┌──────────────────┐
│  ApplicationProvider      │  N:1     │ PaymentProvider  │
│  - priority               │◄─────────┤ - Stripe         │
│  - is_default             │          │ - PayPal         │
│  - provider_config (JSON) │          │ - Square         │
│  - supported_currencies   │          │ - etc.           │
│  - min_amount / max_amount│          └──────────────────┘
└───────────────────────────┘
       │
       │ Used by
       │
┌──────▼──────────┐
│   Transaction   │
│  - amount       │
│  - currency     │
│  - status       │
│  - provider     │
└─────────────────┘
```

## Request Flow

```
1. CLIENT REQUEST
   ↓
   HTTP POST /api/payments
   Headers: 
     - Authorization: Bearer {JWT}
     - X-App-Key: app_xxx
   Body: {amount, currency, ...}

2. AUTHENTICATION
   ↓
   Validate JWT → Get User → Get Merchant

3. APPLICATION LOOKUP
   ↓
   Find Application by app_key
   Verify: application.active = true
   Verify: application.merchant = merchant

4. PROVIDER SELECTION
   ↓
   If request.provider specified:
     → Get ApplicationProvider by name
   Else:
     → Get default ApplicationProvider

5. VALIDATION
   ↓
   Check currency in supported_currencies
   Check amount >= min_amount
   Check amount <= max_amount

6. PAYMENT PROCESSING
   ↓
   Load provider_config (API keys, secrets)
   Call provider API (Stripe, PayPal, etc.)
   
7. TRANSACTION RECORDING
   ↓
   Save Transaction with:
     - application_id
     - merchant_id
     - provider
     - status
     - amount, currency, etc.

8. RESPONSE
   ↓
   Return Transaction details to client
```

## Multi-Tenant Scenarios

### Scenario 1: Regional Applications
```
┌─────────────────────────────────────────────────────────────┐
│                      Merchant: Global Corp                   │
└──────────────────┬───────────────────┬──────────────────────┘
                   │                   │
         ┌─────────▼────────┐  ┌───────▼──────────┐
         │  US Application  │  │  EU Application  │
         │  app_us_001      │  │  app_eu_001      │
         └─────────┬────────┘  └───────┬──────────┘
                   │                   │
              ┌────▼────┐         ┌────▼────┐
              │ Stripe  │         │ PayPal  │
              │  (USD)  │         │(EUR,GBP)│
              └─────────┘         └─────────┘
```

### Scenario 2: Multi-Tenant SaaS
```
┌────────────────────────────────────────────────────────────┐
│                  Merchant: SaaS Platform                    │
└────┬──────────────────┬──────────────────┬─────────────────┘
     │                  │                  │
┌────▼────┐      ┌──────▼────┐     ┌──────▼────┐
│Tenant A │      │ Tenant B  │     │ Tenant C  │
│App      │      │ App       │     │ App       │
└────┬────┘      └──────┬────┘     └──────┬────┘
     │                  │                  │
┌────▼────┐      ┌──────▼────┐     ┌──────▼────┐┌──────────┐
│Stripe   │      │PayPal     │     │Stripe     ││PayPal    │
│(Tenant A│      │(Tenant B  │     │(Priority  ││(Priority │
│Account) │      │Account)   │     │10)        ││5, Backup)│
└─────────┘      └───────────┘     └───────────┘└──────────┘
```

### Scenario 3: Failover Configuration
```
┌──────────────────────┐
│   Mobile App         │
│   app_mobile_001     │
└──────────┬───────────┘
           │
    ┌──────┴──────┬─────────────┬──────────────┐
    │             │             │              │
┌───▼────┐  ┌─────▼───┐  ┌──────▼────┐  ┌─────▼────┐
│Stripe  │  │PayPal   │  │Square     │  │Local     │
│Pri: 10 │  │Pri: 8   │  │Pri: 5     │  │Pri: 1    │
│Default │  │Active   │  │Active     │  │Fallback  │
└────────┘  └─────────┘  └───────────┘  └──────────┘
    ↓
  Try first
    ↓
  If fails → Try PayPal
    ↓
  If fails → Try Square
    ↓
  If fails → Try Local
```

## Configuration Example

```yaml
Application: "E-Commerce Store"
  ├── app_key: "app_ecommerce_001"
  ├── app_type: "E_COMMERCE"
  ├── webhook_url: "https://store.com/webhook"
  ├── active: true
  └── Providers:
      ├── Stripe (Default, Priority 10)
      │   ├── providerConfig: {"api_key": "sk_live_xxx"}
      │   ├── supportedCurrencies: "USD,CAD"
      │   ├── minAmount: 1.00
      │   └── maxAmount: 10000.00
      └── PayPal (Backup, Priority 5)
          ├── providerConfig: {"client_id": "xxx", "secret": "yyy"}
          ├── supportedCurrencies: "USD,EUR,GBP"
          ├── minAmount: 5.00
          └── maxAmount: 5000.00
```

## Security Model

```
┌──────────────┐
│   Client     │
└──────┬───────┘
       │
       │ 1. JWT Authentication
       ▼
┌──────────────┐
│ API Gateway  │
└──────┬───────┘
       │
       │ 2. App Key Validation
       ▼
┌────────────────────┐
│  Application       │ ← Belongs to Merchant
│  (app_key verified)│
└──────┬─────────────┘
       │
       │ 3. Provider Config Lookup
       ▼
┌────────────────────────┐
│ ApplicationProvider    │ ← Application-specific credentials
│ (Encrypted API keys)   │
└──────┬─────────────────┘
       │
       │ 4. Payment Processing
       ▼
┌────────────────────┐
│ Payment Provider   │ (Stripe, PayPal, etc.)
│ External API       │
└────────────────────┘
```

## Benefits Visualization

```
Traditional Single-Provider Setup:
┌──────────────────┐
│  All Clients     │
└────────┬─────────┘
         │
    ┌────▼────┐
    │ Stripe  │
    │  Only   │
    └─────────┘

Multi-Application Multi-Provider Setup:
┌────────┬─────────┬─────────┬─────────┐
│Client A│Client B │Client C │Client D │
└────┬───┴────┬────┴────┬────┴────┬────┘
     │        │         │         │
┌────▼──┐ ┌───▼───┐ ┌───▼───┐ ┌──▼────┐
│Stripe │ │PayPal │ │Square │ │Custom │
│(USD)  │ │(EUR)  │ │(Global)│ │Local  │
└───────┘ └───────┘ └───────┘ └───────┘

✅ Flexibility      ✅ Redundancy
✅ Cost Optimization ✅ Regional Compliance
✅ Multi-Tenancy    ✅ Scalability
```

---

This architecture supports:
- ✅ Unlimited applications per merchant
- ✅ Multiple providers per application
- ✅ Provider failover/redundancy
- ✅ Application-specific configurations
- ✅ Isolated transaction tracking
- ✅ Flexible routing rules

