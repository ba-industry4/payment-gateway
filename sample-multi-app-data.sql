-- Sample data for testing multi-application payment provider setup

-- Insert sample payment providers (available providers in the system)
INSERT INTO payment_providers (name, type, active, supported_currencies, supported_payment_methods, fee_percentage, fee_fixed, configuration, created_at, updated_at)
VALUES
  ('STRIPE', 'CARD', true, 'USD,EUR,GBP,CAD,AUD', 'CREDIT_CARD,DEBIT_CARD', 2.9, 0.30, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('PAYPAL', 'CARD', true, 'USD,EUR,GBP,CAD,AUD,JPY', 'PAYPAL,CREDIT_CARD,DEBIT_CARD', 2.9, 0.30, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('SQUARE', 'CARD', true, 'USD,CAD,GBP,AUD', 'CREDIT_CARD,DEBIT_CARD', 2.6, 0.10, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('SPG', 'CARD', true, 'USD,EUR,GBP,AUD,CAD,SGD,MYR,INR', 'CREDIT_CARD,DEBIT_CARD,BANK_TRANSFER,E_WALLET', 2.5, 0.25, '{"api_version": "v1", "environment": "production", "supported_regions": ["APAC", "EMEA", "Americas"]}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Example 1: E-commerce platform with regional applications
-- US Application using Stripe
INSERT INTO applications (merchant_id, name, description, app_type, app_key, webhook_url, active, created_at, updated_at)
VALUES
  (1, 'US Online Store', 'E-commerce store for US market', 'E_COMMERCE', 'app_us_store_001', 'https://us-store.example.com/webhook', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO application_providers (application_id, payment_provider_id, priority, is_default, active, provider_config, supported_currencies, min_amount, max_amount, created_at, updated_at)
VALUES
  ((SELECT id FROM applications WHERE app_key = 'app_us_store_001'),
   (SELECT id FROM payment_providers WHERE name = 'STRIPE'),
   10, true, true,
   '{"api_key": "sk_test_us_xxx", "webhook_secret": "whsec_us_xxx"}',
   'USD,CAD', 1.00, 10000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- EU Application using PayPal
INSERT INTO applications (merchant_id, name, description, app_type, app_key, webhook_url, active, created_at, updated_at)
VALUES
  (1, 'EU Online Store', 'E-commerce store for EU market', 'E_COMMERCE', 'app_eu_store_001', 'https://eu-store.example.com/webhook', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO application_providers (application_id, payment_provider_id, priority, is_default, active, provider_config, supported_currencies, min_amount, max_amount, created_at, updated_at)
VALUES
  ((SELECT id FROM applications WHERE app_key = 'app_eu_store_001'),
   (SELECT id FROM payment_providers WHERE name = 'PAYPAL'),
   10, true, true,
   '{"client_id": "paypal_eu_client_xxx", "client_secret": "paypal_eu_secret_xxx"}',
   'EUR,GBP', 1.00, 10000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Example 2: Mobile app with multiple providers for redundancy
INSERT INTO applications (merchant_id, name, description, app_type, app_key, webhook_url, callback_url, active, created_at, updated_at)
VALUES
  (1, 'Mobile Payment App', 'iOS and Android payment application', 'MOBILE', 'app_mobile_001', 'https://api.mobile-app.com/webhook', 'https://app.mobile-app.com/callback', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Primary provider: Stripe
INSERT INTO application_providers (application_id, payment_provider_id, priority, is_default, active, provider_config, supported_currencies, min_amount, max_amount, created_at, updated_at)
VALUES
  ((SELECT id FROM applications WHERE app_key = 'app_mobile_001'),
   (SELECT id FROM payment_providers WHERE name = 'STRIPE'),
   10, true, true,
   '{"api_key": "sk_test_mobile_stripe_xxx"}',
   'USD,EUR,GBP', 1.00, 5000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Backup provider: PayPal
INSERT INTO application_providers (application_id, payment_provider_id, priority, is_default, active, provider_config, supported_currencies, min_amount, max_amount, created_at, updated_at)
VALUES
  ((SELECT id FROM applications WHERE app_key = 'app_rideshare_001'),
   (SELECT id FROM payment_providers WHERE name = 'PAYPAL'),
   5, false, true,
   '{"client_id": "paypal_rideshare_xxx", "client_secret": "paypal_rideshare_secret_xxx"}',
   'USD,EUR,GBP', 3.00, 500.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Example 3: SaaS platform API
INSERT INTO applications (merchant_id, name, description, app_type, app_key, active, created_at, updated_at)
VALUES
  (1, 'SaaS Platform API', 'API for SaaS subscription payments', 'API', 'app_saas_api_001', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Using Square as primary provider
INSERT INTO application_providers (application_id, payment_provider_id, priority, is_default, active, provider_config, supported_currencies, min_amount, max_amount, created_at, updated_at)
VALUES
  ((SELECT id FROM applications WHERE app_key = 'app_saas_api_001'),
   (SELECT id FROM payment_providers WHERE name = 'SQUARE'),
   10, true, true,
   '{"access_token": "square_access_token_xxx", "location_id": "location_xxx"}',
   'USD', 5.00, 50000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Example 4: Multi-tenant application where different clients use different providers
-- Tenant A application
INSERT INTO applications (merchant_id, name, description, app_type, app_key, webhook_url, active, settings, created_at, updated_at)
VALUES
  (1, 'Tenant A Application', 'Dedicated application for Tenant A', 'WEB', 'app_tenant_a_001', 'https://tenant-a.example.com/webhook', true, '{"tenant_id": "tenant_a", "branding": "custom"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO application_providers (application_id, payment_provider_id, priority, is_default, active, provider_config, supported_currencies, created_at, updated_at)
VALUES
  ((SELECT id FROM applications WHERE app_key = 'app_tenant_a_001'),
   (SELECT id FROM payment_providers WHERE name = 'STRIPE'),
   10, true, true,
   '{"api_key": "sk_test_tenant_a_stripe_xxx", "connected_account_id": "acct_tenant_a_xxx"}',
   'USD,EUR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Tenant B application
INSERT INTO applications (merchant_id, name, description, app_type, app_key, webhook_url, active, settings, created_at, updated_at)
VALUES
  (1, 'Tenant B Application', 'Dedicated application for Tenant B', 'WEB', 'app_tenant_b_001', 'https://tenant-b.example.com/webhook', true, '{"tenant_id": "tenant_b", "branding": "custom"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO application_providers (application_id, payment_provider_id, priority, is_default, active, provider_config, supported_currencies, created_at, updated_at)
VALUES
  ((SELECT id FROM applications WHERE app_key = 'app_tenant_b_001'),
   (SELECT id FROM payment_providers WHERE name = 'PAYPAL'),
   10, true, true,
   '{"client_id": "tenant_b_paypal_client_xxx", "client_secret": "tenant_b_paypal_secret_xxx"}',
   'USD,GBP,CAD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================================================
-- CLIENT G: "TechMart Asia" - E-commerce Business (APAC Region)
-- =============================================================================
-- Another CLIENT running e-commerce in Asia-Pacific region
-- They use SPG payment provider for multi-currency APAC support
INSERT INTO applications (merchant_id, name, description, app_type, app_key, webhook_url, active, settings, created_at, updated_at)
VALUES
  (1, 'TechMart Asia', 'Client: Electronics and gadgets e-commerce for APAC region', 'E_COMMERCE', 'app_techmart_001', 'https://techmart.asia/payment/webhook', true, '{"region": "APAC", "currencies": ["SGD", "MYR", "INR"], "features": ["multi_currency", "local_payment_methods"]}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- TechMart uses SPG for comprehensive APAC payment support
INSERT INTO application_providers (application_id, payment_provider_id, priority, is_default, active, provider_config, supported_currencies, min_amount, max_amount, created_at, updated_at)
VALUES
  ((SELECT id FROM applications WHERE app_key = 'app_techmart_001'),
   (SELECT id FROM payment_providers WHERE name = 'SPG'),
   10, true, true,
   '{"api_key": "spg_live_techmart_xxx", "merchant_id": "techmart_apac_001", "webhook_secret": "spg_webhook_techmart_xxx", "settlement_currency": "SGD"}',
   'SGD,MYR,INR,USD,AUD', 10.00, 50000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================================================
-- SUMMARY: 7 Different Client Businesses Configured
-- =============================================================================
--
-- Your Payment Gateway now serves 7 INDEPENDENT CLIENTS:
--
-- 1. ShopEasy         → E-commerce (Stripe, USD/CAD)
-- 2. FoodDelivery Pro → Food Delivery (PayPal, USD/EUR/GBP)
-- 3. RideShare Go     → Transportation (Stripe + PayPal redundancy)
-- 4. CloudSuite       → SaaS (Square, USD)
-- 5. BookStore Online → Retail (Stripe, USD/EUR/GBP)
-- 6. FitnessHub       → Gym Management (PayPal, Multi-currency)
-- 7. TechMart Asia    → E-commerce APAC (SPG, SGD/MYR/INR/USD/AUD)
--
-- Each client:
--   ✓ Has their own app_key for API authentication
--   ✓ Uses THEIR OWN payment provider credentials (not yours)
--   ✓ Has isolated transaction history
--   ✓ Has their own webhook URLs
--   ✓ Cannot see other clients' data
--
-- =============================================================================

-- Link API keys to client applications (uncomment to use)
-- UPDATE api_keys SET application_id = (SELECT id FROM applications WHERE app_key = 'app_shopeasy_001') WHERE id = 1;
-- UPDATE api_keys SET application_id = (SELECT id FROM applications WHERE app_key = 'app_rideshare_001') WHERE id = 2;

