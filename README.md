# PashuMandi Backend - Enterprise Authentication & User Management

[![Java](https://img.shields.io/badge/Java-17%20%7C%2019%20LTS-ED8B00?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6.x-6DB33F?logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![JWT](https://img.shields.io/badge/JWT-JJWT%200.11.5-000000?logo=jsonwebtokens&logoColor=white)](https://jwt.io/)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0%20%2F%20Swagger-85EA2D?logo=swagger&logoColor=black)](http://localhost:8080/swagger-ui.html)

---

## 1. Project Overview

**PashuMandi** is a production-oriented digital marketplace connecting buyers and sellers for cattle (Cows, Buffaloes, Goats) and dairy products. 

This repository houses the **core authentication and user profile subsystem** (`pashumandi-backend`), engineered with enterprise clean architecture to serve as the security foundation for all future modules (Livestock Listings, Milk Market, Order Processing, Real-time Chat, Reviews, and Payments).

### Key Features
- **Registration**: Indian phone number validation (10-digit), unique email check, and automatic assignment of default role `BUYER` and status `ACTIVE`.
- **Dual-Identifier Login**: Authenticate seamlessly using either registered mobile number or email.
- **Stateless JWT Security**:
  - Cryptographically signed HMAC-SHA256 Access Tokens (15-minute validity).
  - High-entropy UUID database-backed Refresh Tokens (7-day validity).
  - Refresh Token Rotation with automatic revocation of compromised token chains.
- **Secure Logout**: Instant revocation of database-backed refresh tokens across current or all user sessions.
- **Profile Management**: Retrieve and update profile details (Name, Address, Village, District, PIN code) without exposing password hashes.
- **Password Security**: BCrypt password hashing (strength 12) with full session invalidation upon password reset.
- **Role-Based Access Control (RBAC)**: Clear role boundaries (`BUYER`, `SELLER`, `ADMIN`) and account status validation (`ACTIVE`, `INACTIVE`, `BLOCKED`, `SUSPENDED`).
- **Standardized API Envelopes**: Consistent success and error JSON envelopes with no raw stack traces or internal leaks.

---

## 2. Technology Stack

| Layer | Technology |
|---|---|
| **Language** | Java 17 / 19 LTS |
| **Framework** | Spring Boot 3.2.5 (Spring MVC, Spring Data JPA, Spring Security 6) |
| **Database** | MySQL 8.0+ (InnoDB Engine, utf8mb4 character set) |
| **Database Migration**| Versioned SQL Migrations (`src/main/resources/db/migration/`) |
| **Security & Tokens** | Spring Security 6, JJWT (io.jsonwebtoken 0.11.5), BCrypt |
| **Validation** | Jakarta Bean Validation (Hibernate Validator) |
| **Documentation** | SpringDoc OpenAPI 3.0 / Swagger UI |
| **Build & Tooling** | Apache Maven 3.9+, Lombok |
| **Testing** | JUnit 5, Mockito, Spring Boot Test, MockMvc, H2 In-Memory DB |

---

## 3. Project Directory Structure

```
pashumandi-backend/
├── pom.xml
├── .gitignore
├── .env.example
├── README.md
├── src/
│   ├── main/
│   │   ├── java/com/pashumandi/
│   │   │   ├── PashuMandiApplication.java       # Application entry point
│   │   │   ├── config/                          # Security, JWT, CORS & Swagger configs
│   │   │   │   ├── CorsConfig.java
│   │   │   │   ├── JwtConfig.java
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── controller/                      # REST API Endpoints
│   │   │   │   ├── AuthController.java
│   │   │   │   └── UserController.java
│   │   │   ├── dto/                             # Data Transfer Objects
│   │   │   │   ├── auth/                        # Register, Login, Refresh, AuthResponse
│   │   │   │   ├── user/                        # Profile, Update, ChangePassword
│   │   │   │   └── common/                      # Standard ApiResponse & ErrorResponse
│   │   │   ├── entity/                          # JPA Entities
│   │   │   │   ├── User.java
│   │   │   │   └── RefreshToken.java
│   │   │   ├── enums/                           # Domain Enums
│   │   │   │   ├── UserRole.java
│   │   │   │   └── UserStatus.java
│   │   │   ├── exception/                       # Custom Exceptions & Global Handler
│   │   │   │   ├── AccountStatusException.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── InvalidCredentialsException.java
│   │   │   │   ├── InvalidPasswordException.java
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   ├── TokenRefreshException.java
│   │   │   │   └── UserAlreadyExistsException.java
│   │   │   ├── repository/                      # Spring Data Repositories
│   │   │   │   ├── RefreshTokenRepository.java
│   │   │   │   └── UserRepository.java
│   │   │   ├── security/                        # Filter, EntryPoint, UserPrincipal, JwtService
│   │   │   │   ├── CustomAccessDeniedHandler.java
│   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   ├── JwtAuthenticationEntryPoint.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── JwtService.java
│   │   │   │   └── UserPrincipal.java
│   │   │   └── service/                         # Business Logic Layer
│   │   │       ├── AuthService.java
│   │   │       └── UserService.java
│   │   └── resources/
│   │       ├── application.yml                  # Externalized application configuration
│   │       └── db/
│   │           ├── schema.sql                   # Standalone database and table initialization
│   │           └── migration/                   # Versioned migration scripts
│   │               ├── V1__create_users_table.sql
│   │               └── V2__create_refresh_tokens_table.sql
│   └── test/
│       ├── java/com/pashumandi/
│       │   ├── controller/                      # MockMvc Integration Tests
│       │   │   ├── AuthControllerTest.java
│       │   │   └── UserControllerTest.java
│       │   └── security/                        # JWT & Security Tests
│       │       ├── JwtServiceTest.java
│       │       └── SecurityRuleTest.java
│       └── resources/
│           └── application.yml                  # Hermetic test configuration (H2)
```

---

## 4. Environment Variables

All sensitive values and credentials are strictly injected via environment variables.

| Variable | Description | Example Value |
|---|---|---|
| `DB_URL` | MySQL JDBC connection string | `jdbc:mysql://localhost:3306/pashumandi?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` |
| `DB_USERNAME` | MySQL database username | `root` |
| `DB_PASSWORD` | MySQL user password | `YourSecretPassword` |
| `JWT_SECRET` | 256-bit Base64-encoded secret key | `dGVzdF9zZWNyZXRfa2V5X2Zvcl9wYXNodW1hbmRpX2F1dGhlbnRpY2F0aW9uX3N5c3RlbV8yNTZiaXRzIQ==` |
| `JWT_ACCESS_EXPIRATION` | Access token lifetime in milliseconds | `900000` (15 mins) |
| `JWT_REFRESH_EXPIRATION` | Refresh token lifetime in milliseconds | `604800000` (7 days) |
| `FRONTEND_URL` | Allowed CORS frontend origins (comma-separated) | `http://localhost:5173` |

> **Security Note:** Never commit `.env` or plain credentials to version control. Use `.env.example` as a template.

---

## 5. Database Setup

### Option A: Direct MySQL Execution
Run the provided SQL script `src/main/resources/db/schema.sql` in MySQL Workbench or CLI:

```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

This creates:
1. `pashumandi` database (UTF8mb4).
2. `users` table with constraints and indexes.
3. `refresh_tokens` table with foreign key relationship to `users(id)`.

### Option B: Automatic Hibernate Sync (Local Development)
When the Spring Boot application boots with `spring.jpa.hibernate.ddl-auto: update`, Hibernate automatically syncs the schema based on `@Entity` definitions.

---

## 6. How to Build and Run

### 1. Build and Run Tests
```bash
mvn clean test
```
The test suite uses an in-memory H2 database (`MODE=MySQL`), executing hermetically without requiring a live MySQL instance.

### 2. Set Environment Variables
In PowerShell:
```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/pashumandi?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
$env:JWT_SECRET="dGVzdF9zZWNyZXRfa2V5X2Zvcl9wYXNodW1hbmRpX2F1dGhlbnRpY2F0aW9uX3N5c3RlbV8yNTZiaXRzIQ=="
$env:FRONTEND_URL="http://localhost:5173"
```

In Linux / macOS Bash:
```bash
export DB_URL="jdbc:mysql://localhost:3306/pashumandi?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
export DB_USERNAME="root"
export DB_PASSWORD="your_mysql_password"
export JWT_SECRET="dGVzdF9zZWNyZXRfa2V5X2Zvcl9wYXNodW1hbmRpX2F1dGhlbnRpY2F0aW9uX3N5c3RlbV8yNTZiaXRzIQ=="
export FRONTEND_URL="http://localhost:5173"
```

### 3. Run the Application
```bash
mvn spring-boot:run
```
The server starts on port `8080`.

---

## 7. API Specification

### Base URL: `http://localhost:8080`

### Interactive Documentation (Swagger UI)
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

### Endpoints Overview

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Public | Register new user account |
| `POST` | `/api/v1/auth/login` | Public | Login via phone/email and receive tokens |
| `POST` | `/api/v1/auth/refresh` | Public | Rotate refresh token and get new access token |
| `POST` | `/api/v1/auth/logout` | Authenticated | Revoke refresh tokens and end session |
| `GET` | `/api/v1/users/me` | Authenticated | Retrieve profile of authenticated user |
| `PUT` | `/api/v1/users/me` | Authenticated | Update profile information |
| `PUT` | `/api/v1/users/me/password` | Authenticated | Change password & invalidate other sessions |

---

### Sample Requests & Responses

#### 1. Register User
`POST /api/v1/auth/register`
```json
{
  "firstName": "Anil",
  "lastName": "Gupta",
  "phone": "9876543210",
  "email": "anil@example.com",
  "password": "StrongPassword123",
  "state": "Uttar Pradesh",
  "district": "Kushinagar",
  "village": "Example Village",
  "pincode": "274001"
}
```
**Response (201 Created):**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "id": 1,
    "firstName": "Anil",
    "lastName": "Gupta",
    "phone": "9876543210",
    "email": "anil@example.com",
    "role": "BUYER",
    "status": "ACTIVE",
    "state": "Uttar Pradesh",
    "district": "Kushinagar",
    "village": "Example Village",
    "pincode": "274001",
    "createdAt": "2026-10-04T20:00:00"
  }
}
```

#### 2. Login User
`POST /api/v1/auth/login`
```json
{
  "identifier": "9876543210",
  "password": "StrongPassword123"
}
```
**Response (200 OK):**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI...",
    "refreshToken": "4bbdb69e7691443796f310d15fa6dc5c...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "id": 1,
      "firstName": "Anil",
      "lastName": "Gupta",
      "phone": "9876543210",
      "email": "anil@example.com",
      "role": "BUYER",
      "status": "ACTIVE"
    }
  }
}
```

