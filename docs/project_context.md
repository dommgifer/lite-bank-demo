---
project_name: 'lite-bank-demo'
created_at: '2025-12-09'
last_updated: '2025-12-09'
sections_completed: ['technology-stack', 'language-specific-rules', 'framework-specific-rules', 'critical-anti-patterns', 'code-quality-style-rules', 'development-workflow-rules']
---

# Project Context for AI Agents

_This document contains critical implementation rules that AI agents MUST follow when writing code for lite-bank-demo. Focus on unobvious details that agents might miss._

---

## Technology Stack & Versions

### Backend Technologies

**Java Services (Spring Boot):**
- **Spring Boot**: 3.4.x
- **Java Version**: 17 (minimum required)
- **Build Tool**: Maven (pom.xml, NOT Gradle)
- **Maven Wrapper**: Enabled (mvnw/mvnw.cmd)
- **Services**: User Service, Transaction Service, Deposit-Withdrawal Service, Audit Service

**Java Services (Quarkus):**
- **Quarkus**: 3.15.x LTS (Long Term Support)
- **Java Version**: 17 (minimum required)
- **Build Tool**: Maven (pom.xml)
- **Services**: Account Service, Currency Exchange Service

**API Gateway:**
- **Spring Cloud Gateway**: 3.1.x (Phase 1 必備)
- **Java Version**: 17
- **Build Tool**: Maven

**Python Services:**
- **Python**: 3.11
- **Framework**: FastAPI ^0.115.0
- **Package Manager**: pip + requirements.txt (NOT Poetry, NOT pyproject.toml)
- **Dev Dependencies**: requirements-dev.txt (pytest, black, mypy, etc.)
- **Service**: Transfer Service (SAGA Orchestrator)

### Frontend Technologies

**Core Stack:**
- **Node.js**: 20.x LTS (Iron, support until April 2026)
- **React**: 18 (required for MUI v5 compatibility)
- **TypeScript**: >= 5.0.4 (建議 5.7.x for OpenTelemetry SDK 2.x support)
- **Build Tool**: Vite 5.x (NOT webpack)

**UI Libraries:**
- **@mui/material**: ^5.15.0 (CRITICAL: MUI v5 ONLY, NOT v4 - breaking changes)
- **@mui/icons-material**: ^5.15.0
- **@emotion/react**: ^11.11.0 (MUI v5 dependency)
- **@emotion/styled**: ^11.11.0 (MUI v5 dependency)

**Routing & Communication:**
- **React Router**: 6.x
- **Socket.io-client**: 4.x (WebSocket connections to Notification Service)

### Data & Messaging

**Database:**
- **PostgreSQL**: (single instance, Shared Schema strategy)
- **Migration Tool**: Flyway 10.x (SQL-based migrations)
- **Migration Location**: `src/main/resources/db/migration/` (Java) or `db/migration/` (Python)
- **Naming Convention**: `V{version}__{description}.sql` (e.g., `V1__create_accounts_table.sql`)

**Messaging:**
- **Kafka**: 4.0+ **KRaft mode** (NO ZooKeeper - KRaft is mandatory, minimum 3.3.1+)
- **Topic Naming**: PascalCase + past tense (e.g., `TransferCompleted`, `NotificationSent`)

### Infrastructure

**Container Base Images:**
- **Java Services**: `eclipse-temurin:17-jre` (standard version, NOT alpine - better compatibility)
- **Python Service**: `python:3.11-slim` (slim version, NOT alpine - balance size and compatibility)
- **Frontend**: `nginx:1.25-alpine` (alpine for small size)
- **Multi-stage Builds**: Required for all services

**Orchestration:**
- **Kubernetes**: (4 namespaces: lite-bank-services, lite-bank-infra, observability, chaos-mesh)
- **Deployment Tool**: Helm Charts (NOT Kustomize)

### Observability

**OpenTelemetry (Manual SDK Implementation - NOT auto-instrumentation):**

**Java:**
- **opentelemetry-api**: 1.57.0
- **opentelemetry-sdk**: 1.57.0
- **Implementation**: Manual span creation with business attributes
- **Constant Class**: `OTelAttributes.java` (centralized span attribute names)

**Python:**
- **opentelemetry-api**: 1.39.0
- **opentelemetry-sdk**: 1.39.0
- **Implementation**: Manual span creation with business attributes
- **Constant Module**: `otel_attributes.py` (centralized span attribute names)

**JavaScript/React:**
- **@opentelemetry/api**: (compatible with SDK 2.x)
- **@opentelemetry/sdk-trace-web**: 2.x (SDK 2.0+, released March 2025)
- **Requirements**: Node.js >= 18.19.0, TypeScript >= 5.0.4, ES2022 target
- **Implementation**: Manual span creation with business attributes
- **Constant File**: `otelAttributes.ts` (centralized span attribute names)

**Monitoring Stack:**
- **Grafana Stack**: Tempo (traces) + Loki (logs) + Prometheus (metrics)
- **OpenTelemetry Collector**: (unified data collection, OTLP protocol)
- **Chaos Engineering**: Chaos Mesh

### Testing Frameworks & Strategy

**Java Testing:**
- **JUnit**: 5.10.x (Spring Boot 3.4 default)
- **Mockito**: 5.x
- **AssertJ**: 3.x (optional, better assertions)
- **REST Assured**: 5.4.x (API integration tests)

**Python Testing:**
- **pytest**: 8.x
- **httpx**: 0.27.x (FastAPI test client)
- **pytest-asyncio**: 0.24.x (async test support)

**React Testing:**
- **Vitest**: 2.x (Vite official recommendation, faster than Jest)
- **@testing-library/react**: 16.x
- **@testing-library/user-event**: 14.x
- **jsdom**: 25.x (DOM environment simulation)

**Test Coverage Requirements:**
- **Target**: 70% overall code coverage
- **Critical (Must Test)**: Service layer, SAGA orchestration logic, error handling & compensation logic
- **Important (Should Test)**: Controller/Resource layer, Repository layer, custom Exception handlers
- **Optional (May Skip)**: Config classes, DTO/Model classes, simple utility methods

**Integration Test Strategy:**
- **API Tests**: Required for all REST endpoints (REST Assured, httpx, MSW)
- **Database Integration**: Testcontainers PostgreSQL (real database in Docker)
- **Kafka Integration**: Embedded Kafka (in-memory, fast, NOT Testcontainers Kafka)

---

## Critical Version Constraints

⚠️ **MUST use exact versions below to avoid compatibility issues:**

1. **Material-UI v5.15.0+** (v4 has breaking changes, do NOT downgrade to v4)
2. **Emotion ^11.11.0** (required by MUI v5, incompatible with older versions)
3. **Kafka KRaft mode** (ZooKeeper mode is deprecated and NOT supported, minimum Kafka 3.3.1+)
4. **Java 17** (Spring Boot 3.x and Quarkus 3.x minimum requirement, Java 16 or lower NOT supported)
5. **React 18** (MUI v5 + OpenTelemetry Browser SDK 2.x compatibility requirement)
6. **Flyway 10.x** (Spring Boot 3.x compatibility, Flyway 9.x may have issues)
7. **Node.js 20.x** (OpenTelemetry SDK 2.x requires Node.js >= 18.19.0)
8. **TypeScript >= 5.0.4** (OpenTelemetry SDK 2.x minimum requirement, 4.x NOT supported)
9. **Spring Cloud Gateway 3.1.x + Spring Boot 3.4.x** (Gateway 3.1.x requires Boot 3.1.x+, NOT compatible with Boot 3.0.x)
10. **OpenTelemetry SDK 2.x** (Browser/React only, requires ES2022 compilation target)

---

## Version Compatibility Matrix

| Component | Version | Compatible With | NOT Compatible With |
|-----------|---------|-----------------|---------------------|
| Spring Boot | 3.4.x | Java 17+, Flyway 10.x, Gateway 3.1.x | Java 16 or lower, Flyway 9.x, Gateway 3.0.x |
| Quarkus | 3.15.x LTS | Java 17+, Maven 3.8+ | Java 16 or lower |
| Spring Cloud Gateway | 3.1.x | Spring Boot 3.1.x+ | Spring Boot 3.0.x |
| OpenTelemetry JS SDK | 2.x | Node.js 18.19.0+, TypeScript 5.0.4+, ES2022 | Node.js 16.x or lower, TypeScript 4.x, ES2017 |
| Material-UI | v5.15.0 | React 18, Emotion 11.x | React 17 or lower, MUI v4, styled-components |
| Kafka KRaft | 4.0+ | Kafka 3.3.1+ | ZooKeeper mode, Kafka 3.2.x or lower |

---

## Package Manager Commands Reference

**Java (Maven):**
```bash
./mvnw clean install              # Build project
./mvnw spring-boot:run            # Run Spring Boot service
./mvnw quarkus:dev                # Run Quarkus service in dev mode
./mvnw test                       # Run tests
```

