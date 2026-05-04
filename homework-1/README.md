# Homework 1: Banking Transactions API

**Status**: ✅ Complete and Running  
**Technology**: Kotlin + Spring Boot 3.2.3 | OpenAPI 3.0 | Gradle Multi-Module

A REST API for managing banking transactions with OpenAPI-driven development, comprehensive validation, and advanced features.

---

## 🚀 Startup Guide

### Prerequisites

- **Java 17+** - Check with `java -version`
- **Gradle** - Included via wrapper (`./gradlew`)
- **Git** - For version control

### Clone & Setup

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/ai-workshops.git
cd ai-workshops
```

### Run the Application

```bash
# Option 1: Run via Gradle (recommended for development)
./gradlew :homework-1:bootRun

# Option 2: Build and run JAR directly (faster startup)
./gradlew :homework-1:bootJar
java -jar homework-1/build/libs/homework-1-1.0.0.jar
```

**Expected Output:**
```
Started BankingApplication in X.XXX seconds
Tomcat started on port 8080
```

### Verify Installation & Access Swagger UI

Once running, access the interactive API documentation:

#### 🎨 Swagger UI (Interactive API Explorer)
- **Main**: http://localhost:8080/swagger-ui.html

#### 📋 API Specification
- **OpenAPI JSON**: http://localhost:8080/api-docs
- **OpenAPI YAML**: http://localhost:8080/api-docs.yaml
- **Source**: [`openapi-spec/homework-1.yaml`](../openapi-spec/homework-1.yaml)

#### ✅ Health Check
- **Endpoint**: http://localhost:8080/actuator/health
- **Expected**: `{"status":"UP"}`

### Build & Test

```bash
# Build the project (with tests)
./gradlew clean build

# Build without tests (faster)
./gradlew build -x test

# Run tests only
./gradlew test
```

### Project Structure

```
homework-1/
├── build.gradle.kts                      # Module build configuration
├── src/
│   ├── main/
│   │   ├── kotlin/com/banking/
│   │   │   ├── BankingApplication.kt     # Spring Boot entry point
│   │   │   ├── config/
│   │   │   │   └── SwaggerConfig.kt      # OpenAPI/Swagger configuration
│   │   │   ├── controller/
│   │   │   │   └── TransactionController.kt  # REST API endpoints
│   │   │   ├── service/
│   │   │   │   └── TransactionService.kt # Business logic & data management
│   │   │   ├── validator/
│   │   │   │   └── TransactionValidator.kt  # Input validation
│   │   │   └── model/
│   │   │       └── Transaction.kt        # Domain models & enums
│   │   └── resources/
│   │       └── application.yml           # Spring Boot configuration
│   └── test/                             # Unit tests
└── build/
    └── libs/homework-1-1.0.0.jar        # Compiled JAR artifact
```

---

## 🔗 Dependency Architecture Scheme

```
┌─────────────────────────────────────────────────────────────┐
│              openapi-spec/homework-1.yaml                   │
│              (API Contract - Source of Truth)               │
└────────────────────────┬────────────────────────────────────┘
                         │ generates via
                         ↓ openapi-generator plugin
┌─────────────────────────────────────────────────────────────┐
│              :openapi-spec Module (v0.0.1)                  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Generated DTOs & Models (from OpenAPI schemas)        │  │
│  │ - CreateTransactionRequest                            │  │
│  │ - Transaction                                         │  │
│  │ - AccountBalance, AccountSummary, etc.                │  │
│  │ - Enums: TransactionType, TransactionStatus           │  │
│  └───────────────────────────────────────────────────────┘  │
└────────────────┬──────────────────────────────────────────┘
                 │ depends on (project dependency)
                 ↓
┌─────────────────────────────────────────────────────────────┐
│            :homework-1 Module (v1.0.0)                      │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ BankingApplication.kt                                │  │
│  │ TransactionController.kt                             │  │
│  │ TransactionService.kt                                │  │
│  │ TransactionValidator.kt                              │  │
│  │ SwaggerConfig.kt                                     │  │
│  │ Transaction.kt (domain model)                        │  │
│  └───────────────────────────────────────────────────────┘  │
└────────────────┬──────────────────────────────────────────┘
                 │ provides REST API on
                 ↓
          http://localhost:8080/api/v1/*
