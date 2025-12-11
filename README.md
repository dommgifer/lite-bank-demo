# Lite Bank Demo

A microservices-based banking system demonstrating distributed transactions, event-driven architecture, and comprehensive observability with OpenTelemetry.

## 📋 Project Overview

Lite Bank Demo is a learning-focused microservices project showcasing:

- **Distributed Transactions**: SAGA pattern implementation for cross-service consistency
- **Event-Driven Architecture**: Kafka-based asynchronous communication
- **Full Observability**: Manual OpenTelemetry instrumentation with trace context propagation
- **Polyglot Microservices**: Java (Spring Boot, Quarkus), Python (FastAPI), TypeScript (React)
- **Modern DevOps**: Docker, Kubernetes, Flyway migrations

## 🏗️ Architecture

### Microservices (10 Services)

#### Java Services (Spring Boot 3.4.x)
- **Account Service** - Account management and balance operations
- **User Service** - User authentication and profile management
- **Audit Service** - Audit logging and compliance tracking
- **Statement Service** - Account statement generation

#### Java Services (Quarkus 3.15.x LTS)
- **Notification Service** - Email/SMS notifications
- **Report Service** - Reporting and analytics
- **Compliance Service** - Regulatory compliance checks

#### Python Services (FastAPI 0.115.0)
- **Transfer Service** - Money transfer orchestration (SAGA coordinator)
- **Exchange Rate Service** - Currency conversion and FX rates
- **Analytics Service** - Real-time analytics and metrics

#### Frontend
- **React 18 + Vite 6** - Single-page application with Material-UI v5

### Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Backend (Java) | Spring Boot | 3.4.x |
| Backend (Java) | Quarkus | 3.15.x LTS |
| Backend (Python) | FastAPI | 0.115.0 |
| Frontend | React + Vite | 18 + 6.x |
| Database | PostgreSQL | 16.x |
| Message Broker | Apache Kafka | 3.9.x |
| Observability | OpenTelemetry | Java 1.57.0, Python 1.39.0, JS 2.x |
| Observability Stack | Grafana + Tempo + Loki | Latest |
| Container Runtime | Docker | Latest |
| Orchestration | Kubernetes | 1.28+ |

## 🚀 Quick Start

### Prerequisites

- **Java 21** (for Spring Boot & Quarkus services)
- **Python 3.11+** (for FastAPI services)
- **Node.js 20.x LTS** (for React frontend)
- **Docker & Docker Compose** (for local development)
- **Maven 3.9+** (for Java builds)

### Local Development Setup

1. **Clone the repository**
   ```bash
   git clone git@github.com:dommgifer/lite-bank-demo.git
   cd lite-bank-demo
   ```

2. **Start infrastructure services** (PostgreSQL, Kafka, OpenTelemetry Collector)
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

   **Java (Spring Boot) services:**
   ```bash
   cd services/account-service
   mvn spring-boot:run
   ```

   **Java (Quarkus) services:**
   ```bash
   cd services/notification-service
   mvn quarkus:dev
   ```

   **Python (FastAPI) services:**
   ```bash
   cd services/transfer-service
   pip install -r requirements.txt
   uvicorn app.main:app --reload
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
├── services/                   # Microservices
│   ├── account-service/        # Spring Boot - Account management
│   ├── user-service/           # Spring Boot - User management
│   ├── transfer-service/       # FastAPI - Transfer orchestration (SAGA)
│   ├── notification-service/   # Quarkus - Notifications
│   ├── exchange-rate-service/  # FastAPI - FX rates
│   ├── audit-service/          # Spring Boot - Audit logging
│   ├── statement-service/      # Spring Boot - Statement generation
│   ├── report-service/         # Quarkus - Reporting
│   ├── analytics-service/      # FastAPI - Analytics
│   └── compliance-service/     # Quarkus - Compliance checks
├── frontend/                   # React + Vite frontend
├── infrastructure/             # Kubernetes manifests & Helm charts
├── docs/                       # Documentation
│   ├── architecture.md         # Architecture decisions (ADR)
│   └── project_context.md      # AI agent implementation rules
├── scripts/                    # Deployment & utility scripts
├── docker-compose.yml          # Local development infrastructure
└── README.md                   # This file
```

## 🔄 SAGA Pattern Implementation

The **Transfer Service** (Python FastAPI) orchestrates distributed transactions using the SAGA pattern:

1. **Freeze Source Account** (Account Service)
2. **Freeze Destination Account** (Account Service)
3. **Check Compliance** (Compliance Service)
4. **Execute FX Conversion** (Exchange Rate Service)
5. **Debit Source Account** (Account Service)
6. **Credit Destination Account** (Account Service)
7. **Send Notification** (Notification Service)
8. **Record Audit Log** (Audit Service)

**Compensation logic** is triggered automatically if any step fails, ensuring data consistency across services.

## 📊 Observability

### OpenTelemetry Manual Instrumentation

All services use **manual instrumentation** to capture:

- **Distributed Traces**: Full request flow across all microservices
- **Business Attributes**: Custom span attributes (account.id, transaction.amount, saga.step)
- **Trace Context Propagation**: W3C TraceContext headers in HTTP & Kafka messages

### Observability Stack

- **Grafana**: Dashboards and visualization
- **Tempo**: Distributed tracing backend
- **Loki**: Log aggregation
- **Prometheus**: Metrics collection (future)

Access Grafana at http://localhost:3000 after starting `docker-compose`.

## 🧪 Testing

### Running Tests

**Java Services (JUnit 5 + Testcontainers):**
```bash
cd services/account-service
mvn test
```

**Python Services (pytest):**
```bash
cd services/transfer-service
pytest --cov=app tests/
```

**Frontend (Vitest + React Testing Library):**
```bash
cd frontend
npm run test
```

### Test Coverage

- Target: **70%+ code coverage** for all services
- Integration tests use **Testcontainers** for PostgreSQL and Kafka

## 🔐 Security

- **No hardcoded credentials**: All secrets in environment variables
- **Business IDs**: No database auto-increment IDs exposed in APIs
- **Input validation**: Pydantic models (Python), Bean Validation (Java)
- **Error handling**: Centralized exception handlers prevent information leakage

## 📖 Documentation

- **[Architecture Decisions](docs/architecture.md)** - Complete ADR with technology choices
- **[Project Context](docs/project_context.md)** - AI agent implementation rules and coding standards
- **API Documentation** - OpenAPI/Swagger available at `/api/docs` for each service

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
- `feat(account-service): add account creation endpoint`
- `fix(kafka): propagate trace context in message headers`
- `refactor(otel): extract common OpenTelemetry config`

## 📝 License

This project is for educational purposes only.

## 🤝 Contributing

This is a learning project. Contributions are welcome for:

- Bug fixes
- Documentation improvements
- Test coverage improvements
- Performance optimizations

Please follow the coding standards in [docs/project_context.md](docs/project_context.md).

## 📧 Contact

For questions or discussions, please open an issue on GitHub.

---

**Built with ❤️ using Claude Code**

_Last updated: 2025-12-11_
