# End-to-End (E2E) Tests - API Gateway

## 📋 Overview

Complete End-to-End tests for **Lite Bank Demo** - testing all banking operations through API Gateway with full system integration.

### Test Coverage

| Test Suite | FR Coverage | Tests | Description |
|------------|-------------|-------|-------------|
| `AuthenticationE2ETest` | - | 8 | User registration, login, JWT validation |
| `AccountManagementE2ETest` | FR1 | 10 | Account creation & query |
| `DepositWithdrawalE2ETest` | FR2, FR3, FR8 | 10 | Deposit & withdrawal operations |
| `TransferE2ETest` | FR4, FR7, FR8 | 10 | Same-currency transfers |
| `ExchangeE2ETest` | FR5, FR6, FR8 | 10 | Currency exchange & rates |
| `TransactionHistoryE2ETest` | FR11, FR12, FR15 | 10 | Transaction queries & audit trail |
| `FullUserJourneyE2ETest` | FR1-FR8 | 2 | Complete user journey |

**Total: ~60 End-to-End Tests**

## 🎯 What These Tests Validate

### ✅ All 8 Core Banking Features (FR1-FR8)
- **FR1**: Query all accounts for user
- **FR2**: Deposit money to account
- **FR3**: Withdraw money from account
- **FR4**: Same-currency transfer between accounts
- **FR5**: Cross-currency exchange
- **FR6**: Exchange rate query
- **FR7**: Transfer failure leaves balances unchanged
- **FR8**: Transaction idempotency (using referenceId)

### ✅ Complete System Integration
- JWT authentication & authorization
- API Gateway routing to all microservices
- Distributed tracing (Trace ID propagation)
- Error handling & validation
- Data consistency across services

### ✅ Observability (FR15-FR25)
- Trace ID in all API responses
- Trace ID preservation in transaction records
- Cross-service trace propagation
- Audit trail completeness

## 🚀 How to Run

### Prerequisites

1. **Docker Desktop** running with at least **4GB RAM**
2. **Java 21** installed
3. **Maven 3.9+** installed

### Option 1: Run All E2E Tests (Recommended)

```bash
# From api-gateway directory
cd services/api-gateway

# Run all E2E tests
mvn clean test -Dtest="com.litebank.gateway.e2e.*"
```

### Option 2: Run Specific Test Suite

```bash
# Authentication tests only
mvn test -Dtest=AuthenticationE2ETest

# Transfer tests
mvn test -Dtest=TransferE2ETest

# Complete user journey
mvn test -Dtest=FullUserJourneyE2ETest
```

### Option 3: Run Tests with Docker Compose

```bash
# Start all services first
docker-compose up -d

# Wait for services to be healthy (check with)
docker-compose ps

# Run tests
mvn test -Dtest="com.litebank.gateway.e2e.*"

# Stop services
docker-compose down
```

## ⚙️ Test Architecture

### Testcontainers Integration

Tests use **Testcontainers** to spin up the entire system:

```
┌─────────────────────────────────────────────┐
│         Testcontainers Environment          │
├─────────────────────────────────────────────┤
│ • PostgreSQL (Database)                     │
│ • Kafka (Event Streaming)                   │
│ • All 8 Microservices:                      │
│   - user-service (8080)                     │
│   - account-service (8081)                  │
│   - transaction-service (8082)              │
│   - transfer-service (8083)                 │
│   - exchange-rate-service (8084)            │
│   - exchange-service (8085)                 │
│   - deposit-withdrawal-service (8086)       │
│   - API Gateway (random port)               │
│ • Observability Stack:                      │
│   - Grafana Tempo (Tracing)                 │
│   - Grafana Loki (Logging)                  │
│   - Prometheus (Metrics)                    │
└─────────────────────────────────────────────┘
```

### REST Assured Framework

Tests use **REST Assured** for API testing:

```java
// Example: Testing deposit with JWT auth
RestAssured.given(requestSpec)
    .header("Authorization", "Bearer " + token)
    .body(depositRequest)
    .post("/api/v1/deposits")
    .then()
    .statusCode(201)
    .body("success", equalTo(true))
    .body("traceId", notNullValue());
```

## 📊 Test Execution Flow

### 1. Environment Startup (Testcontainers)
- Starts PostgreSQL and waits for health check
- Starts all microservices sequentially
- Waits for each service health endpoint (`/health`)
- Typically takes **2-3 minutes**

### 2. Test Execution
- Each test class has isolated user data
- Tests run in parallel where possible
- Await async operations completion
- Typically takes **5-10 minutes** for full suite