**Python (pip):**
```bash
pip install -r requirements.txt       # Install dependencies
pip install -r requirements-dev.txt   # Install dev dependencies
pytest                                # Run tests
pytest --cov=app --cov-report=term   # Run tests with coverage
```

**Frontend (npm):**
```bash
npm install                       # Install dependencies
npm run dev                       # Start Vite dev server
npm run build                     # Build for production
npm run test                      # Run Vitest tests
npm run test:coverage             # Run tests with coverage
```

---

## Docker Base Image Tags

**Production Images (use exact tags for reproducibility):**
```dockerfile
# Java Services
FROM eclipse-temurin:17-jre AS runtime

# Python Service
FROM python:3.11-slim AS runtime

# Frontend
FROM nginx:1.25-alpine
```

**Build Stage Images:**
```dockerfile
# Java Build
FROM eclipse-temurin:17-jdk AS build

# Python Build
FROM python:3.11-slim AS build

# Frontend Build
FROM node:20-alpine AS build
```

---

## Language-Specific Rules

### Java (Spring Boot & Quarkus) Critical Rules

**⚠️ Naming Conventions (Strictly Enforced):**
- **Classes**: PascalCase, 名詞 (`AccountService`, `TransferController`, `SagaExecutionRepository`)
- **Methods**: camelCase, 動詞開頭 (`getAccountById()`, `createTransfer()`, `validateBalance()`)
- **Variables**: camelCase (`accountId`, `transactionAmount`, `userId`)
- **Constants**: UPPER_SNAKE_CASE (`MAX_RETRY_ATTEMPTS`, `DEFAULT_TIMEOUT_MS`, `SAGA_STEP_FREEZE_ACCOUNT`)
- **Packages**: 全小寫, 點號分隔, 反向域名 (`com.bank.account.service`, `com.bank.transfer.saga`)
- **Exception Classes**: 必須以 `Exception` 結尾 (`AccountNotFoundException`, `InsufficientBalanceException`)

**⚠️ Optional Usage Pattern (Repository → Service → Controller):**

**CRITICAL: This is the MOST COMMON mistake AI agents make in layered architecture.**

```java
// ✅ CORRECT: Repository layer returns Optional
public interface AccountRepository extends JpaRepository<Account, String> {
    Optional<Account> findByAccountId(String accountId);
}

// ✅ CORRECT: Service layer converts Optional to Exception
@Service
public class AccountService {
    public Account getAccountById(String accountId) {
        return repository.findByAccountId(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));
        // If not found, throw Exception to be handled by GlobalExceptionHandler
    }
}

// ✅ CORRECT: Controller just returns the data
@RestController
public class AccountController {
    @GetMapping("/api/v1/accounts/{accountId}")
    public Account getAccount(@PathVariable String accountId) {
        return service.getAccountById(accountId);
        // Simple! No Optional handling needed
    }
}

// ❌ WRONG: Service returns Optional (forces every Controller to handle it)
@Service
public class AccountService {
    public Optional<Account> getAccountById(String accountId) {
        return repository.findByAccountId(accountId);  // Don't do this!
    }
}
```

**Rule:** Optional ONLY in Repository layer. Service layer MUST convert to Exception.

**⚠️ Date/Time Handling:**

```java
// ✅ CORRECT: Use java.time.Instant for timestamps
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonFormat;

public class Transaction {
    @JsonFormat(shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                timezone = "UTC")
    private Instant createdAt;
}

// ❌ WRONG: Do NOT use these
private Date createdAt;        // Deprecated, NOT thread-safe
private Long createdAt;        // Unix timestamp, NOT ISO 8601
private LocalDateTime createdAt;  // No timezone info
```

**⚠️ Repository Pattern:**

```java
// ✅ CORRECT: Extend JpaRepository
public interface AccountRepository extends JpaRepository<Account, String> {
    Optional<Account> findByAccountId(String accountId);
    List<Account> findByUserId(String userId);
}

// ❌ WRONG: Do NOT create custom DAO classes
public class AccountDAO {
    public Account findById(String id) { ... }
}
```

**⚠️ Service Layer Pattern:**

```java
// OPTION 1: Interface + Implementation (optional, not mandatory)
public interface AccountService {
    Account getAccountById(String accountId);
}

@Service
public class AccountServiceImpl implements AccountService {
    // Implementation
}

// OPTION 2: Direct implementation (also acceptable)
@Service
public class AccountService {
    // Implementation
}

// Both are acceptable. Choose based on team preference.
```

**⚠️ Environment Variables (NO hardcoding):**

```java
// ✅ CORRECT: Read from application.yml via @Value
@Value("${otel.exporter.otlp.endpoint}")
private String otelEndpoint;

@Value("${spring.kafka.bootstrap-servers}")
private String kafkaBootstrapServers;

// ❌ WRONG: Hardcoded values
private String otelEndpoint = "http://localhost:4318";
private String kafkaBootstrapServers = "localhost:9092";
```

---

### Python (FastAPI) Critical Rules

**⚠️ Naming Conventions (Strictly Enforced):**
- **Modules/Files**: snake_case (`account_service.py`, `saga_orchestrator.py`, `otel_config.py`)
- **Functions**: snake_case, 動詞開頭 (`get_account_by_id()`, `create_transfer()`, `validate_balance()`)
- **Classes**: PascalCase (`TransferRequest`, `SagaOrchestrator`, `AccountResponse`)
- **Variables**: snake_case (`account_id`, `transaction_amount`, `user_id`)
- **Constants**: UPPER_SNAKE_CASE (`MAX_RETRY_ATTEMPTS`, `DEFAULT_TIMEOUT_SECONDS`)

**⚠️ CRITICAL: Pydantic Field Aliases (snake_case → camelCase JSON)**

**This is the #1 MOST COMMON mistake AI agents make in Python FastAPI projects.**

```python
# ✅ CORRECT: Use Field(alias=...) to convert snake_case to camelCase JSON
from pydantic import BaseModel, Field
from datetime import datetime

class AccountResponse(BaseModel):
    account_id: str = Field(..., alias="accountId")
    user_id: str = Field(..., alias="userId")
    balance: float
    currency: str
    created_at: datetime = Field(..., alias="createdAt")

    class Config:
        populate_by_name = True  # Allow both snake_case and camelCase input
        json_schema_extra = {
            "example": {
                "accountId": "TWD-001",
                "userId": "user-001",
                "balance": 10000.00,
                "currency": "TWD",
                "createdAt": "2024-12-04T10:30:00Z"
            }
        }

# API Response will be:
# {
#   "accountId": "TWD-001",     ← camelCase
#   "userId": "user-001",       ← camelCase
#   "balance": 10000.00,
#   "currency": "TWD",
#   "createdAt": "2024-12-04T10:30:00Z"  ← camelCase
# }

# ❌ WRONG: Without aliases, JSON will have snake_case (inconsistent with API standard)
class AccountResponse(BaseModel):
    account_id: str  # JSON output: {"account_id": "..."} ← WRONG!
    user_id: str
```

**Rule:** ALL Pydantic models for API request/response MUST use `Field(alias=...)` for camelCase JSON.

**⚠️ Async/Await Usage Rules:**

```python
# ✅ CORRECT: Use async def ONLY for I/O operations (database, HTTP, file)
async def get_account_from_db(account_id: str) -> Account:
    result = await database.execute(
        select(Account).where(Account.id == account_id)
    )
    return result.scalar_one_or_none()

async def call_external_api(url: str) -> dict:
    async with httpx.AsyncClient() as client:
        response = await client.get(url)
        return response.json()

# ✅ CORRECT: Use regular def for pure computation (no I/O)
def calculate_total(amount: float, rate: float) -> float:
    return amount * rate

def validate_account_id(account_id: str) -> bool:
    return bool(re.match(r'^[A-Z]{3}-\d{3}$', account_id))

# ✅ CORRECT: FastAPI route handlers use async def (usually call database/services)
@app.get("/api/v1/accounts/{account_id}")
async def get_account(account_id: str):
    account = await get_account_from_db(account_id)
    if not account:
        raise HTTPException(status_code=404, detail="Account not found")
    return account

# ❌ WRONG: Using async for pure computation (unnecessary)
async def calculate_total(amount: float, rate: float) -> float:
    return amount * rate  # No I/O, don't need async!

# ❌ WRONG: Using sync for I/O operations (blocking)
def get_account_from_db(account_id: str) -> Account:
    result = database.query(Account).filter_by(id=account_id).first()
    # This blocks the event loop!
```

**Rule:** async/await ONLY for I/O operations. Pure computation uses regular def.

**⚠️ Type Hints (Mandatory):**

```python
# ✅ CORRECT: Always use type hints
from typing import Optional, List

def get_account_by_id(account_id: str) -> Optional[Account]:
    ...

async def get_all_accounts(user_id: str) -> List[Account]:
    ...

# ❌ WRONG: No type hints
def get_account_by_id(account_id):
    ...
```

