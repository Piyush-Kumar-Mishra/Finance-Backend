# Finance Dashboard Backend

A clean Kotlin backend service built with Ktor for finance record management, role-based access control, dashboard summaries, and JWT-based authentication. [API documentation↗](https://documenter.getpostman.com/view/50647382/2sBXiqDTjE)

## What This Backend Showcases

- Layered backend structure with routes, services, repositories, models, and security separated clearly
- JWT authentication with refresh token sessions
- Role-based access control for viewer, editor, and admin users
- Financial records CRUD with filtering
- Dashboard summary and trend APIs
- Validation and error handling using consistent HTTP responses

## Technical Architecture

### Layered Architecture Pattern

```text
-------------------------------------------------------------
Presentation Layer
- Auth Routes
- Record Routes
- Dashboard Routes

Business Layer
- Auth Service
- User Record Service
- Dashboard Service

Data Access Layer
- User Repository
- User Record Repository
- User Session Repository
-------------------------------------------------------------
```

### Package Structure

```text
src/main/kotlin/
|- Application.kt                 # Application wiring and dependency setup
|- db/
|  |- DatabaseFactory.kt          # Database connection setup
|  |- schemas/                    # Table definitions and enums
|- dto/                           # Request and response models
|- model/                         # Domain models
|- repository/                    # Database access layer
|- routes/                        # Route handlers
|- security/                      # JWT, password hashing, access control, rate limiting
|- service/                       # Core business logic
|- utils/                         # Shared exceptions and helpers
```

## Core Features and Capabilities

### Authentication and Security

- User signup and login
- Refresh token flow with server-side session validation
- Logout support
- Single active refresh session per user
- admin signup endpoint for first admin creation
- JWT-protected routes using access tokens
- BCrypt password hashing
- Custom validation and error messages
- Auth route rate limiting for:
  - `POST /account/signup`
  - `POST /account/login`
  - `POST /account/refresh`

### User and Role Management

- Users have both `role` and `status`
- Supported roles:
  - `VIEWER`
  - `EDITOR`
  - `ADMIN`
- Admin can update another user's role, status

### Financial Records Management

- Create records
- View all records
- View record by id
- Update records
- Delete records
- Supported record types:
  - `INCOME`
  - `EXPENSE`
  - `LOAN`
- Filter records by:
  - `type`
  - `category`
  - `date`
  - `fromDate`
  - `toDate`

### Dashboard APIs

- Total income
- Total expense
- Net balance
- Category-wise totals
- Recent activity showing last three records
- Monthly trends
- Weekly trends

### Validation and Error Handling

- Invalid email validation
- Password validation
- Invalid role and status validation
- Invalid record type validation
- Invalid date validation
- Proper `400`, `401`, `403`, `404`, `409`, and `429` responses where needed
- Centralized error mapping through `StatusPages`

## Role-Based Access Rules

| Role | Can View Records | Can Create Records | Can Update Records | Can Delete Records | Can View Dashboard | Can Manage Users |
|------|------------------|--------------------|--------------------|--------------------|-------------------|------------------|
| `VIEWER` | Yes | No | No | No | Yes | No |
| `EDITOR` | Yes | Yes | Yes | No | Yes | No |
| `ADMIN` | Yes | Yes | Yes | Yes | Yes | Yes |

Inactive users are blocked from protected business actions.

## Technology Stack

### Core Framework

| Technology | Version | Purpose |
|------------|---------|---------|
| Kotlin | 2.3.0 | Primary backend language |
| Java | 21 | Runtime and language level |
| Ktor | 3.4.2 | HTTP server framework |
| kotlinx.serialization | 1.6.3 | JSON serialization |

### Database and Persistence

| Technology | Version | Purpose |
|------------|---------|---------|
| MySQL | 8.x | Relational database |
| Exposed | 0.61.0 | SQL DSL and data access |

### Security

| Technology | Version | Purpose |
|------------|---------|---------|
| Ktor Auth | 3.4.2 | Route authentication |
| Ktor JWT | 3.4.2 | JWT validation |
| jBCrypt | 0.4 | Password hashing |

### Testing

| Technology | Version | Purpose |
|------------|---------|---------|
| kotlin.test | Kotlin 2.3.0 | Assertions and unit tests |
| Ktor Test Host | 3.4.2 | Route and application testing |

## Development Setup

### Prerequisites

- Java 21 or higher
- MySQL installed locally
- Database created manually before running the app
- Gradle wrapper included in the project

### Quick Start

```bash
# Clone the repository
git clone <https://github.com/Piyush-Kumar-Mishra/Finance-Backend.git>
cd finance_backend

# Create database tables manually in MySQL
# Add environment variables in .env

# Run tests
./gradlew test

# Run the server
./gradlew run
```

Base URL:

```text
http://localhost:8080
```

## Environment Configuration

Create a `.env` file in the project root:

```properties
DB_URL=jdbc:mysql://localhost:3306/finance_db
DB_USER=root
DB_PASSWORD= password
DB_DRIVER=com.mysql.cj.jdbc.Driver

JWT_SECRET=your_secret_key
JWT_ISSUER=your_app
JWT_AUDIENCE=your_users
JWT_ACCESS_EXPIRATION=30
JWT_REFRESH_EXPIRATION=10
```

Notes:

- `JWT_ACCESS_EXPIRATION` is in minutes
- `JWT_REFRESH_EXPIRATION` is in days
## Database Schema

Run these SQL statements before starting the backend:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    role ENUM('ADMIN', 'VIEWER', 'EDITOR') DEFAULT 'VIEWER'
);

