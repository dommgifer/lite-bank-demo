# Lite Bank Demo

A microservices-based banking system demonstrating coordination layer architecture, distributed transactions, and comprehensive observability with OpenTelemetry.

## 📋 Project Overview

Lite Bank Demo is a learning-focused microservices project showcasing:

- **Coordination Layer Architecture**: Clear separation between business orchestration and data operations
- **Distributed Transactions**: SAGA pattern with coordination services orchestrating data layer operations
- **Full Observability**: Manual OpenTelemetry instrumentation with complete trace context propagation
- **Unified Technology Stack**: Java 21 + Spring Boot 3.x for all microservices
- **Modern DevOps**: Docker, Kubernetes, Flyway migrations

## 🏗️ Architecture

### Coordination Layer Pattern

This project implements a **coordination layer architecture** where:
- **Coordination Layer Services** (3): Handle business logic and orchestrate operations
- **Data Layer Services** (4): Provide data access and core operations
- **Transaction Service**: The ONLY service that can modify account balances

### Microservices (7 Services)

#### Data Layer Services (Java Spring Boot 3.4.x)
- **User Service** - User authentication and profile management
- **Account Service** - Account queries and validation (read-only)
- **Transaction Service** - Core financial operations (ONLY service that modifies balances)
- **Exchange Rate Service** - Currency conversion rates (mock)

#### Coordination Layer Services (Java Spring Boot 3.4.x)
- **Deposit-Withdrawal Service** - Orchestrates deposit/withdrawal operations
- **Transfer Service** - Orchestrates money transfers (SAGA coordinator)
- **Exchange Service** - Orchestrates currency exchange operations

#### Frontend
- **React 18 + Vite 6** - Single-page application with Material-UI v5

### Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Backend | Java + Spring Boot | 21 + 3.4.x |
| Frontend | React + Vite | 18 + 6.x |
| Database | PostgreSQL | 16.x |
| Observability | OpenTelemetry | Java SDK 1.57.0 |
| Observability Stack | Grafana + Tempo + Loki | Latest |
| Container Runtime | Docker | Latest |
| Orchestration | Kubernetes | 1.28+ |

## 🚀 Quick Start

### Prerequisites

- **Java 21** (for all Spring Boot services)
- **Node.js 20.x LTS** (for React frontend)
- **Docker & Docker Compose** (for local development)
- **Maven 3.9+** (for Java builds)

### Local Development Setup

1. **Clone the repository**
   ```bash
   git clone git@github.com:dommgifer/lite-bank-demo.git
   cd lite-bank-demo
   ```

2. **Start infrastructure services** (PostgreSQL, Grafana Stack, OpenTelemetry Collector)
   ```bash
   docker-compose up -d
   ```

3. **Run database migrations**
   ```bash
   # For each Java service
   cd services/account-service
   mvn flyway:migrate
   ```

4. **Start backend services**

   **All services use Spring Boot:**
   ```bash
   # Data Layer Services
   cd services/user-service && mvn spring-boot:run
   cd services/account-service && mvn spring-boot:run
   cd services/transaction-service && mvn spring-boot:run
   cd services/exchange-rate-service && mvn spring-boot:run

   # Coordination Layer Services
   cd services/deposit-withdrawal-service && mvn spring-boot:run
   cd services/transfer-service && mvn spring-boot:run
   cd services/exchange-service && mvn spring-boot:run
   ```

5. **Start frontend**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

6. **Access the application**
   - Frontend: http://localhost:5173
   - Grafana (Observability): http://localhost:3000

## 📁 Project Structure

```
lite-bank-demo/
├── services/                         # Microservices
│   ├── user-service/                 # Data Layer - User management & auth
│   ├── account-service/              # Data Layer - Account queries (read-only)
│   ├── transaction-service/          # Data Layer - Financial operations (ONLY modifies balances)
│   ├── exchange-rate-service/        # Data Layer - FX rates (mock)
│   ├── deposit-withdrawal-service/   # Coordination Layer - Deposit/Withdrawal orchestration
│   ├── transfer-service/             # Coordination Layer - Transfer orchestration (SAGA)
│   └── exchange-service/             # Coordination Layer - Exchange orchestration
├── frontend/                         # React + Vite frontend
├── infrastructure/                   # Kubernetes manifests & Helm charts
├── docs/                             # Documentation
│   ├── architecture.md               # Architecture decisions (ADR)
│   ├── prd.md                        # Product requirements document
│   └── epics.md                      # Epic and story breakdown
├── scripts/                          # Deployment & utility scripts
├── docker-compose.yml                # Local development infrastructure
└── README.md                         # This file
```