**⚠️ Environment Variables (NO hardcoding):**

```python
# ✅ CORRECT: Use pydantic_settings
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    database_url: str
    otel_endpoint: str
    kafka_bootstrap_servers: str

    class Config:
        env_file = ".env"

settings = Settings()

# Usage
database_url = settings.database_url

# ❌ WRONG: Hardcoded
DATABASE_URL = "postgresql://localhost:5432/bank"
OTEL_ENDPOINT = "http://localhost:4318"
```

---

### TypeScript / React Critical Rules

**⚠️ Naming Conventions (Strictly Enforced):**
- **Component Files**: PascalCase `.tsx` (`AccountCard.tsx`, `TransferForm.tsx`, `LoginPage.tsx`)
- **Component Names**: PascalCase, 與檔案名一致 (`export const AccountCard = ...`)
- **Hooks**: camelCase + `use` prefix (`useAuth`, `useAccounts`, `useWebSocket`)
- **Utility Files**: camelCase `.ts` (`apiClient.ts`, `formatCurrency.ts`, `otelConfig.ts`)
- **Variables/Functions**: camelCase (`accountId`, `fetchAccounts()`, `handleSubmit()`)
- **Constants**: UPPER_SNAKE_CASE (`API_BASE_URL`, `MAX_RETRIES`)

**⚠️ Component File Naming (Common Mistake):**

```typescript
// ✅ CORRECT: PascalCase component file names
AccountCard.tsx
TransferForm.tsx
LoginPage.tsx

// ❌ WRONG: Do NOT use kebab-case or snake_case
account-card.tsx    ← WRONG
transfer_form.tsx   ← WRONG
login-page.tsx      ← WRONG
```

**⚠️ Props Interface Pattern:**

```typescript
// ✅ CORRECT: {ComponentName}Props naming
interface AccountCardProps {
  account: Account;
  onSelect: (accountId: string) => void;
}

export const AccountCard: React.FC<AccountCardProps> = ({ account, onSelect }) => {
  return (
    <div onClick={() => onSelect(account.accountId)}>
      <h3>{account.currency} Account</h3>
      <p>Balance: {account.balance.toFixed(2)}</p>
    </div>
  );
};

// ❌ WRONG: Generic or inconsistent naming
interface Props { ... }              ← Too generic
interface IAccountCardProps { ... }  ← No "I" prefix in TypeScript
interface accountCardProps { ... }   ← Not PascalCase
```

**⚠️ Avoid `any` Type:**

```typescript
// ✅ CORRECT: Use specific types or unknown
function processAccounts(accounts: Account[]): void { ... }
function handleError(error: unknown): void {
  if (error instanceof Error) {
    console.error(error.message);
  }
}

// ❌ WRONG: Avoid any unless absolutely necessary
function processAccounts(accounts: any): void { ... }
function handleError(error: any): void { ... }
```

**⚠️ React 18 Strict Mode & useEffect Cleanup:**

```typescript
// ✅ CORRECT: Wrap app in StrictMode (Vite default)
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>
);

// ✅ CORRECT: useEffect MUST have cleanup function
useEffect(() => {
  const controller = new AbortController();

  fetch('/api/v1/accounts', { signal: controller.signal })
    .then(res => res.json())
    .then(setAccounts);

  // MANDATORY cleanup function
  return () => {
    controller.abort();
  };
}, []);

// ❌ WRONG: No cleanup function (memory leak)
useEffect(() => {
  fetch('/api/v1/accounts')
    .then(res => res.json())
    .then(setAccounts);
  // Missing return () => { ... }
}, []);
```

**⚠️ Environment Variables (Vite-specific):**

```typescript
// ✅ CORRECT: Use Vite's import.meta.env
const API_URL = import.meta.env.VITE_API_URL;
const OTEL_ENDPOINT = import.meta.env.VITE_OTEL_ENDPOINT;

// .env file:
// VITE_API_URL=http://localhost:8080
// VITE_OTEL_ENDPOINT=http://localhost:4318

// ❌ WRONG: Hardcoded
const API_URL = 'http://localhost:8080';

// ❌ WRONG: Using process.env (webpack pattern, NOT Vite)
const API_URL = process.env.REACT_APP_API_URL;
```

---

### Testing Code Rules (All Languages)

**⚠️ Java Test Naming:**

```java
// ✅ CORRECT: Use should_when pattern
@Test
public void shouldReturnAccount_whenValidAccountIdProvided() {
    // Given
    String accountId = "TWD-001";
    when(repository.findByAccountId(accountId))
        .thenReturn(Optional.of(expectedAccount));

    // When
    Account actual = service.getAccountById(accountId);

    // Then
    assertThat(actual).isEqualTo(expectedAccount);
}

@Test
public void shouldThrowException_whenAccountNotFound() {
    // Given
    when(repository.findByAccountId(anyString()))
        .thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> service.getAccountById("INVALID"))
        .isInstanceOf(AccountNotFoundException.class);
}

// ❌ WRONG: Unclear test names
@Test
public void test1() { ... }

@Test
public void testGetAccount() { ... }
```

**⚠️ Java Test File Location:**

```
// ✅ CORRECT: Tests in src/test directory
src/test/java/com/bank/account/service/AccountServiceTest.java

// ❌ WRONG: Tests mixed with main code
src/main/java/com/bank/account/service/AccountServiceTest.java
```

**⚠️ Python Test Naming:**

```python
# ✅ CORRECT: Use test_ prefix and descriptive names
def test_transfer_succeeds_when_balance_sufficient():
    # Arrange
    account = Account(account_id="TWD-001", balance=1000.0)

    # Act
    result = transfer_service.execute(account, 500.0)

    # Assert
    assert result.status == "SUCCESS"
    assert account.balance == 500.0

# ✅ CORRECT: Async tests need @pytest.mark.asyncio
@pytest.mark.asyncio
async def test_create_account_returns_new_account():
    result = await account_service.create(request)
    assert result.account_id is not None

# ❌ WRONG: Missing test_ prefix (pytest won't run it)
def check_transfer_works():
    ...

# ❌ WRONG: Async test without decorator
async def test_create_account():  # Won't run properly!
    result = await account_service.create(request)
```

**⚠️ React Test Naming & Async Handling:**

```typescript
// ✅ CORRECT: Use .test.tsx suffix (Vitest convention)
AccountCard.test.tsx
TransferForm.test.tsx

// ❌ WRONG: Inconsistent suffixes
AccountCard.spec.tsx
AccountCard.test.ts  // Should be .tsx for components

// ✅ CORRECT: Wait for async operations
import { render, screen, waitFor } from '@testing-library/react';

test('should display accounts after loading', async () => {
  render(<AccountList />);

  await waitFor(() => {
    expect(screen.getByText('Account 1')).toBeInTheDocument();
  });
});

// ❌ WRONG: Not waiting for async operations
test('should display accounts', () => {
  render(<AccountList />);
  expect(screen.getByText('Account 1')).toBeInTheDocument();  // May fail!
});
```

---

## Cross-Language Consistency Rules

### JSON API Naming (ALL Languages MUST Follow)

**⚠️ CRITICAL: All JSON APIs use camelCase, regardless of backend language:**

```json
// ✅ CORRECT: camelCase in all APIs
{
  "accountId": "TWD-001",
  "userId": "user-001",
  "transactionAmount": 1000.00,
  "createdAt": "2024-12-04T10:30:00.123Z"
}

// ❌ WRONG: snake_case in JSON (even from Python backend)
{
  "account_id": "TWD-001",
  "user_id": "user-001",
  "transaction_amount": 1000.00
}
```

**Implementation by Language:**
- **Java**: Jackson default (no config needed) ✅
- **Python**: Pydantic `Field(alias=...)` (MANDATORY) ⚠️
- **TypeScript**: Interface already camelCase (no conversion needed) ✅

### Date/Time Format (ALL Languages)

**⚠️ MANDATORY: ISO 8601 with UTC timezone:**

```
Format: YYYY-MM-DDTHH:mm:ss.sssZ
Example: 2024-12-04T10:30:00.123Z
```

**NEVER use:**
- ❌ Unix timestamps (`1733312400000`)
- ❌ Non-UTC timezones (`2024-12-04T10:30:00+08:00`)
- ❌ Space-separated format (`2024-12-04 10:30:00`)

### Error Response Format (ALL Languages)

```json
{
  "errorCode": "ERR_001",
  "message": "Insufficient balance",
  "timestamp": "2024-12-04T10:30:00.123Z",
  "path": "/api/v1/transfers"
}
```

**Field naming:** camelCase
**Status code:** Use HTTP standard codes (400, 404, 500, etc.)
**Implementation:** GlobalExceptionHandler (Java) or exception_handler (Python)

---

## Framework-Specific Rules

### Spring Boot Critical Patterns

**⚠️ CRITICAL: Dependency Injection (Constructor Injection ONLY):**