CREATE TABLE user_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    refresh_token_hash VARCHAR(200) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE user_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type ENUM('INCOME', 'EXPENSE', 'LOAN') NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    category VARCHAR(50),
    date DATE NOT NULL,
    description VARCHAR(100),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

### Authentication Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/account/signup` | Register a new viewer user |
| `POST` | `/account/admin/signup` | Create the first admin user |
| `POST` | `/account/login` | Login with email and password |
| `POST` | `/account/refresh` | Get a new access and refresh token |
| `POST` | `/account/logout` | Logout and delete active session |

### User Management Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `PATCH` | `/users/{id}/role` | Update another user's role, admin only |
| `PATCH` | `/users/{id}/status` | Update another user's status, admin only |

### Record Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/records` | Get records with optional filters |
| `GET` | `/records/{id}` | Get record by id |
| `POST` | `/records` | Create record |
| `PUT` | `/records/{id}` | Update record |
| `DELETE` | `/records/{id}` | Delete record |

### Dashboard Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/dashboard/summary` | Get total income, expense, and net balance |
| `GET` | `/dashboard/category-totals` | Get totals grouped by category |
| `GET` | `/dashboard/recent-activity` | Get last three records |
| `GET` | `/dashboard/monthly-trends` | Get month-wise income, expense, and net balance |
| `GET` | `/dashboard/weekly-trends` | Get week-wise income, expense, and net balance |

## Request and Response Examples

### Signup Request

```json
{
  "email": "john@example.com",
  "password": "abc123"
}
```

### Login Response

```json
{
  "accessToken": "your_access_token",
  "refreshToken": "your_refresh_token"
}
```

### Update User Role Request

```json
{
  "role": "EDITOR"
}
```

### Update User Status Request

```json
{
  "status": "INACTIVE"
}
```

### Create Record Request

```json
{
  "type": "INCOME",
  "amount": 50000.0,
  "category": "Salary",
  "date": "2026-04-04",
  "description": "Monthly salary"
}
```

### Records Filter Example

```text
GET /records?type=EXPENSE&category=Food&fromDate=2026-04-01&toDate=2026-04-10
```

### Dashboard Summary Response

```json
{
  "totalIncome": 2500.0,
  "totalExpense": 1200.0,
  "netBalance": 1300.0
}
```

### Monthly Trend Response

```json
{
  "trends": [
    {
      "period": "2026-03",
      "totalIncome": 4000.0,
      "totalExpense": 2200.0,
      "netBalance": 1800.0
    },
    {
      "period": "2026-04",
      "totalIncome": 5000.0,
      "totalExpense": 1200.0,
      "netBalance": 1300.0
    }
  ]
}
```

## Authentication and Session Flow

### Authentication Flow

1. User signs up or logs in
2. Backend validates credentials and user status
3. Backend creates one active refresh session for that user
4. Backend returns:
   - short-lived access token
   - refresh token tied to the stored session
5. Protected routes validate the access token and active session

### Session Rules

- only one refresh session is kept active for each user
- logging in again replaces the previous refresh session
- logout deletes the session
- expired refresh token deletes the session and forces login again


### Rate Limiting

Rate limiting is applied to `POST /account/signup`, `POST /account/login`, and `POST /account/refresh`. The backend stores rate-limit entries in memory using a `ConcurrentHashMap<String, AuthRateLimitState>`. The key format combines route action and client id, for example `LOGIN:127.0.0.1`, and each value stores `requestCount`, `windowEndsAt`, and `blockedUntil`. If a client exceeds the allowed number of requests for one of these routes, the backend returns `429 Too Many Requests` with a message telling the client to try again after 5 minutes.

Current limits:

- signup: 5 attempts
- login: 5 attempts
- refresh: 10 attempts

## Validation and Error Handling

Examples of handled cases:

- invalid email format
- duplicate email signup
- invalid password format
- invalid refresh token
- expired refresh token
- invalid role or status
- invalid record id
- invalid record type
- invalid date filters
- forbidden actions based on role
- rate limit exceeded