#### 3. Get Current Profile
`GET /api/v1/users/me`  
**Header:** `Authorization: Bearer <accessToken>`  
**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "firstName": "Anil",
    "lastName": "Gupta",
    "phone": "9876543210",
    "email": "anil@example.com",
    "role": "BUYER",
    "status": "ACTIVE",
    "profileImageUrl": null,
    "state": "Uttar Pradesh",
    "district": "Kushinagar",
    "village": "Example Village",
    "pincode": "274001",
    "createdAt": "2026-10-04T20:00:00",
    "lastLoginAt": "2026-10-04T20:05:00"
  }
}
```

#### 4. Change Password
`PUT /api/v1/users/me/password`  
**Header:** `Authorization: Bearer <accessToken>`
```json
{
  "currentPassword": "StrongPassword123",
  "newPassword": "NewStrongPassword456"
}
```
**Response (200 OK):**
```json
{
  "success": true,
  "message": "Password changed successfully. All other sessions have been logged out."
}
```

---

## 8. Security Design & Guarantees

1. **Zero Password Leakage**: The `password` and `passwordHash` are never serialized in DTOs, logged to consoles, or stored in JWT claims.
2. **Stateless Scalability**: No session state is retained on the application server. The `SecurityContext` is reconstructed per request by `JwtAuthenticationFilter`.
3. **Session Invalidation**:
   - Refresh tokens are verified against the database for every renewal.
   - Calling logout marks the user's refresh tokens as revoked.
   - Changing a password immediately revokes all active refresh tokens, protecting against compromised devices.
4. **Account Status Enforcement**: Accounts marked `BLOCKED`, `SUSPENDED`, or `INACTIVE` cannot authenticate or reuse tokens, returning HTTP 403 Forbidden.
5. **CORS Hardening**: Strict origin whitelisting (`http://localhost:5173`) with explicit headers and methods. Wildcard `*` origins are rejected when credentials are enabled.