```java
// ✅ CORRECT: Constructor injection (recommended, immutable)
@RestController
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }
}

// ❌ WRONG: Field injection (not recommended, harder to test)
@RestController
public class AccountController {
    @Autowired
    private AccountService accountService;  // Avoid @Autowired on fields
}
```

**Rule:** ALWAYS use constructor injection. Spring Boot 4.3+ automatically injects single-constructor beans (no @Autowired needed on constructor).

**⚠️ Exception Handling (GlobalExceptionHandler Pattern):**

```java
// ✅ CORRECT: Centralized exception handling
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(
            AccountNotFoundException ex,
            HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
            .errorCode("ERR_001")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(
            InsufficientBalanceException ex,
            HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
            .errorCode("ERR_002")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}

// ❌ WRONG: Exception handling in Controller
@RestController
public class AccountController {
    @GetMapping("/api/v1/accounts/{accountId}")
    public ResponseEntity<?> getAccount(@PathVariable String accountId) {
        try {
            Account account = service.getAccountById(accountId);
            return ResponseEntity.ok(account);
        } catch (AccountNotFoundException ex) {
            return ResponseEntity.notFound().build();  // Don't do this!
        }
    }
}
```

**⚠️ OpenTelemetry Manual Instrumentation:**

```java
// ✅ CORRECT: Manual span creation with business attributes
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

@Service
public class AccountService {
    private final Tracer tracer;

    public Account getAccountById(String accountId) {
        Span span = tracer.spanBuilder("AccountService.getAccountById")
            .startSpan();

        try {
            // Add business attributes
            span.setAttribute("account.id", accountId);
            span.setAttribute("service.name", "account-service");

            Account account = repository.findByAccountId(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

            span.setAttribute("account.currency", account.getCurrency());
            span.setStatus(StatusCode.OK);
            return account;
        } catch (Exception ex) {
            span.recordException(ex);
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
        }
    }
}

// ❌ WRONG: Using auto-instrumentation or no attributes
// Auto-instrumentation misses business context like account.id, account.currency
```

**Rule:** ALWAYS add business attributes to spans (account.id, transaction.amount, saga.step, etc.).

---

### Quarkus Critical Patterns

**⚠️ Resource vs Controller Naming:**

```java
// ✅ CORRECT: Quarkus uses "Resource" not "Controller"
@Path("/api/v1/accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountResource {  // NOT AccountController

    @Inject
    AccountService accountService;

    @GET
    @Path("/{accountId}")
    public Account getAccount(@PathParam("accountId") String accountId) {
        return accountService.getAccountById(accountId);
    }
}

// ❌ WRONG: Using Spring Boot naming in Quarkus
public class AccountController {  // Wrong name for Quarkus
    @GetMapping("/api/v1/accounts/{accountId}")  // Wrong annotation
    public Account getAccount(@PathVariable String accountId) { ... }
}
```

**⚠️ Exception Mapping (Quarkus-specific):**

```java
// ✅ CORRECT: Quarkus uses ExceptionMapper
@Provider
public class AccountNotFoundExceptionMapper
        implements ExceptionMapper<AccountNotFoundException> {

    @Override
    public Response toResponse(AccountNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            "ERR_001",
            ex.getMessage(),
            Instant.now(),
            // Get path from UriInfo if needed
        );
        return Response.status(Response.Status.NOT_FOUND)
                .entity(error)
                .build();
    }
}

// ❌ WRONG: Using Spring's @ControllerAdvice in Quarkus
@ControllerAdvice  // This is Spring Boot, NOT Quarkus!
public class GlobalExceptionHandler { ... }
```

**⚠️ Dependency Injection (@Inject vs @Autowired):**

```java
// ✅ CORRECT: Quarkus uses @Inject (CDI)
@Path("/api/v1/accounts")
public class AccountResource {
    @Inject
    AccountService accountService;  // Use @Inject

    @Inject
    Tracer tracer;
}

// ❌ WRONG: Using Spring's @Autowired in Quarkus
@Path("/api/v1/accounts")
public class AccountResource {
    @Autowired  // This is Spring Boot, NOT Quarkus!
    AccountService accountService;
}
```

---

### FastAPI Critical Patterns

**⚠️ Dependency Injection (FastAPI Depends):**

```python
# ✅ CORRECT: Use Depends() for dependency injection
from fastapi import Depends
from sqlalchemy.orm import Session

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

@app.get("/api/v1/accounts/{account_id}")
async def get_account(
    account_id: str,
    db: Session = Depends(get_db)  # Dependency injection
):
    account = await get_account_from_db(db, account_id)
    if not account:
        raise HTTPException(status_code=404, detail="Account not found")
    return account

# ❌ WRONG: Global database connection (not request-scoped)
db = SessionLocal()  # Global, shared across requests - WRONG!

@app.get("/api/v1/accounts/{account_id}")
async def get_account(account_id: str):
    account = db.query(Account).filter_by(id=account_id).first()
```

**⚠️ Exception Handling (Custom exception_handler):**

```python
# ✅ CORRECT: Custom exception handlers
from fastapi import FastAPI, Request, HTTPException
from fastapi.responses import JSONResponse
from datetime import datetime

app = FastAPI()

class AccountNotFoundException(Exception):
    def __init__(self, account_id: str):
        self.account_id = account_id

@app.exception_handler(AccountNotFoundException)
async def account_not_found_handler(request: Request, exc: AccountNotFoundException):
    return JSONResponse(
        status_code=404,
        content={
            "errorCode": "ERR_001",
            "message": f"Account not found: {exc.account_id}",
            "timestamp": datetime.utcnow().isoformat() + "Z",
            "path": str(request.url.path)
        }
    )

# ❌ WRONG: No exception handler, returning dict directly
@app.get("/api/v1/accounts/{account_id}")
async def get_account(account_id: str):
    account = await get_account_from_db(account_id)
    if not account:
        return {"error": "Not found"}  # Inconsistent format!
```

**⚠️ Request/Response Models (Pydantic MUST use):**

```python
# ✅ CORRECT: Use Pydantic models for validation
from pydantic import BaseModel, Field

class TransferRequest(BaseModel):
    from_account_id: str = Field(..., alias="fromAccountId")
    to_account_id: str = Field(..., alias="toAccountId")
    amount: float = Field(..., gt=0)  # Greater than 0
    currency: str = Field(..., regex="^(TWD|USD|JPY)$")

class TransferResponse(BaseModel):
    transfer_id: str = Field(..., alias="transferId")
    status: str
    created_at: datetime = Field(..., alias="createdAt")

@app.post("/api/v1/transfers", response_model=TransferResponse)
async def create_transfer(request: TransferRequest):
    # Pydantic automatically validates input
    result = await transfer_service.execute(request)
    return result

# ❌ WRONG: Using dict without validation
@app.post("/api/v1/transfers")
async def create_transfer(request: dict):  # No validation!
    # What if amount is negative? What if currency is invalid?
    result = await transfer_service.execute(request)
```

---

### React + Material-UI Critical Patterns

**⚠️ Theme Provider (MUST wrap App):**

```typescript
// ✅ CORRECT: Wrap app in ThemeProvider
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';

const theme = createTheme({
  palette: {
    primary: { main: '#1976d2' },
    secondary: { main: '#dc004e' },
  },
});

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider theme={theme}>
      <CssBaseline />  {/* Reset CSS */}
      <App />
    </ThemeProvider>
  </StrictMode>
);

// ❌ WRONG: Using MUI components without ThemeProvider
createRoot(document.getElementById('root')!).render(
  <App />  // MUI components won't have theme!
);
```

**⚠️ Component Structure (Container vs Presentational):**

```typescript
// ✅ CORRECT: Separate data fetching from presentation
// Container component (handles logic)
export const AccountListPage: React.FC = () => {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    fetch('/api/v1/accounts', { signal: controller.signal })
      .then(res => res.json())
      .then(setAccounts)
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));

    return () => controller.abort();
  }, []);

  if (loading) return <CircularProgress />;
  if (error) return <Alert severity="error">{error}</Alert>;

  return <AccountList accounts={accounts} />;
};

// Presentational component (pure UI)
interface AccountListProps {
  accounts: Account[];
}

export const AccountList: React.FC<AccountListProps> = ({ accounts }) => {
  return (
    <Grid container spacing={2}>
      {accounts.map(account => (
        <Grid item xs={12} md={6} key={account.accountId}>
          <AccountCard account={account} />
        </Grid>
      ))}
    </Grid>
  );
};

// ❌ WRONG: Mixing data fetching and presentation
export const AccountList: React.FC = () => {
  const [accounts, setAccounts] = useState<Account[]>([]);

  useEffect(() => {
    fetch('/api/v1/accounts').then(res => res.json()).then(setAccounts);
  }, []);

  // Mixing data logic and UI in one component
  return <Grid>...</Grid>;
};
```

