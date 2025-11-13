# Quick Start Guide - Multi-Application Payment Gateway

## 🚀 5-Minute Setup

### 1. Apply Database Changes
```bash
mvn liquibase:update
```

### 2. Load Sample Data
```bash
psql -U postgres -d payment_gateway -f sample-multi-app-data.sql
```

### 3. Start the Service
```bash
mvn spring-boot:run
```

## 📞 API Quick Reference

### Create Application
```bash
POST http://localhost:8996/api/applications
Authorization: Bearer {token}

{
  "name": "My App",
  "appType": "E_COMMERCE",
  "webhookUrl": "https://myapp.com/webhook"
}

# Response: app_key = "app_xxxx..."
```

### Configure Provider
```bash
POST http://localhost:8996/api/applications/1/providers
Authorization: Bearer {token}

{
  "paymentProviderId": 1,
  "isDefault": true,
  "priority": 10,
  "providerConfig": "{\"api_key\":\"sk_live_xxx\"}",
  "supportedCurrencies": "USD,EUR"
}
```

### Process Payment
```java
// In your payment controller/service:
Application app = applicationRepository.findByAppKey(appKey);
Transaction tx = paymentService.processPaymentForApplication(
    paymentRequest, 
    merchant, 
    app
);
```

## 🗂️ Database Tables

```
applications              - Your client applications
application_providers     - Provider configs per app
api_keys                 - Now linked to applications
transactions             - Now tracks which app initiated
payment_providers        - Available providers (Stripe, PayPal, etc.)
```

## 🔑 Key Concepts

**Application** = A client system using your gateway (web app, mobile app, etc.)
**Provider** = Payment processor (Stripe, PayPal, Square, etc.)
**ApplicationProvider** = Configuration linking an app to a provider

## 🎯 Common Scenarios

### Scenario A: One App, One Provider
```
Create App → Configure 1 Provider (default) → Process Payments
```

### Scenario B: One App, Multiple Providers (Redundancy)
```
Create App → Configure Provider 1 (priority 10, default)
          → Configure Provider 2 (priority 5, backup)
```

### Scenario C: Multiple Apps, Different Providers
```
Create App A → Configure Stripe
Create App B → Configure PayPal
Create App C → Configure Both
```

## 📊 Sample Data Included

After running `sample-multi-app-data.sql`:
- ✅ 3 providers (Stripe, PayPal, Square)
- ✅ 6 applications with different configurations
- ✅ Ready to test immediately

## 🐛 Troubleshooting

**IDE shows errors but Maven builds successfully?**
→ IntelliJ cache issue: `File → Invalidate Caches → Restart`

**Database migration fails?**
→ Check database connection in `application.yml`

**Can't find application?**
→ Ensure `active=true` or use `findByAppKey()` instead of `findByAppKeyAndActiveTrue()`

## 📚 Full Documentation

- **IMPLEMENTATION_COMPLETED.md** - Complete implementation summary
- **MULTI_APPLICATION_GUIDE.md** - Detailed guide with examples
- **sample-multi-app-data.sql** - Sample data with 6 scenarios

## ✅ Verification Checklist

- [ ] Run `mvn liquibase:update`
- [ ] Load sample data (optional)
- [ ] Start application
- [ ] Create test application via API
- [ ] Configure provider for application
- [ ] Process test payment
- [ ] Check transaction has application_id

---

Need help? Check the full documentation files!