## 🔄 Coordination Layer Architecture

### SAGA Pattern Implementation

The **Transfer Service** (Coordination Layer) orchestrates distributed transactions:

1. **Validate Source Account** → calls Account Service
2. **Validate Destination Account** → calls Account Service
3. **Debit from Source** → calls Transaction Service
4. **Credit to Destination** → calls Transaction Service
5. **Record Transfer Transactions** → calls Transaction Service

**Key Principles:**
- **Only Transaction Service modifies balances**: All coordination services must call Transaction Service for money operations
- **Account Service is read-only**: Only provides queries and validation
- **Compensation logic**: Handled by coordination layer, reverses operations via Transaction Service

## 📊 Observability

### OpenTelemetry Manual Instrumentation

All services use **manual instrumentation** to capture:

- **Distributed Traces**: Complete trace propagation from coordination layer → data layer
- **Business Attributes**: Custom span attributes (account.id, transaction.amount, transfer.amount)
- **Trace Context Propagation**: W3C TraceContext headers (`traceparent`) in all HTTP calls

### Observability Stack

- **Grafana**: Dashboards and visualization
- **Tempo**: Distributed tracing backend
- **Loki**: Log aggregation with structured JSON logs
- **OpenTelemetry Collector**: Centralized data collection

Access Grafana at http://localhost:3000 after starting `docker-compose`.

### Trace Visualization

View complete service interaction traces:
- Coordination Layer → Account Service (validation)
- Coordination Layer → Transaction Service (money operations)
- All spans include business context for fast debugging

## 🧪 Testing

### Running Tests

**Java Services (JUnit 5 + Testcontainers):**
```bash
cd services/account-service
mvn test
```

**Frontend (Vitest + React Testing Library):**
```bash
cd frontend
npm run test
```

### Test Coverage

- Target: **70%+ code coverage** for all services
- Integration tests use **Testcontainers** for PostgreSQL

## 🔐 Security

- **No hardcoded credentials**: All secrets in environment variables
- **Business IDs**: No database auto-increment IDs exposed in APIs
- **Input validation**: Bean Validation (Java)
- **Error handling**: Centralized exception handlers prevent information leakage

## 📖 Documentation

- **[Architecture Decisions](docs/architecture.md)** - Complete ADR with coordination layer architecture
- **[Product Requirements](docs/prd.md)** - Detailed PRD with user stories and requirements
- **[Epic Breakdown](docs/epics.md)** - Epic and story breakdown for implementation
- **API Documentation** - OpenAPI/Swagger available at `/actuator/swagger-ui` for each service

## 🛠️ Development Workflow

### Branch Naming Convention

```
feature/<description>  # New features
fix/<description>      # Bug fixes
refactor/<description> # Code refactoring
docs/<description>     # Documentation updates
```

### Commit Message Format (Conventional Commits)

```
<type>(<scope>): <short summary>

<optional body>

🤖 Generated with Claude Code
Co-Authored-By: Claude <noreply@anthropic.com>
```

**Examples:**
- `feat(account-service): add account validation endpoint`
- `fix(transfer-service): fix compensation logic for failed transfers`
- `refactor(otel): extract common OpenTelemetry config`

## 📝 License

This project is for educational purposes only.

## 🤝 Contributing

This is a learning project. Contributions are welcome for:

- Bug fixes
- Documentation improvements
- Test coverage improvements
- Performance optimizations

Please follow the architecture principles in [docs/architecture.md](docs/architecture.md).

## 📧 Contact

For questions or discussions, please open an issue on GitHub.

---

**Built with ❤️ using Claude Code**

_Last updated: 2025-12-19_