**⚠️ Custom Hooks Pattern:**

```typescript
// ✅ CORRECT: Extract reusable logic into custom hooks
export const useAccounts = (userId: string) => {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    const fetchAccounts = async () => {
      try {
        const response = await fetch(`/api/v1/users/${userId}/accounts`, {
          signal: controller.signal
        });
        const data = await response.json();
        setAccounts(data);
      } catch (err) {
        if (err instanceof Error) setError(err);
      } finally {
        setLoading(false);
      }
    };

    fetchAccounts();
    return () => controller.abort();
  }, [userId]);

  return { accounts, loading, error };
};

// Usage in component
export const AccountListPage: React.FC = () => {
  const { accounts, loading, error } = useAccounts(userId);

  if (loading) return <CircularProgress />;
  if (error) return <Alert severity="error">{error.message}</Alert>;
  return <AccountList accounts={accounts} />;
};

// ❌ WRONG: Duplicating fetch logic in every component
export const AccountListPage: React.FC = () => {
  const [accounts, setAccounts] = useState<Account[]>([]);
  useEffect(() => {
    fetch('/api/v1/accounts').then(...)  // Duplicated in every component
  }, []);
};
```

**⚠️ MUI Icons Import (Tree-shaking):**

```typescript
// ✅ CORRECT: Import specific icons (tree-shaking)
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import TransferWithinAStationIcon from '@mui/icons-material/TransferWithinAStation';
import DeleteIcon from '@mui/icons-material/Delete';

export const AccountCard: React.FC = () => (
  <Card>
    <AccountBalanceIcon />
    <Typography>Account Details</Typography>
  </Card>
);

// ❌ WRONG: Importing entire icons package (huge bundle)
import { AccountBalance, TransferWithinAStation, Delete } from '@mui/icons-material';
// This imports ALL icons! Bundle size will be massive.
```

---

## Critical Don't-Miss Rules (Anti-Patterns)

### Database Anti-Patterns

**❌ N+1 Query Problem:**

```java
// ❌ WRONG: N+1 queries (1 query for accounts + N queries for users)
@Service
public class AccountService {
    public List<AccountDTO> getAllAccounts() {
        List<Account> accounts = repository.findAll();  // 1 query
        return accounts.stream()
            .map(account -> {
                User user = userRepository.findById(account.getUserId()).get();  // N queries!
                return new AccountDTO(account, user);
            })
            .collect(Collectors.toList());
    }
}

// ✅ CORRECT: Use JOIN FETCH to avoid N+1
@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    @Query("SELECT a FROM Account a JOIN FETCH a.user")
    List<Account> findAllWithUser();
}
```

**❌ Missing Transaction Management:**

```java
// ❌ WRONG: No @Transactional for multi-step operations
@Service
public class TransferService {
    public void executeTransfer(TransferRequest request) {
        Account from = accountRepo.findById(request.getFromAccountId()).get();
        from.setBalance(from.getBalance() - request.getAmount());
        accountRepo.save(from);  // If this fails after debit, data is inconsistent!

        Account to = accountRepo.findById(request.getToAccountId()).get();
        to.setBalance(to.getBalance() + request.getAmount());
        accountRepo.save(to);
    }
}

// ✅ CORRECT: Use @Transactional for atomicity
@Service
public class TransferService {
    @Transactional  // All-or-nothing
    public void executeTransfer(TransferRequest request) {
        Account from = accountRepo.findById(request.getFromAccountId()).get();
        from.setBalance(from.getBalance() - request.getAmount());
        accountRepo.save(from);

        Account to = accountRepo.findById(request.getToAccountId()).get();
        to.setBalance(to.getBalance() + request.getAmount());
        accountRepo.save(to);
    }
}
```

### API Anti-Patterns

**❌ Exposing Internal IDs:**

```java
// ❌ WRONG: Exposing database auto-increment IDs
@Entity
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // Database ID exposed in API
}

// API returns: {"id": 1, "balance": 1000}  // Sequential IDs are a security risk!

// ✅ CORRECT: Use business IDs or UUIDs
@Entity
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // Internal database ID

    @Column(unique = true, nullable = false)
    private String accountId;  // Business ID (TWD-001, USD-002)
}

// API returns: {"accountId": "TWD-001", "balance": 1000}
```

**❌ Over-fetching / Under-fetching:**

```java
// ❌ WRONG: Returning entire entity with all relationships
@GetMapping("/api/v1/accounts/{accountId}")
public Account getAccount(@PathVariable String accountId) {
    return repository.findById(accountId).get();
    // Returns EVERYTHING including sensitive fields, lazy-loaded collections
}

// ✅ CORRECT: Use DTOs to control what's exposed
@GetMapping("/api/v1/accounts/{accountId}")
public AccountResponse getAccount(@PathVariable String accountId) {
    Account account = repository.findById(accountId).get();
    return AccountResponse.from(account);  // DTO with only necessary fields
}
```

### React Anti-Patterns

**❌ Missing Dependency Array:**

```typescript
// ❌ WRONG: Missing dependency array (runs on every render)
useEffect(() => {
  fetch('/api/v1/accounts')
    .then(res => res.json())
    .then(setAccounts);
});  // This runs on EVERY render! Infinite loop possible.

// ✅ CORRECT: Empty dependency array for mount-only
useEffect(() => {
  fetch('/api/v1/accounts')
    .then(res => res.json())
    .then(setAccounts);
}, []);  // Runs only once on mount

// ✅ CORRECT: Specific dependencies
useEffect(() => {
  fetch(`/api/v1/users/${userId}/accounts`)
    .then(res => res.json())
    .then(setAccounts);
}, [userId]);  // Re-run when userId changes
```

**❌ State Mutation:**

```typescript
// ❌ WRONG: Mutating state directly
const addAccount = (newAccount: Account) => {
  accounts.push(newAccount);  // Direct mutation!
  setAccounts(accounts);  // React won't detect the change
};

// ✅ CORRECT: Create new array (immutable update)
const addAccount = (newAccount: Account) => {
  setAccounts([...accounts, newAccount]);  // New array
};

// ✅ CORRECT: Update object property immutably
const updateBalance = (accountId: string, newBalance: number) => {
  setAccounts(accounts.map(account =>
    account.accountId === accountId
      ? { ...account, balance: newBalance }  // New object
      : account
  ));
};
```

---

## Next Sections

_The following sections will be added progressively:_

- [x] Language-Specific Rules (Java, Python, TypeScript)
- [x] Framework-Specific Rules (Spring Boot, Quarkus, FastAPI, React)
- [x] Critical Don't-Miss Rules (Anti-Patterns)
- [x] Code Quality & Style Rules
- [ ] Development Workflow Rules

---

## Code Quality & Style Rules

### Naming Conventions (命名慣例)

**⚠️ CRITICAL: Follow language-specific naming patterns strictly to maintain codebase consistency.**

#### Java (Spring Boot & Quarkus)

**Class Naming:**
- **Rule**: PascalCase, noun or noun phrase
- **Examples**:
  - ✅ `AccountService`, `TransferController`, `SagaExecutionRepository`
  - ❌ `accountService`, `Transfer_Controller`, `sagaRepo`

**Method Naming:**
- **Rule**: camelCase, start with verb
- **Examples**:
  - ✅ `getAccountById()`, `createTransfer()`, `validateBalance()`
  - ❌ `GetAccount()`, `account_by_id()`, `validate_balance()`

**Variable Naming:**
- **Rule**: camelCase
- **Examples**:
  - ✅ `accountId`, `transactionAmount`, `userId`
  - ❌ `account_id`, `TransactionAmount`, `user_ID`

**Constant Naming:**
- **Rule**: UPPER_SNAKE_CASE
- **Examples**:
  - ✅ `MAX_RETRY_ATTEMPTS`, `DEFAULT_TIMEOUT_MS`, `SAGA_STEP_FREEZE_ACCOUNT`
  - ❌ `maxRetryAttempts`, `default_timeout`, `SagaStepFreezeAccount`

**Package Naming:**
- **Rule**: all lowercase, dot-separated, reverse domain
- **Examples**:
  - ✅ `com.bank.account.service`, `com.bank.transfer.saga`
  - ❌ `com.bank.Account.Service`, `com.bank.transfer_saga`

#### Python (FastAPI)

**Module/File Naming:**
- **Rule**: lowercase snake_case
- **Examples**:
  - ✅ `account_service.py`, `saga_orchestrator.py`, `otel_config.py`
  - ❌ `AccountService.py`, `sagaOrchestrator.py`

**Function Naming:**
- **Rule**: lowercase snake_case, start with verb
- **Examples**:
  - ✅ `get_account_by_id()`, `create_transfer()`, `validate_balance()`
  - ❌ `getAccountById()`, `CreateTransfer()`

**Class Naming:**
- **Rule**: PascalCase
- **Examples**:
  - ✅ `TransferRequest`, `SagaOrchestrator`, `AccountResponse`
  - ❌ `transfer_request`, `sagaOrchestrator`

