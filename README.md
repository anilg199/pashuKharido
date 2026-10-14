# PashuMandi Platform - Enterprise Microservices Architecture

[![Java](https://img.shields.io/badge/Java-17%20%7C%2019%20LTS-ED8B00?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6.x-6DB33F?logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![JWT](https://img.shields.io/badge/JWT-JJWT%200.11.5-000000?logo=jsonwebtokens&logoColor=white)](https://jwt.io/)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0%20%2F%20Swagger-85EA2D?logo=swagger&logoColor=black)](http://localhost:8080/swagger-ui.html)

---

## 1. Enterprise Architecture Overview

**PashuMandi** is structured as an **enterprise multi-module platform** divided into specialized microservice domains while sharing a unified identity and single source of authentication truth:

```
pashuKharide/ (Platform Root - Monorepo)
├── pom.xml                               # Root Maven Aggregator POM
├── mvnw / mvnw.cmd / .mvn/                # Maven Wrapper
│
├── pashumandi-core-service/              # PORT 8080: Core Platform & Livestock Subsystem
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/pashumandi/
│       │   ├── controller/               # Auth, User, Animal Listings & Media Endpoints
│       │   ├── entity/                   # User, RefreshToken, Animal, AnimalImage
│       │   ├── security/                 # JWT Issuer, UserDetailsService, BCrypt
│       │   ├── service/                  # AuthService, UserService, AnimalService, Cloudinary
│       │   └── repository/               # JPA Repositories & Specifications
│       └── main/resources/
│           ├── db/migration/             # Flyway V1-V6 (Users, RefreshTokens, Animals)
│           └── application-*.yml         # Port 8080, DB: pashumandi
│
└── pashumandi-milk-service/              # PORT 8081: Milk Marketplace Microservice
    ├── pom.xml
    └── src/
        ├── main/java/com/pashumandi/milk/
        │   ├── controller/               # Milk Listings, Orders, Subscriptions, Inquiries, Favorites
        │   ├── entity/                   # MilkListing, MilkOrder, MilkSubscription, etc.
        │   ├── security/                 # Stateless JWT Validator (Validates Core JWT tokens)
        │   ├── service/                  # MilkListingService, MilkOrderService, SubscriptionService
        │   └── repository/               # Pessimistic Locking Repositories & Specifications
        └── main/resources/
            ├── db/migration/             # Flyway V1 (Milk Tables & Indexes)
            └── application-*.yml         # Port 8081, DB: pashumandi_milk
```

---

## 2. Service Separation & Enterprise Design Principles

### Why This Structure is Enterprise-Grade
1. **Single Source of Truth for Identity (SSO / Shared JWT)**:
   - Users register and log in **only once** via `pashumandi-core-service` (`/api/v1/auth/login`).
   - `pashumandi-core-service` generates and signs the HMAC-SHA256 JWT access token.
   - `pashumandi-milk-service` validates the same JWT token statelessly using the shared HMAC secret without needing a duplicate user database, extra network hops, or a second login system.
2. **Database Isolation (Database-per-Service)**:
   - `pashumandi-core-service` manages the `pashumandi` schema (Users, Animals, Media).
   - `pashumandi-milk-service` manages the `pashumandi_milk` schema (Milk Listings, Orders, Subscriptions, Inquiries, Favorites).
   - Neither service queries the other's database directly. Milk listings and orders reference `seller_id` and `buyer_id` from token claims.
3. **Independent Deployability & Scalability**:
   - High traffic in milk delivery orders (frequent morning/evening spikes) will not degrade cattle listing operations or user authentication.
   - Each microservice can be packaged into its own Docker container and scaled horizontally.
4. **Clean Root Maven Monorepo**:
   - The root `pom.xml` aggregates both modules, allowing a single command (`.\mvnw test` or `.\mvnw package`) to build and verify the entire platform in seconds.

---

## 3. Technology Stack & Ports

| Service | Port | Database | Primary Responsibilities |
|---|---|---|---|
| **`pashumandi-core-service`** | `8080` | `pashumandi` (MySQL) | User Registration, Dual-ID Login, JWT Issuance, Refresh Tokens, Profile Management, Cattle/Livestock Listings, Cloudinary Media |
| **`pashumandi-milk-service`** | `8081` | `pashumandi_milk` (MySQL) | Daily Milk Listings, On-demand Orders, Recurring Milk Subscriptions, Favorites, Inquiries, Pessimistic Stock Lock |

---

## 4. Shared JWT Authentication Flow

```
[ Frontend / React Client ]
       │
       │ 1. POST /api/v1/auth/login (Phone/Email + Password)
       ▼
[ pashumandi-core-service (Port 8080) ]
       │
       │ 2. Authenticates credentials & signs JWT Access Token
       ▼
[ Frontend / React Client ]
       │
       ├─── 3a. Header: Bearer <Token> ──► [ pashumandi-core-service:8080 ] (Animal Listings, Profile)
       │
       └─── 3b. Header: Bearer <Token> ──► [ pashumandi-milk-service:8081 ] (Milk Orders, Subscriptions)
                                                        │
                                            Stateless verification via
                                            matching jwt.secret (No 2nd login!)
```

---

## 5. Building & Running the Platform

### Prerequisites
- Java 17 or Java 19+
- MySQL 8.0+ running on `localhost:3306`
- Create two separate databases:
  ```sql
  CREATE DATABASE IF NOT EXISTS pashumandi CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  CREATE DATABASE IF NOT EXISTS pashumandi_milk CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  ```

### Build Everything from Root
```bash
# Clean, compile, and run all unit & integration tests across both modules
.\mvnw.cmd clean test

# Build executable production JARs for both microservices
.\mvnw.cmd clean package -DskipTests
```

### Running the Services Locally

#### Option A: Running Core Service (Port 8080)
```bash
cd pashumandi-core-service
..\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```
- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- API Docs: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

#### Option B: Running Milk Service (Port 8081)
```bash
cd pashumandi-milk-service
..\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```
- Swagger UI: [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html)
- API Docs: [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs)

---

## 6. API Route Mapping

### Core Service (`http://localhost:8080`)
- **Authentication**:
  - `POST /api/v1/auth/register` - Create account
  - `POST /api/v1/auth/login` - Dual login (phone/email)
  - `POST /api/v1/auth/refresh-token` - Rotate refresh token
  - `POST /api/v1/auth/logout` - Revoke tokens
- **User Profile**:
  - `GET /api/v1/users/me` - Get profile
  - `PUT /api/v1/users/me` - Update profile
  - `POST /api/v1/users/me/profile-image` - Cloudinary avatar upload
- **Animal Listings**:
  - `POST /api/v1/animals` - Create animal listing
  - `GET /api/v1/animals` - Search & filter animals
  - `POST /api/v1/animals/{id}/images` - Upload animal photos

### Milk Marketplace Service (`http://localhost:8081`)
- **Milk Listings**:
  - `POST /api/v1/milk/listings` - Post milk listing (Cow/Buffalo/Goat/Mixed)
  - `GET /api/v1/milk/listings` - Search listings (filter by animal type, fat%, delivery type, organic)
  - `GET /api/v1/milk/listings/{id}` - Listing details
  - `PUT /api/v1/milk/listings/{id}` - Update listing
  - `PATCH /api/v1/milk/listings/{id}/status` - Update status (PUBLISHED, PAUSED, CLOSED)
  - `GET /api/v1/milk/listings/my-listings` - Seller listings
- **Direct Orders**:
  - `POST /api/v1/milk/orders` - Place direct order (with pessimistic locking)
  - `GET /api/v1/milk/orders/my-orders` - Buyer order history
  - `GET /api/v1/milk/orders/seller-orders` - Seller order dashboard
  - `PATCH /api/v1/milk/orders/{id}/status` - Order lifecycle (CONFIRMED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED)
- **Recurring Subscriptions**:
  - `POST /api/v1/milk/subscriptions` - Start daily/alternate-day/weekly subscription
  - `GET /api/v1/milk/subscriptions/my-subscriptions` - Buyer active subscriptions
  - `PATCH /api/v1/milk/subscriptions/{id}/pause` - Pause subscription
  - `PATCH /api/v1/milk/subscriptions/{id}/resume` - Resume subscription
  - `PATCH /api/v1/milk/subscriptions/{id}/cancel` - Cancel subscription
- **Buyer Inquiries & Favorites**:
  - `POST /api/v1/milk/inquiries` - Send inquiry to dairy seller
  - `POST /api/v1/milk/favorites/{listingId}` - Bookmark listing
  - `DELETE /api/v1/milk/favorites/{listingId}` - Remove bookmark
  - `GET /api/v1/milk/favorites/my-favorites` - List bookmarked milk sources

---

## 7. Automated Testing Suite

All tests can be executed in one command:
```bash
.\mvnw.cmd test
```

- **`pashumandi-core-service`**: 66 unit/integration tests verifying JWT generation, refresh token rotation, user profiles, BCrypt security, animal listing filters, and Cloudinary validations.
- **`pashumandi-milk-service`**: 30 comprehensive tests covering pessimistic write locks, stock validations, order workflows, recurring subscriptions, inquiries, favorites, and stateless security filter integration.
