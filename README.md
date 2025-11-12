# Payment Gateway

A comprehensive payment gateway service built with Java 21, Spring Boot 3.x, and PostgreSQL. This service provides a robust, secure, and scalable solution for processing payments through multiple payment providers (Stripe, PayPal) with JWT and API Key authentication.

## Features

✅ **Complete Maven project** with Java 21 and Spring Boot 3.5.7
✅ **10 database tables** with PostgreSQL for comprehensive payment processing
✅ **Liquibase migrations** in YAML format for database version control
✅ **Complete entities, repositories, services, and controllers**
✅ **Security configuration** with JWT and API Key authentication
✅ **Payment provider framework** with Stripe and PayPal integration
✅ **Multi-application support** with redirect URLs for different client types (WEB, MOBILE, POS)
✅ **Flexible redirect handling** after payment completion
✅ **Docker setup** with docker-compose for easy deployment
✅ **SOLID principles** throughout the codebase

## Table of Contents

- [Architecture](#architecture)
- [Database Schema](#database-schema)
- [Technologies](#technologies)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Security](#security)
- [Payment Providers](#payment-providers)
- [Development](#development)
- [Deployment](#deployment)
- [SOLID Principles](#solid-principles)

## Architecture

The application follows a layered architecture:

```
├── Controller Layer (REST API endpoints)
├── Service Layer (Business logic)
├── Repository Layer (Data access)
├── Entity Layer (Domain models)
├── Security Layer (Authentication & Authorization)
└── Provider Layer (Payment provider integrations)
```

### Key Design Patterns

- **Strategy Pattern**: Payment providers implement a common interface
- **Factory Pattern**: PaymentProviderFactory manages provider instances
- **Repository Pattern**: Spring Data JPA repositories
- **Dependency Injection**: Constructor-based dependency injection

## Database Schema

The application uses 10 PostgreSQL tables:

1. **users** - User accounts with authentication details
2. **api_keys** - API keys for programmatic access
3. **merchants** - Merchant/business information
4. **payment_methods** - Stored payment methods
5. **transactions** - Payment transactions
6. **refunds** - Refund records
7. **webhooks** - Webhook configurations
8. **webhook_events** - Webhook event delivery tracking
9. **payment_providers** - Payment provider configurations
10. **audit_logs** - Audit trail of all operations

### ER Diagram Overview

```
users (1) --> (*) api_keys
users (1) --> (*) merchants
users (1) --> (*) payment_methods
merchants (1) --> (*) transactions
merchants (1) --> (*) webhooks
transactions (1) --> (*) refunds
webhooks (1) --> (*) webhook_events
```

## Technologies

### Core
- **Java 21** - Latest LTS version with modern language features
- **Spring Boot 3.5.7** - Application framework
- **Spring Data JPA** - Data persistence
- **Spring Security** - Authentication and authorization
- **PostgreSQL 16** - Relational database
- **Liquibase** - Database migration tool

### Security
- **JWT (JSON Web Tokens)** - Stateless authentication
- **BCrypt** - Password hashing
- **API Keys** - Alternative authentication method

### Payment Providers
- **Stripe SDK** - Stripe payment integration
- **PayPal SDK** - PayPal payment integration

### DevOps
- **Docker** - Containerization
- **Docker Compose** - Multi-container orchestration
- **Maven** - Build tool

### Utilities
- **Lombok** - Reduce boilerplate code
- **SLF4J/Logback** - Logging

## Getting Started

### Prerequisites

- Java 21 or higher
- Docker and Docker Compose (for containerized deployment)
- Maven 3.9+ (if building locally)
- PostgreSQL 16+ (if running locally without Docker)

### Quick Start with Docker

1. Clone the repository:
```bash
git clone https://github.com/ba-industry4/payment-gateway.git
cd payment-gateway
```

2. Configure environment variables in `docker-compose.yml`:
```yaml
environment:
  STRIPE_API_KEY: your_stripe_api_key
  PAYPAL_CLIENT_ID: your_paypal_client_id
  PAYPAL_CLIENT_SECRET: your_paypal_client_secret
```

3. Start the application:
```bash
docker-compose up -d
```

4. Access the application at `http://localhost:8080`

### Local Development Setup

1. Install Java 21:
```bash
# Ubuntu/Debian
sudo apt install openjdk-21-jdk

# macOS
brew install openjdk@21
```

2. Start PostgreSQL:
```bash
docker run -d \
  --name postgres \
  -e POSTGRES_DB=payment_gateway \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16-alpine
```

3. Build and run the application:
```bash
./mvnw clean package
./mvnw spring-boot:run
```

## Configuration

### Application Configuration

The application is configured via `src/main/resources/application.yml`. Key configurations:

#### Database
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/payment_gateway
    username: postgres
    password: postgres
```

#### JWT
```yaml
security:
  jwt:
    secret-key: your-256-bit-secret
    expiration: 86400000  # 24 hours
    refresh-expiration: 604800000  # 7 days
```

#### Payment Providers
```yaml
payment:
  stripe:
    api-key: sk_test_...
    webhook-secret: whsec_...
  paypal:
    client-id: your_client_id
    client-secret: your_client_secret
    mode: sandbox  # or 'live'
```

### Environment Variables

All sensitive configurations support environment variables:

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET`, `JWT_EXPIRATION`, `JWT_REFRESH_EXPIRATION`
- `STRIPE_API_KEY`, `STRIPE_WEBHOOK_SECRET`
- `PAYPAL_CLIENT_ID`, `PAYPAL_CLIENT_SECRET`, `PAYPAL_MODE`

## API Documentation

### Multi-Application Support

The payment gateway supports different types of client applications with flexible redirect handling:

#### Client Application Types
- **WEB**: Web-based applications (e-commerce sites, web portals)
- **MOBILE**: Mobile applications (iOS, Android apps)
- **POS**: Point-of-sale systems
- **Other**: Custom application types

#### Redirect URL Handling

Each merchant can configure default redirect URLs for their application type:
- **Default Redirect URL**: Generic URL for payment completion
- **Default Callback URL**: URL for payment notifications/webhooks

Payment requests can override these defaults with transaction-specific URLs:
- **redirectUrl**: Generic redirect for any payment completion
- **successUrl**: Specific redirect for successful payments
- **failureUrl**: Specific redirect for failed payments

**Priority Order**: Request URLs > Merchant Default URLs

The payment response includes the appropriate redirect URL with transaction details appended as query parameters:
- `transactionId`: Unique transaction identifier
- `status`: Payment status (COMPLETED, FAILED, PENDING)
- `amount`: Payment amount
- `currency`: Payment currency

**Example Redirect URL**:
```
https://yourapp.com/payment/success?transactionId=550e8400-e29b-41d4-a716-446655440000&status=COMPLETED&amount=100.00&currency=USD
```

### Authentication

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "password"
}

Response:
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "type": "Bearer"
}
```

### Payments

#### Process Payment
```http
POST /api/payments/{merchantId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "amount": 100.00,
  "currency": "USD",
  "provider": "STRIPE",
  "paymentMethodToken": "pm_...",
  "description": "Order #12345",
  "customerEmail": "customer@example.com",
  "customerName": "John Doe",
  "redirectUrl": "https://yourapp.com/payment/callback",
  "successUrl": "https://yourapp.com/payment/success",
  "failureUrl": "https://yourapp.com/payment/failure",
  "metadata": {
    "orderId": "12345"
  }
}

Response:
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "amount": 100.00,
  "currency": "USD",
  "provider": "STRIPE",
  "providerTransactionId": "ch_...",
  "description": "Order #12345",
  "redirectUrl": "https://yourapp.com/payment/success?transactionId=550e8400-e29b-41d4-a716-446655440000&status=COMPLETED&amount=100.00&currency=USD",
  "processedAt": "2025-11-12T18:00:00Z",
  "createdAt": "2025-11-12T17:59:55Z"
}
```

**Redirect URLs**:
- `redirectUrl`: Generic redirect URL for payment completion (optional)
- `successUrl`: Specific URL for successful payments (optional)
- `failureUrl`: Specific URL for failed payments (optional)
- If not provided in the request, merchant's default redirect URLs are used
- The response includes the appropriate redirect URL with transaction details as query parameters

#### Get Transaction
```http
GET /api/payments/{transactionId}
Authorization: Bearer <token>
```

### API Key Authentication

Alternatively, use API keys for authentication:

```http
GET /api/payments/{transactionId}
X-API-Key: your-api-key-here
```

## Security

### Authentication Methods

1. **JWT (JSON Web Tokens)**
   - Stateless authentication
   - Access token (24 hours default)
   - Refresh token (7 days default)
   - Suitable for web and mobile applications

2. **API Keys**
   - Long-lived authentication tokens
   - Suitable for server-to-server communication
   - Can have custom permissions and expiration

### Security Features

- **Password Encryption**: BCrypt with strength 10
- **HTTPS Required**: In production
- **CSRF Protection**: Disabled for stateless API (JWT)
- **CORS**: Configurable
- **Rate Limiting**: Recommended for production
- **Input Validation**: Jakarta Validation annotations

### Authorization

Role-based access control (RBAC):
- `ADMIN` - Full system access
- `MERCHANT` - Merchant operations
- `USER` - Customer operations
- `API_USER` - API key authentication

## Payment Providers

### Stripe Integration

```java
@Component
public class StripePaymentProvider implements PaymentProviderStrategy {
    // Implementation follows Strategy pattern
}
```

**Supported Currencies**: USD, EUR, GBP, CAD, AUD, JPY, CHF, SEK, NOK, DKK

**Features**:
- Payment processing
- Refunds
- Transaction status checking
- Webhook support

### PayPal Integration

```java
@Component
public class PayPalPaymentProvider implements PaymentProviderStrategy {
    // Implementation follows Strategy pattern
}
```

**Supported Currencies**: USD, EUR, GBP, CAD, AUD, JPY

**Features**:
- Payment processing
- Refunds
- Transaction status checking

### Adding New Providers

To add a new payment provider:

1. Implement `PaymentProviderStrategy`:
```java
@Component
public class NewPaymentProvider implements PaymentProviderStrategy {
    @Override
    public String processPayment(...) { }
    
    @Override
    public String processRefund(...) { }
    
    @Override
    public String getTransactionStatus(...) { }
    
    @Override
    public String getProviderName() { return "NEW_PROVIDER"; }
    
    @Override
    public boolean supportsCurrency(String currency) { }
}
```

2. Spring will automatically register it with `PaymentProviderFactory`

## Development

### Project Structure

```
src/
├── main/
│   ├── java/com/ba/payment/gateway/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST controllers
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── entity/          # JPA entities
│   │   ├── exception/       # Custom exceptions
│   │   ├── provider/        # Payment provider implementations
│   │   ├── repository/      # Spring Data repositories
│   │   ├── security/        # Security configuration
│   │   └── service/         # Business logic services
│   └── resources/
│       ├── application.yml  # Application configuration
│       └── db/changelog/    # Liquibase migrations
└── test/
    └── java/                # Unit and integration tests
```

### Building

```bash
# Build without tests
./mvnw clean package -DskipTests

# Build with tests
./mvnw clean package

# Run application
./mvnw spring-boot:run

# Build Docker image
docker build -t payment-gateway:latest .
```

### Testing

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=PaymentServiceTest

# Run with coverage
./mvnw test jacoco:report
```

### Database Migrations

Liquibase automatically runs migrations on startup. To manually manage migrations:

```bash
# Validate migrations
./mvnw liquibase:validate

# Generate SQL
./mvnw liquibase:updateSQL

# Rollback
./mvnw liquibase:rollback -Dliquibase.rollbackCount=1
```

## Deployment

### Docker Deployment

```bash
# Build and start
docker-compose up -d

# View logs
docker-compose logs -f payment-gateway

# Stop
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### Environment-Specific Configuration

Create environment-specific configuration files:

- `application-dev.yml` - Development
- `application-staging.yml` - Staging
- `application-prod.yml` - Production

Run with specific profile:
```bash
java -jar app.jar --spring.profiles.active=prod
```

### Production Considerations

1. **Security**:
   - Use strong JWT secret (256-bit or longer)
   - Enable HTTPS/TLS
   - Configure CORS properly
   - Use production payment provider credentials

2. **Database**:
   - Use connection pooling (HikariCP is default)
   - Enable SSL for database connections
   - Regular backups
   - Database tuning for performance

3. **Monitoring**:
   - Enable Spring Actuator endpoints
   - Integrate with monitoring tools (Prometheus, Grafana)
   - Set up alerting
   - Centralized logging (ELK stack)

4. **Scaling**:
   - Stateless design allows horizontal scaling
   - Use load balancer
   - Consider Redis for session management if needed
   - Database read replicas for read-heavy workloads

## SOLID Principles

This project strictly follows SOLID principles:

### Single Responsibility Principle (S)
Each class has one reason to change:
- `AuthService` - Authentication only
- `PaymentService` - Payment processing only
- `UserRepository` - User data access only

### Open/Closed Principle (O)
Open for extension, closed for modification:
- `PaymentProviderStrategy` interface allows new providers without changing existing code
- Easy to add new payment providers by implementing the interface

### Liskov Substitution Principle (L)
Implementations can be substituted:
- Any `PaymentProviderStrategy` implementation can replace another
- Controllers depend on service interfaces, not implementations

### Interface Segregation Principle (I)
Clients don't depend on unused interfaces:
- `PaymentProviderStrategy` has only essential methods
- Repository interfaces are specific to each entity

### Dependency Inversion Principle (D)
Depend on abstractions:
- `PaymentService` depends on `PaymentProviderStrategy` interface
- `PaymentProviderFactory` manages concrete implementations
- Controllers depend on service interfaces

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License.

## Support

For issues and questions:
- GitHub Issues: [Create an issue](https://github.com/ba-industry4/payment-gateway/issues)
- Email: support@ba-industry4.com

## Roadmap

- [ ] GraphQL API
- [ ] Additional payment providers (Square, Adyen)
- [ ] Subscription management
- [ ] Advanced fraud detection
- [ ] Multi-currency support enhancements
- [ ] Admin dashboard
- [ ] Real-time transaction monitoring
- [ ] Comprehensive webhook system
- [ ] PCI DSS compliance features