**Variable Naming:**
- **Rule**: lowercase snake_case
- **Examples**:
  - ✅ `account_id`, `transaction_amount`, `user_id`
  - ❌ `accountId`, `TransactionAmount`

**Constant Naming:**
- **Rule**: UPPER_SNAKE_CASE
- **Examples**:
  - ✅ `MAX_RETRY_ATTEMPTS`, `DEFAULT_TIMEOUT_SECONDS`
  - ❌ `MaxRetryAttempts`, `max_retry_attempts`

#### TypeScript / React

**Component File Naming:**
- **Rule**: PascalCase (`.tsx` files)
- **Examples**:
  - ✅ `AccountCard.tsx`, `TransferForm.tsx`, `LoginPage.tsx`
  - ❌ `account-card.tsx`, `transferForm.tsx`, `login_page.tsx`

**Component Naming:**
- **Rule**: PascalCase, match file name
- **Examples**:
  - ✅ `export const AccountCard = () => {...}`
  - ❌ `export const accountCard = () => {...}`

**Hook Naming:**
- **Rule**: camelCase, `use` prefix
- **Examples**:
  - ✅ `useAuth()`, `useAccounts()`, `useWebSocket()`
  - ❌ `UseAuth()`, `use_accounts()`

**Utility File Naming:**
- **Rule**: camelCase (`.ts` files)
- **Examples**:
  - ✅ `apiClient.ts`, `formatCurrency.ts`, `otelConfig.ts`
  - ❌ `ApiClient.ts`, `format_currency.ts`

**Variable/Function Naming:**
- **Rule**: camelCase
- **Examples**:
  - ✅ `accountId`, `fetchAccounts()`, `handleSubmit()`
  - ❌ `account_id`, `FetchAccounts()`

**Constant Naming:**
- **Rule**: UPPER_SNAKE_CASE
- **Examples**:
  - ✅ `API_BASE_URL`, `MAX_RETRIES`
  - ❌ `apiBaseUrl`, `MaxRetries`

---

### Project Structure Patterns (專案結構模式)

**⚠️ CRITICAL: Follow exact directory structure to maintain architectural consistency.**

#### Java Services Structure

**Spring Boot Service:**

```
account-service/
├── src/
│   ├── main/
│   │   ├── java/com/bank/account/
│   │   │   ├── controller/          # REST endpoints
│   │   │   ├── service/             # Business logic
│   │   │   ├── repository/          # Data access
│   │   │   ├── model/               # Domain entities + DTOs
│   │   │   ├── exception/           # Custom exceptions
│   │   │   ├── config/              # Configuration classes
│   │   │   └── {Service}Application.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── logback-spring.xml
│   │       └── db/migration/        # Flyway migrations
│   └── test/                        # SEPARATE test directory
│       └── java/com/bank/account/
│           ├── controller/
│           ├── service/
│           └── repository/
├── pom.xml
└── README.md
```

**Critical Rules:**
- ✅ Tests MUST be in separate `src/test/java/` directory (NOT co-located)
- ✅ Package organization by layer (controller, service, repository)
- ✅ Exception package MUST be separate
- ✅ Config package for centralized configuration
- ❌ Do NOT mix test files with main code
- ❌ Do NOT use `src/main/test/` (incorrect path)

#### Python Services Structure

**FastAPI Service:**

```
transfer-service/
├── app/
│   ├── main.py
│   ├── api/
│   │   ├── routes/                  # API route handlers
│   │   └── dependencies.py
│   ├── services/                    # Business logic
│   ├── models/                      # Pydantic models
│   ├── exceptions/                  # Custom exceptions
│   ├── config/                      # Settings + OTel config
│   └── utils/
├── tests/                           # SEPARATE test directory
│   ├── test_saga_orchestrator.py
│   └── test_transfer_api.py
├── requirements.txt
├── Dockerfile
└── README.md
```

**Critical Rules:**
- ✅ Tests MUST be in separate `tests/` directory (NOT inside `app/`)
- ✅ `api/` for FastAPI routes & dependencies
- ✅ `services/` for business logic
- ✅ `models/` for Pydantic models
- ✅ `config/` for settings & OTel config
- ❌ Do NOT put tests inside `app/tests/`
- ❌ Do NOT name files with hyphens (use snake_case)

#### React Frontend Structure

**Frontend Structure:**

```
frontend/
├── public/
│   ├── index.html
│   └── favicon.ico
├── src/
│   ├── index.tsx
│   ├── App.tsx
│   ├── components/                  # Reusable components
│   │   ├── common/                  # Shared components
│   │   ├── accounts/                # Domain-specific
│   │   └── transfers/
│   ├── pages/                       # Page components
│   ├── contexts/                    # React Context providers
│   ├── hooks/                       # Custom React hooks
│   ├── services/                    # API clients
│   ├── utils/                       # Utility functions
│   ├── types/                       # TypeScript types
│   └── styles/                      # Theme + global styles
├── package.json
├── tsconfig.json
└── vite.config.ts
```

**Critical Rules:**
- ✅ `components/` organized by domain (accounts, transfers, common)
- ✅ `pages/` for top-level page components
- ✅ `hooks/` for custom React hooks (use* prefix)
- ✅ `services/` for API client logic
- ✅ `types/` for TypeScript type definitions
- ✅ Tests can be co-located OR in `src/__tests__/`
- ❌ Do NOT mix utility functions in components
- ❌ Do NOT put API logic directly in components

---

### Test Organization Rules

**⚠️ CRITICAL: Test file location MUST follow language conventions.**

#### Java (JUnit 5)

**Test File Location:**
- ✅ MUST be in `src/test/java/{package}/`
- ❌ NEVER in `src/main/java/`

**Test Naming Convention:**
```java
// ✅ CORRECT: should_when pattern
@Test
void shouldReturnAccount_whenValidAccountIdProvided() {
    // Given
    String accountId = "TWD-001";
    when(repository.findByAccountId(accountId))
        .thenReturn(Optional.of(expectedAccount));

    // When
    Account actual = service.getAccountById(accountId);

    // Then
    assertThat(actual).isEqualTo(expectedAccount);
}

// ❌ WRONG: Unclear test names
@Test
void test1() { ... }

@Test
void getAccountTest() { ... }
```

**Rule:** Use `shouldX_whenY` pattern for test method names.

#### Python (pytest)

**Test File Location:**
- ✅ MUST be in `tests/` directory (separate from `app/`)
- ❌ NEVER in `app/tests/`

**Test Naming Convention:**
```python
# ✅ CORRECT: test_ prefix + descriptive name
def test_create_transfer_success():
    # Arrange
    request = TransferRequest(...)

    # Act
    result = transfer_service.create(request)

    # Assert
    assert result.status == "COMPLETED"

# ✅ CORRECT: Async tests with decorator
@pytest.mark.asyncio
async def test_saga_orchestrator_compensates_on_failure():
    result = await saga_orchestrator.execute(transfer)
    assert result.status == "COMPENSATED"

# ❌ WRONG: No test_ prefix
def create_transfer_test(): ...

# ❌ WRONG: Async test without decorator
async def test_async_function():  # Won't run properly!
    await some_async_call()
```

**Rule:** All test functions MUST start with `test_` prefix.

#### TypeScript / React (Vitest + React Testing Library)

**Test File Location:**
- ✅ Option A: Co-located with component (`AccountCard.test.tsx` next to `AccountCard.tsx`)
- ✅ Option B: In `src/__tests__/` directory
- ❌ NEVER mix both approaches in same project

**Test Naming Convention:**
```typescript
// ✅ CORRECT: describe + it pattern
describe('AccountCard', () => {
  it('should display account balance correctly', () => {
    render(<AccountCard account={mockAccount} />);
    expect(screen.getByText('$10,000.00')).toBeInTheDocument();
  });

  it('should show loading state when fetching data', () => {
    render(<AccountCard account={null} loading={true} />);
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });
});

// ❌ WRONG: No describe block
it('account card test', () => { ... });

// ❌ WRONG: Unclear test names
test('test1', () => { ... });
```

**Rule:** Use `describe` for component grouping + `it` for individual test cases.

---

### Code Review Checklist (AI Agents MUST Verify)

**Before marking any implementation as complete, verify ALL of the following:**

#### Naming Verification
- [ ] JSON field names use camelCase (NOT snake_case)
- [ ] Java classes use PascalCase
- [ ] Java methods/variables use camelCase
- [ ] Python modules/files use snake_case
- [ ] Python classes use PascalCase
- [ ] React components use PascalCase (.tsx files)
- [ ] React hooks use camelCase with `use` prefix
- [ ] Constants use UPPER_SNAKE_CASE (all languages)

