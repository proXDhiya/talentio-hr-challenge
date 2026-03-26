# HR Management API

A RESTful HR management system built with Spring Boot. Handles employee lifecycle, role-based access control, and leave request workflows including submission, approval, and rejection.

---

## Folder Structure

```
src/main/java/me/dhiya/hr/
├── config/           # Security filter chain, JWT filter, and JWT properties
├── controllers/      # REST controllers - one per resource (auth, employees, leave requests, currencies)
├── domain/           # JPA entities and enums (Role, LeaveType, LeaveStatus, EmployeeStatus)
├── dto/              # Request and response DTOs grouped by domain (auth, employee, leave, common)
├── exception/        # GlobalExceptionHandler and BusinessException
├── mappers/          # Mapper interfaces and implementations (entity → DTO)
├── repositories/     # Spring Data repositories and projection interfaces for native queries
├── seed/             # DataSeeder - seeds currencies (DZD, EUR, USD) on first startup
├── services/         # Service interfaces and implementations - all business logic lives here
└── util/             # Shared constants: validation patterns, Swagger example payloads
```

---

## Database Schema

### `currencies`
| Column | Type | Notes |
|--------|------|-------|
| id | varchar | PK, UUID |
| code | varchar(3) | Unique (e.g. USD) |
| name | varchar | |
| symbol | varchar | |
| created_at | timestamptz | |
| updated_at | timestamptz | |

### `employees`
| Column | Type | Notes |
|--------|------|-------|
| id | varchar | PK, UUID |
| first_name | varchar(50) | |
| last_name | varchar(50) | |
| email | varchar | Unique |
| password | varchar | BCrypt hashed |
| role | varchar | `EMPLOYEE`, `MANAGER`, `HR` |
| department | varchar | |
| position | varchar | |
| salary | numeric(10,2) | |
| currency_id | varchar | FK → currencies |
| manager_id | varchar | FK → employees (self-reference) |
| hire_date | date | |
| annual_leave_days | int | Default: 30 |
| status | varchar | `ACTIVE`, `INACTIVE` |
| created_at | timestamptz | |
| updated_at | timestamptz | |
| deleted_at | timestamptz | Set on deactivation |

### `leave_requests`
| Column | Type | Notes |
|--------|------|-------|
| id | varchar | PK, UUID |
| employee_id | varchar | FK → employees |
| start_date | date | |
| end_date | date | |
| type | varchar | `ANNUAL`, `SICK`, `UNPAID` |
| status | varchar | `PENDING`, `APPROVED`, `REJECTED` |
| reason | text | Optional |
| review_comment | text | Set on approval/rejection |
| reviewed_by | varchar | FK → employees |
| created_at | timestamptz | |
| updated_at | timestamptz | |

---

## Endpoints

> ⚠️ **Start here:** call `POST /apis/v1/auth/setup` first to create the initial MANAGER account. This endpoint is only available once.

### Authentication
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/apis/v1/auth/setup` | None | One-time system bootstrap - creates the first MANAGER |
| POST | `/apis/v1/auth/login` | None | Login and receive a JWT Bearer token |

### Currencies
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/apis/v1/currencies` | MANAGER, HR | List all supported currencies |

### Employees
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/apis/v1/employees` | MANAGER, HR | Create a new employee |
| GET | `/apis/v1/employees` | MANAGER, HR | List employees with filters and pagination |
| GET | `/apis/v1/employees/me` | Any | Get own profile and leave balance |
| GET | `/apis/v1/employees/{id}` | MANAGER, HR | Get a specific employee's profile |
| PUT | `/apis/v1/employees/{id}` | MANAGER, HR | Partially update an employee |
| DELETE | `/apis/v1/employees/{id}` | MANAGER, HR | Deactivate (soft-delete) an employee |

### Leave Requests
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/apis/v1/leave-requests` | Any | Submit a new leave request |
| GET | `/apis/v1/leave-requests` | Any | List leave requests with filters and pagination |
| GET | `/apis/v1/leave-requests/{id}` | Any* | Get a leave request by ID |
| PATCH | `/apis/v1/leave-requests/{id}/approve` | MANAGER, HR | Approve a pending leave request |
| PATCH | `/apis/v1/leave-requests/{id}/reject` | MANAGER, HR | Reject a pending leave request |

---

## Running Locally

### Prerequisites

- Java 21+
- Maven
- Docker

### 1. Environment Setup

Copy the example below into a `.env` file at the project root:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/hr
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_DATASOURCE_DRIVER=org.postgresql.Driver

POSTGRES_DB=hr
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
```

### 2. Start the Database Only (dev)

Spins up only the PostgreSQL container so you can run the app locally via Maven:

```bash
docker compose -f docker-compose.dev.yml up -d
./mvnw spring-boot:run
```

### 3. Run Everything with Docker

Builds the app image and starts both the database and the application:

```bash
docker compose up --build
```

> **Note:** The Docker setup here is intended for local testing only. It is not production-ready. Due to time constraints, we did not get to implement a proper CI pipeline, production-grade Docker configuration, or any deployment setup. That would be the natural next step.

---

## Swagger UI

Interactive API documentation is available at:

```
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON spec:

```
http://localhost:8080/apis/v1/api-docs
```

---

## Conclusion

This project took approximately **20 hours** of focused work - 14 hours on 25/03 and 6 hours on 26/03.

### What I would add with more time

- **Department and team tables** - replace free-text department strings with proper relational entities to support team management and org structure
- **Roles table** - make roles configurable rather than a fixed enum, allowing custom roles with fine-grained permissions
- **Anonymization process** - automatically anonymize deactivated employee records after 30 days by removing personal data (name, email, etc.) while keeping the records for analytics and audit history
- **Monitoring and observability** - integrate a proper logging framework (structured logs), metrics (Micrometer + Prometheus), and distributed tracing
- **CI/CD pipeline** - automated testing, Docker image builds, and deployment on every push
- **Production-grade Docker setup** - secrets management, environment-specific configs, health checks, and a proper container orchestration setup

### Final note

This effort may not be perfect and there is always room to improve - but this was my honest result after learning Spring Boot for 3 weeks. I gave it everything I had within the time available and I hope it reflects both the effort and the progress made.