```

**Benefits of this architecture**:
- ✅ **Separation of Concerns** - Spec generation isolated from business logic
- ✅ **Single Source of Truth** - OpenAPI spec drives all API contracts
- ✅ **Version Independence** - Spec (v0.0.1) and app (v1.0.0) evolve separately
- ✅ **Type Safety** - Generated models with full validation
- ✅ **Auto-Documentation** - Swagger UI auto-generated from spec
- ✅ **Reusability** - :openapi-spec can be used by multiple applications

---

## 📚 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/transactions` | List transactions (with filters) |
| `POST` | `/api/v1/transactions` | Create transaction |
| `GET` | `/api/v1/transactions/{id}` | Get by ID |
| `GET` | `/api/v1/accounts/{accountId}/balance` | Account balance |
| `GET` | `/api/v1/accounts/{accountId}/summary` | Account statistics |
| `GET` | `/api/v1/accounts/{accountId}/interest` | Calculate interest |

## 🔍 Example Requests

```bash
# List all transactions
curl http://localhost:8080/api/v1/transactions

# Filter by type
curl "http://localhost:8080/api/v1/transactions?type=TRANSFER"

# Get balance
curl http://localhost:8080/api/v1/accounts/ACC-12345/balance

# Create transaction
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccount": "ACC-ABC01",
    "toAccount": "ACC-XYZ02",
    "amount": 150.25,
    "currency": "EUR",
    "type": "transfer"
  }'
```

---

## ✅ Validation Rules

| Field | Rule |
|-------|------|
| **Amount** | Positive, max 2 decimal places |
| **Account** | Format `ACC-XXXXX` (5 alphanumeric chars) |
| **Currency** | ISO 4217 code (USD, EUR, GBP, etc.) |
| **Type** | DEPOSIT, WITHDRAWAL, or TRANSFER |

---

## 📊 Data Model

### Transaction
```json
{
  "id": "uuid",
  "fromAccount": "ACC-XXXXX",
  "toAccount": "ACC-XXXXX",
  "amount": 150.25,
  "currency": "USD",
  "type": "transfer|deposit|withdrawal",
  "timestamp": "2026-04-26T14:25:24Z",
  "status": "pending|completed|failed",
  "description": "optional"
}
```

### Account Balance
```json
{
  "accountId": "ACC-XXXXX",
  "balance": 399.50,
  "currency": "USD",
  "lastUpdated": "2026-04-26T14:25:24Z"
}
```

### Account Summary
```json
{
  "accountId": "ACC-XXXXX",
  "totalDeposits": 500.00,
  "totalWithdrawals": 600.50,
  "transactionCount": 5,
  "lastTransactionDate": "2026-04-26T14:15:00Z"
}
```

---

## 📊 Sample Data

Sample transactions are loaded from `transactions.json` at startup:
- 5 pre-populated transactions
- Accounts: ACC-12345, ACC-67890, ACC-11111, ACC-22222, ACC-33333, ACC-44444
- Multiple currencies: USD, EUR, GBP

---

## 📝 Implementation Notes

### Design Decisions

1. **OpenAPI-First**: Spec drives implementation, ensures consistency
2. **Multi-Module**: Separates concerns, enables reusability
3. **In-Memory Storage**: Meets requirements without database complexity
4. **Validation at Service**: Business rules enforced at service layer
5. **Spring Conventions**: Follows Spring Boot best practices


## 📄 Files Overview

| File | Purpose |
|------|---------|
| `openapi-spec/homework-1.yaml` | API specification for HW1 |
| `transactions.json` | Sample data (root) |
| `BankingApplication.kt` | Spring Boot entry point |
| `SwaggerConfig.kt` | OpenAPI/Swagger setup |
| `TransactionController.kt` | REST endpoints |
| `TransactionService.kt` | Business logic & data |
| `TransactionValidator.kt` | Input validation |
| `application.yml` | Spring Boot config |


<div align="center">

Co-Authored-By: Claude Haiku 4.5 <noreply@anthropic.com>

</div>