#### Structure Verification
- [ ] Java tests are in `src/test/java/` (NOT `src/main/test/`)
- [ ] Python tests are in `tests/` (NOT `app/tests/`)
- [ ] Java packages organized by layer (controller, service, repository)
- [ ] Python modules organized by function (api, services, models, config)
- [ ] React components organized by domain (common, accounts, transfers)

#### Test Verification
- [ ] Java tests use `shouldX_whenY` naming pattern
- [ ] Python tests use `test_` prefix
- [ ] Python async tests have `@pytest.mark.asyncio` decorator
- [ ] React tests use `describe` + `it` pattern
- [ ] Test files are in correct location per language convention

#### Pattern Violation Handling
- [ ] If violation found, record in Code Review comments
- [ ] Require fix before merge
- [ ] Update `docs/architecture.md` if pattern needs modification

---

### Documentation Requirements

**⚠️ Code MUST be self-explanatory. Comments are for WHY, not WHAT.**

#### When to Add Comments

**✅ DO Comment:**
- Complex business logic that isn't obvious
- Non-obvious edge cases or workarounds
- Security-sensitive code sections
- Performance optimization rationale
- TODO items with clear context

**❌ DO NOT Comment:**
- Obvious code (e.g., `// Get account by ID` above `getAccountById()`)
- What the code does (code should be self-documenting)
- Commented-out code (delete it, use git history)
- Redundant JavaDoc/docstrings that just repeat method name

#### Comment Examples

```java
// ✅ CORRECT: Explains WHY, non-obvious business rule
@Service
public class TransferService {
    // CRITICAL: Must freeze accounts in alphabetical order to prevent deadlocks
    // when two concurrent transfers involve the same accounts in different order
    public void executeTransfer(Transfer transfer) {
        List<Account> accounts = sortAccountsAlphabetically(
            transfer.getFromAccount(),
            transfer.getToAccount()
        );
        freezeAccounts(accounts);
        // ...
    }
}

// ❌ WRONG: States the obvious
@Service
public class AccountService {
    // Get account by ID
    public Account getAccountById(String id) {
        return repository.findById(id);
    }
}
```

```python
# ✅ CORRECT: Explains complex business logic
async def execute_saga(transfer: Transfer):
    # Use short-lived locks (5s) to prevent saga state corruption
    # while allowing parallel saga executions for different transfers
    async with redis.lock(f"saga:{transfer.id}", timeout=5):
        await orchestrator.execute(transfer)

# ❌ WRONG: Redundant docstring
def get_account(account_id: str) -> Account:
    """Get account by ID."""  # Obvious from function name!
    return repository.find_by_id(account_id)
```

```typescript
// ✅ CORRECT: Explains performance optimization
export const AccountList: React.FC<AccountListProps> = ({ accounts }) => {
  // Use React.memo to prevent re-renders when parent updates
  // but accounts array reference hasn't changed
  const MemoizedAccountCard = React.memo(AccountCard);

  return (
    <div>
      {accounts.map(account => (
        <MemoizedAccountCard key={account.accountId} account={account} />
      ))}
    </div>
  );
};

// ❌ WRONG: Obvious comment
// Render account list
return <AccountList accounts={accounts} />;
```

**Rule Summary:**
- Comments explain **WHY**, not **WHAT**
- Code structure explains **WHAT**
- If code needs comment to explain WHAT it does, refactor it

---

_The following sections will be added progressively:_

- [x] Language-Specific Rules (Java, Python, TypeScript)
- [x] Framework-Specific Rules (Spring Boot, Quarkus, FastAPI, React)
- [x] Critical Don't-Miss Rules (Anti-Patterns)
- [x] Code Quality & Style Rules
- [x] Development Workflow Rules

---

## Development Workflow Rules

### Git Repository Rules

**⚠️ CRITICAL: Git workflow patterns for microservices project.**

#### Repository Structure

**✅ CORRECT: Monorepo structure (all services in one repository)**

```
lite-bank-demo/
├── services/
│   ├── account-service/       # Spring Boot
│   ├── transfer-service/      # Python FastAPI
│   ├── notification-service/  # Quarkus
│   └── ...
├── frontend/                  # React + Vite
├── infrastructure/            # Kubernetes manifests
├── docs/                      # Documentation
└── scripts/                   # Deployment scripts
```

**Why Monorepo:**
- Simplified cross-service refactoring
- Easier trace context propagation testing across services
- Single source of truth for architecture decisions
- Simplified dependency version management

**❌ WRONG: Multi-repo (separate repository per service)**
- Increases complexity for trace context propagation testing
- Harder to maintain consistency across services
- Version management nightmare for shared libraries

---

### Branch Naming Convention

**⚠️ MANDATORY: Use descriptive branch names with prefixes.**

**Branch Name Format:**
```
<type>/<short-description>
```

**Branch Types:**
- `feature/` - New features or enhancements
- `fix/` - Bug fixes
- `refactor/` - Code refactoring (no functional changes)
- `test/` - Adding or modifying tests
- `docs/` - Documentation changes
- `chore/` - Build process, dependencies, tooling

**Examples:**

```bash
# ✅ CORRECT: Clear, descriptive branch names
feature/account-creation-api
feature/saga-compensation-logic
fix/kafka-trace-propagation
fix/n-plus-one-query-account-service
refactor/extract-otel-config
test/add-integration-tests-transfer-service
docs/update-architecture-decisions
chore/upgrade-spring-boot-3.4

# ❌ WRONG: Unclear or inconsistent names
my-feature
bug-fix
test
feature-123
john-dev
```

**Rule:** Branch names MUST be lowercase with hyphens (NOT underscores, NOT camelCase).

---

### Commit Message Convention

**⚠️ CRITICAL: Use Conventional Commits format for clear history.**

**Commit Message Format:**
```
<type>(<scope>): <short summary>

<optional body>

<optional footer>
```

**Commit Types:**
- `feat` - New feature
- `fix` - Bug fix
- `refactor` - Code refactoring
- `test` - Add/modify tests
- `docs` - Documentation changes
- `chore` - Build, dependencies, tooling
- `perf` - Performance improvements
- `style` - Code style changes (formatting, no functional changes)

**Scope (optional but recommended):**
- Service name: `account-service`, `transfer-service`, `frontend`
- Component: `saga`, `otel`, `kafka`, `api-gateway`

**Examples:**

```bash
# ✅ CORRECT: Clear, descriptive commit messages
feat(account-service): add account creation endpoint

Implements POST /api/v1/accounts with validation and OpenTelemetry tracing.

- Add AccountController with constructor injection
- Add AccountService with Optional → Exception pattern
- Add integration tests with Testcontainers PostgreSQL
- Add manual OTel spans with business attributes (account.id, account.currency)

🤖 Generated with Claude Code
Co-Authored-By: Claude <noreply@anthropic.com>

# ✅ CORRECT: Bug fix with clear description
fix(transfer-service): propagate trace context in Kafka messages

Fixed missing trace context propagation when publishing transfer events to Kafka.
Added W3C TraceContext headers to Kafka message headers.

Fixes #42

🤖 Generated with Claude Code
Co-Authored-By: Claude <noreply@anthropic.com>

# ✅ CORRECT: Refactoring
refactor(otel): extract common OpenTelemetry config

Extracted shared OTel configuration into base config class to reduce duplication
across account-service, user-service, and notification-service.

🤖 Generated with Claude Code
Co-Authored-By: Claude <noreply@anthropic.com>

# ❌ WRONG: Unclear or too generic
Update code
Fix bug
WIP
asdf
Fixed stuff
```

**Footer Conventions:**
- `Fixes #<issue-number>` - Links to GitHub/GitLab issue
- `BREAKING CHANGE:` - Indicates breaking API changes
- `Co-Authored-By:` - Multiple authors (Claude Code adds this automatically)

**Rule:** First line MUST be ≤ 72 characters. Use present tense ("add" not "added").

---

### Pull Request (PR) Requirements

**⚠️ CRITICAL: Every PR must meet quality standards before merge.**

#### PR Title Convention

**Format:** Same as commit message format
```
<type>(<scope>): <short summary>
```

**Examples:**
```
feat(account-service): add account creation endpoint
fix(kafka): propagate trace context in message headers
refactor(saga): extract compensation logic to separate class
```

#### PR Description Template