### 3. Environment Teardown
- Testcontainers automatically cleans up
- Removes all Docker containers and volumes

## 🎭 Key Test Scenarios

### Complete User Journey Test

The `FullUserJourneyE2ETest.testCompleteUserJourney()` simulates a realistic banking scenario:

```
1. Register new user → Login
2. Create USD and EUR accounts
3. Deposit $500 to USD account
4. Withdraw $200 from USD account
5. Create second USD account
6. Transfer $300 between USD accounts
7. Query USD to EUR exchange rate
8. Exchange $200 USD → EUR
9. Query transaction history
10. Verify all trace IDs are present
11. Verify final balances
```

**Validates**: All FR1-FR8 + complete observability

### Transfer Failure Atomicity Test

`TransferE2ETest.testTransferFailureAtomicity()` validates:

```
1. Attempt transfer to invalid account
2. Verify source account balance unchanged
3. Verify error has trace ID for debugging
```

**Validates**: FR7 (balance unchanged on failure)

### Idempotency Test

`DepositWithdrawalE2ETest.testDepositIdempotency()` validates:

```
1. Make deposit with referenceId: "DEP-12345"
2. Retry same deposit with same referenceId
3. Verify balance only increased once
4. Verify second request returns 201 or 409
```

**Validates**: FR8 (Idempotency)

## 🐛 Troubleshooting

### Issue: Testcontainers fails to start

```
Error: Could not find a valid Docker environment
```

**Solution**: Ensure Docker Desktop is running

### Issue: Services fail health check

```
Container startup timed out after 180 seconds
```

**Solution**: Increase Docker memory to 6GB or more

### Issue: Tests fail with connection refused

```
Connection refused: localhost:XXXXX
```

**Solution**: Check if ports are already in use:
```bash
lsof -i :8080-8090
```

### Issue: Tests are slow

**Solution**: Tests take time due to full system startup. This is expected for E2E tests.
- First run: ~5-10 minutes (Docker image pulls)
- Subsequent runs: ~3-5 minutes

## 📈 Expected Results

### ✅ All Tests Passing

```
[INFO] Results:
[INFO]
[INFO] Tests run: 60, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### Trace ID Validation

Every test verifies:
- ✅ All responses contain `X-Trace-Id` header
- ✅ All responses contain `traceId` in JSON body
- ✅ Transaction records preserve original trace IDs
- ✅ Different requests have different trace IDs

## 🎓 Understanding the Tests

### Base Test Class

`BaseE2ETest.java` provides:
- Testcontainers setup for all services
- REST Assured configuration
- Helper methods:
  - `registerAndLogin(username, email, password)` → returns JWT token
  - `createAccount(token, userId, currency)` → returns accountId
  - `getTraceId(response)` → extracts trace ID from response

### Test Pattern Example

```java
@Test
@DisplayName("FR2: Should deposit money successfully")
void testDeposit() {
    // Arrange: Setup test data
    String token = registerAndLogin(...);
    Long accountId = createAccount(...);

    // Act: Execute operation
    Response response = RestAssured.given(requestSpec)
        .header("Authorization", "Bearer " + token)
        .body(depositRequest)
        .post("/api/v1/deposits");

    // Assert: Verify results
    response.then()
        .statusCode(201)
        .body("success", equalTo(true))
        .body("data.balanceAfter", equalTo(1500.00f))
        .body("traceId", notNullValue());

    // Verify: Check side effects
    await().untilAsserted(() -> {
        // Balance updated correctly
    });
}
```

## 🔍 Debugging Failed Tests

### Enable Request/Response Logging

Edit `BaseE2ETest.java`:

```java
RestAssured.enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL);
```

### Check Container Logs

```bash
# While tests are running
docker logs litebank-user-service
docker logs litebank-api-gateway
```

### Inspect Test Report

```bash
# After test run
open target/surefire-reports/index.html
```

## 📚 Next Steps

After E2E tests pass:
1. ✅ **Unit tests for each microservice** - Test individual components
2. ✅ **Integration tests per service** - Test service-level integration
3. ✅ **Performance tests** - Load testing with Gatling
4. ✅ **Chaos Engineering** - Inject failures with Chaos Mesh
5. ✅ **Frontend E2E tests** - Selenium/Playwright for UI

## 🤝 Contributing

When adding new features:
1. Add corresponding E2E test
2. Ensure test follows existing patterns
3. Verify trace ID propagation
4. Test both success and error cases
5. Run full E2E suite before PR

---

**Test Philosophy**: These E2E tests validate the complete system works as expected from a user's perspective, ensuring all microservices integrate correctly through API Gateway with full observability.