```markdown
## Summary
- [Brief description of what this PR does]
- [Why this change is needed]

## Changes Made
- [ ] Added/Modified feature X
- [ ] Fixed bug Y
- [ ] Updated tests
- [ ] Updated documentation

## Type of Change
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] Refactoring (no functional changes)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] All tests passing locally
- [ ] Manual testing completed

## Checklist (AI Agents MUST Verify)
- [ ] Code follows project naming conventions (Java: PascalCase classes, Python: snake_case files, React: PascalCase components)
- [ ] JSON APIs use camelCase (NOT snake_case)
- [ ] Tests are in correct location (Java: `src/test/`, Python: `tests/`, React: co-located or `__tests__/`)
- [ ] No hardcoded values (environment variables used)
- [ ] OpenTelemetry manual instrumentation with business attributes added
- [ ] Error handling follows project patterns (GlobalExceptionHandler/exception_handler)
- [ ] No N+1 query problems
- [ ] @Transactional added for multi-step operations
- [ ] useEffect has cleanup function (React)
- [ ] No `any` types in TypeScript
- [ ] Documentation updated if needed

## Related Issues
Fixes #<issue-number>

## Screenshots (if applicable)
[Add screenshots for UI changes]

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

#### PR Review Requirements

**Before Merge, PR MUST have:**
1. ✅ All CI/CD checks passing
2. ✅ At least 1 code review approval
3. ✅ No merge conflicts with target branch
4. ✅ All conversations resolved
5. ✅ Tests covering new functionality (70%+ coverage maintained)
6. ✅ Documentation updated (if public API changed)

**Code Review Focus Areas:**
- [ ] **Naming conventions** (PascalCase, camelCase, snake_case per language)
- [ ] **JSON API consistency** (camelCase in all APIs)
- [ ] **Test location** (src/test/ for Java, tests/ for Python)
- [ ] **OpenTelemetry tracing** (business attributes added to spans)
- [ ] **Error handling** (GlobalExceptionHandler pattern followed)
- [ ] **Database patterns** (N+1 queries avoided, @Transactional used)
- [ ] **React patterns** (useEffect cleanup, no state mutation, no `any` types)
- [ ] **Security** (no hardcoded secrets, environment variables used)

---

### Deployment Workflow

**⚠️ CRITICAL: Deployment patterns for microservices.**

#### Environment Strategy

**Environments:**
1. **Local Development** - Developer machine with Docker Compose
2. **Staging** - Kubernetes cluster (mirror of production)
3. **Production** - Kubernetes cluster

**Environment Variables:**

```bash
# ✅ CORRECT: Environment-specific configuration
# Local (.env.local)
DATABASE_URL=postgresql://localhost:5432/bank_local
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Staging (Kubernetes ConfigMap/Secret)
DATABASE_URL=postgresql://postgres-service:5432/bank_staging
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4318
KAFKA_BOOTSTRAP_SERVERS=kafka-service:9092

# Production (Kubernetes ConfigMap/Secret)
DATABASE_URL=postgresql://postgres-service:5432/bank_prod
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4318
KAFKA_BOOTSTRAP_SERVERS=kafka-service:9092
```

**❌ WRONG: Hardcoded environment values in code**
```java
// NEVER do this!
private String databaseUrl = "postgresql://localhost:5432/bank";
```

#### Database Migration Strategy

**⚠️ MANDATORY: Use Flyway for database schema versioning.**

**Migration File Naming:**
```
V<version>__<description>.sql

Examples:
V1__create_accounts_table.sql
V2__add_currency_column.sql
V3__create_transactions_table.sql
V4__add_saga_execution_table.sql
```

**Migration Rules:**
- ✅ **NEVER** modify existing migration files (create new ones)
- ✅ **ALWAYS** test migrations on staging before production
- ✅ Use reversible migrations when possible
- ✅ Include rollback plan for breaking changes
- ❌ **NEVER** use Flyway in production for auto-migration without approval

**Deployment Order:**
1. Run Flyway migrations (manual approval in production)
2. Deploy backend services (rolling update)
3. Deploy frontend (blue-green deployment)
4. Verify health checks
5. Monitor observability stack (Grafana/Tempo/Loki)

---

### CI/CD Pipeline (Future Setup)

**⚠️ Recommended CI/CD workflow for this project.**

**Pipeline Stages:**

```yaml
# .github/workflows/ci.yml (example structure)

stages:
  - lint
  - test
  - build
  - deploy

lint:
  - Java: mvn checkstyle:check
  - Python: flake8, black --check, mypy
  - TypeScript: eslint, prettier --check

test:
  - Java: mvn test (JUnit + Testcontainers)
  - Python: pytest --cov=app tests/
  - TypeScript: vitest run --coverage
  - Coverage threshold: 70%

build:
  - Java: mvn clean package
  - Python: docker build
  - TypeScript: npm run build

deploy:
  - Staging: Auto-deploy on merge to main
  - Production: Manual approval required
```

**CI/CD Best Practices:**
- ✅ Run tests in parallel for faster feedback
- ✅ Cache dependencies (Maven, npm, pip)
- ✅ Fail fast on linting errors
- ✅ Require passing tests before merge
- ✅ Auto-deploy to staging on main branch
- ❌ **NEVER** auto-deploy to production without approval

---

### Code Review Workflow

**⚠️ MANDATORY: All code changes go through code review.**

**Code Review Process:**

1. **Create Feature Branch**
   ```bash
   git checkout -b feature/account-creation-api
   ```

2. **Implement Feature**
   - Follow all naming conventions
   - Write tests first (TDD when possible)
   - Add OpenTelemetry instrumentation
   - Update documentation

3. **Commit Changes**
   ```bash
   git add .
   git commit -m "feat(account-service): add account creation endpoint"
   ```

4. **Push and Create PR**
   ```bash
   git push -u origin feature/account-creation-api
   # Create PR via GitHub/GitLab UI
   ```

5. **Address Review Comments**
   - Respond to all review comments
   - Make requested changes
   - Push additional commits
   - Request re-review

6. **Merge PR**
   - Squash and merge (recommended) OR
   - Rebase and merge (for clean history)
   - ❌ **NEVER** use "Merge commit" (creates messy history)

7. **Delete Feature Branch**
   ```bash
   git branch -d feature/account-creation-api
   git push origin --delete feature/account-creation-api
   ```

---

### Hotfix Workflow

**⚠️ CRITICAL: Emergency fixes for production issues.**

**Hotfix Process:**

1. **Create Hotfix Branch from Main**
   ```bash
   git checkout main
   git pull
   git checkout -b fix/critical-kafka-trace-propagation
   ```

2. **Implement Minimal Fix**
   - Fix ONLY the critical issue
   - Add regression test
   - No refactoring or unrelated changes

3. **Fast-Track Review**
   - Create PR with `[HOTFIX]` prefix
   - Request immediate review
   - Merge after 1 approval (instead of usual 2)

4. **Deploy to Production Immediately**
   - Manual deployment with approval
   - Monitor observability stack closely

5. **Create Follow-Up Tasks**
   - Document root cause
   - Create issues for long-term fixes
   - Update runbooks

**Example Hotfix Commit:**
```
fix(kafka): [HOTFIX] propagate trace context in message headers

CRITICAL: Trace context was not propagating through Kafka messages,
breaking distributed tracing across services.

Added W3C TraceContext headers to all Kafka producers.

Tested in staging with full trace verification.

Fixes #156 (Production incident)

🤖 Generated with Claude Code
Co-Authored-By: Claude <noreply@anthropic.com>
```

---

### Versioning Strategy

**⚠️ Semantic Versioning for API compatibility.**

**Version Format:** `MAJOR.MINOR.PATCH`

- **MAJOR**: Breaking changes (incompatible API changes)
- **MINOR**: New features (backward-compatible)
- **PATCH**: Bug fixes (backward-compatible)

**Examples:**
- `1.0.0` → `1.0.1`: Bug fix (safe to deploy)
- `1.0.1` → `1.1.0`: New feature (safe to deploy)
- `1.1.0` → `2.0.0`: Breaking change (requires migration plan)

**API Versioning:**
```
/api/v1/accounts  ← Version in URL path
/api/v2/accounts  ← New version for breaking changes
```

**Rule:** Maintain backward compatibility within same major version.

---

### Documentation Requirements

**⚠️ MANDATORY: Keep documentation in sync with code.**

**Documentation Updates Required When:**
- [ ] Adding new API endpoint → Update OpenAPI spec
- [ ] Changing database schema → Update Flyway migration + architecture.md
- [ ] Adding new service → Update architecture.md + README.md
- [ ] Changing environment variables → Update .env.example + docs/
- [ ] Changing deployment process → Update deployment runbook
- [ ] Adding new OpenTelemetry attributes → Update docs/opentelemetry-conventions.md

**Critical Documentation Files:**
- `docs/architecture.md` - Architecture decisions
- `docs/project_context.md` - AI agent implementation rules (THIS FILE)
- `docs/opentelemetry-conventions.md` - OTel naming conventions
- `README.md` - Project setup and quick start
- `services/<service>/README.md` - Service-specific documentation

**Rule:** Documentation is part of "Definition of Done" for every story.

---

_All sections completed!_

- [x] Language-Specific Rules (Java, Python, TypeScript)
- [x] Framework-Specific Rules (Spring Boot, Quarkus, FastAPI, React)
- [x] Critical Don't-Miss Rules (Anti-Patterns)
- [x] Code Quality & Style Rules
- [x] Development Workflow Rules

---

_Last updated: 2025-12-09 by BMAD Generate Project Context workflow_
_Status: COMPLETE - All 5 rule categories generated_
