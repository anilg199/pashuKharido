# PashuMandi Milk Marketplace Microservice (`pashumandi-milk-service`)

A production-oriented Spring Boot 3.2.5 microservice providing a fresh milk marketplace, one-time ordering with concurrency-safe inventory deduction, recurring milk delivery subscriptions, buyer wishlists, and seller customer inquiries.

This microservice integrates seamlessly with the existing **PashuMandi Animal/Auth Backend** using **Shared JWT Stateless Authentication**, ensuring that users continue using their single PashuMandi login without duplicating accounts, passwords, or authentication flows.

---

## Architecture & Design Principles

```
                  +----------------------------------------------+
                  |         React Web Frontend (:5173)           |
                  +----------------------------------------------+
                         /                               \
         Auth & Animals /                                 \ Milk Marketplace APIs
                       /                                   \ [Bearer Existing-JWT]
                      v                                     v
+-----------------------------------+     +-----------------------------------+
|  PashuMandi Auth & Animal Backend |     |    PashuMandi Milk Microservice   |
|            (Port: 8080)           |     |            (Port: 8081)           |
+-----------------------------------+     +-----------------------------------+
| - User Registration & Login       |     | - Milk Listings & Discovery       |
| - JWT Issuer (HMAC-SHA256)        |     | - Stateless JWT Token Validator   |
| - Animal Listings & Cloudinary    |     | - Concurrency-Safe Stock Engine   |
| - Database: `pashumandi`          |     | - One-Time Order Fulfillment      |
+-----------------------------------+     | - Recurring Milk Subscriptions    |
                                          | - Favorites & Inquiries           |
                                          | - Database: `pashumandi_milk`     |
                                          +-----------------------------------+
```

### Key Architectural Highlights:
1. **Single Source of Truth for Identity:** The Milk Service never creates user passwords, registration endpoints, or duplicate user tables. The user logs in once through the existing PashuMandi auth endpoint (`/api/v1/auth/login`).
2. **Stateless JWT Validation:** The Milk Service validates the incoming access token using the shared signing secret (`jwt.secret`), extracts the user ID (`sub`) and role claim (`role`), and constructs an in-memory `AuthenticatedUser` principal.
3. **Database Isolation:** All milk tables reside in the dedicated `pashumandi_milk` schema. External user IDs are stored as numerical reference values (`seller_user_id`, `buyer_user_id`), avoiding cross-database foreign keys.
4. **Concurrency-Safe Inventory:** Stock reservation and deduction use JPA pessimistic write locking (`findByIdForUpdate` with `LockModeType.PESSIMISTIC_WRITE`) and optimistic locking (`@Version`) to prevent race conditions, negative stock, and overselling.
5. **Historical Price Protection:** Orders record snapshot values of `unit_price` and `total_price` at the moment of purchase, insulating historical order records from subsequent seller price adjustments.

---

## Technology Stack

- **Java Version:** 17
- **Framework:** Spring Boot 3.2.5
- **Security:** Spring Security 6 (Stateless JWT Filter)
- **JWT Library:** JJWT 0.11.5 (HMAC-SHA256 / `HS256`)
- **Persistence:** Spring Data JPA / Hibernate 6.4
- **Database:** MySQL 8+ (H2 in MySQL mode for unit & integration tests)
- **Migrations:** Flyway 9+ (`flyway-mysql`)
- **Documentation:** OpenAPI 3.0 / Swagger UI (`springdoc-openapi-starter-webmvc-ui` 2.0.2)
- **Port:** `8081` (Configurable via `SERVER_PORT`)

---

## API Endpoints Reference

### 1. Milk Listings (`/api/v1/milk/listings`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/v1/milk/listings` | Authenticated | Create a new milk listing (DRAFT by default, or PUBLISHED if stock > 0) |
| `GET` | `/api/v1/milk/listings` | **Public** | Browse published listings with available stock (pagination, sorting, filters) |
| `GET` | `/api/v1/milk/listings/search` | **Public** | Dynamic multi-attribute search (milkType, price, location, fat, etc.) |
| `GET` | `/api/v1/milk/listings/{listingId}` | **Public** / Owner | View listing details. Published listings are public; drafts are owner-only |
| `PUT` | `/api/v1/milk/listings/{listingId}` | Authenticated (Owner) | Update mutable listing details |
| `DELETE` | `/api/v1/milk/listings/{listingId}` | Authenticated (Owner) | Soft-deactivate listing (`status = INACTIVE`) |
| `GET` | `/api/v1/milk/listings/my-listings` | Authenticated | Get listings owned by the logged-in seller |
| `PATCH` | `/api/v1/milk/listings/{listingId}/status`| Authenticated (Owner) | Transition status (`PUBLISHED`, `PAUSED`, `INACTIVE`) |

### 2. Milk Orders (`/api/v1/milk/orders`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/v1/milk/orders` | Authenticated (Buyer) | Place a one-time order with concurrency-safe stock deduction |
| `GET` | `/api/v1/milk/orders/my-orders` | Authenticated (Buyer) | Retrieve authenticated buyer's purchase history |
| `GET` | `/api/v1/milk/orders/my-sales` | Authenticated (Seller) | Retrieve incoming orders for seller's listings |
| `GET` | `/api/v1/milk/orders/{orderId}` | Participant / Admin | Get order details (buyer, seller, or admin only) |
| `PATCH` | `/api/v1/milk/orders/{orderId}/status` | Participant / Admin | Update status (`ACCEPTED`, `OUT_FOR_DELIVERY`, `DELIVERED`, etc.) |
| `POST` | `/api/v1/milk/orders/{orderId}/cancel` | Participant / Admin | Cancel order and safely restore listing inventory |

### 3. Milk Subscriptions (`/api/v1/milk/subscriptions`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/v1/milk/subscriptions` | Authenticated (Buyer) | Create recurring daily/weekly milk delivery subscription |
| `GET` | `/api/v1/milk/subscriptions/my-subscriptions` | Authenticated (Buyer) | Get buyer's active and historical subscriptions |
| `GET` | `/api/v1/milk/subscriptions/seller-subscriptions` | Authenticated (Seller) | Get seller's subscriber orders |
| `GET` | `/api/v1/milk/subscriptions/{subscriptionId}` | Participant / Admin | Get subscription details |
| `PATCH` | `/api/v1/milk/subscriptions/{subscriptionId}/pause` | Participant / Admin | Temporarily pause subscription deliveries |
| `PATCH` | `/api/v1/milk/subscriptions/{subscriptionId}/resume` | Participant / Admin | Resume paused subscription |
| `PATCH` | `/api/v1/milk/subscriptions/{subscriptionId}/cancel` | Participant / Admin | Permanently cancel subscription |
| `POST` | `/api/v1/milk/subscriptions/generate-orders` | `ROLE_ADMIN` / Scheduler | Automated recurring order generator hook |

### 4. Milk Favorites & Inquiries

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/v1/milk/favorites/{listingId}` | Authenticated | Add listing to user's favorites |
| `DELETE` | `/api/v1/milk/favorites/{listingId}` | Authenticated | Remove listing from user's favorites |
| `GET` | `/api/v1/milk/favorites` | Authenticated | View current user's favorite listings |
| `POST` | `/api/v1/milk/inquiries/listings/{listingId}` | Authenticated | Send a customer inquiry to seller |
| `GET` | `/api/v1/milk/inquiries/my-inquiries` | Authenticated | View inquiries sent by buyer |
| `GET` | `/api/v1/milk/inquiries/received` | Authenticated | View inquiries received by seller |

---

## Database Schema (Flyway Migration V1)

Dedicated schema: `pashumandi_milk`

```sql
CREATE TABLE milk_listings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    milk_type VARCHAR(20) NOT NULL,
    animal_source VARCHAR(100),
    price_per_litre DECIMAL(10, 2) NOT NULL,
    available_quantity_litres DECIMAL(8, 2) NOT NULL,
    unit VARCHAR(20) NOT NULL DEFAULT 'LITRE',
    fat_percentage DECIMAL(4, 2),
    snf_percentage DECIMAL(4, 2),
    quality_description VARCHAR(500),
    state VARCHAR(100) NOT NULL,
    district VARCHAR(100) NOT NULL,
    village VARCHAR(100) NOT NULL,
    pincode VARCHAR(10) NOT NULL,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    fulfillment_type VARCHAR(20) NOT NULL,
    availability_start TIME,
    availability_end TIME,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    image_url VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE milk_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(36) NOT NULL UNIQUE,
    listing_id BIGINT NOT NULL,
    buyer_user_id BIGINT NOT NULL,
    seller_user_id BIGINT NOT NULL,
    quantity_litres DECIMAL(8, 2) NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    fulfillment_type VARCHAR(20) NOT NULL,
    scheduled_for TIMESTAMP NULL,
    delivery_state VARCHAR(100),
    delivery_district VARCHAR(100),
    delivery_village VARCHAR(100),
    delivery_pincode VARCHAR(10),
    delivery_notes VARCHAR(500),
    order_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    cancellation_reason VARCHAR(255),
    cancelled_by VARCHAR(20),
    cancelled_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_milk_orders_listing FOREIGN KEY (listing_id) REFERENCES milk_listings(id) ON DELETE RESTRICT
);
```

---

## React Frontend Integration

The React application uses the existing access token stored after PashuMandi login.

### API Client Example:

```javascript
// milkClient.js
const MILK_SERVICE_BASE_URL = "http://localhost:8081/api/v1/milk";

export async function milkApiRequest(endpoint, options = {}) {
  // Retrieve token from existing PashuMandi authentication state
  const accessToken = localStorage.getItem("accessToken");

  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {})
  };

  if (accessToken) {
    headers["Authorization"] = `Bearer ${accessToken}`;
  }

  const response = await fetch(`${MILK_SERVICE_BASE_URL}${endpoint}`, {
    ...options,
    headers
  });

  const body = await response.json();

  if (!response.ok) {
    if (response.status === 401) {
      // Existing auth flow handles token refresh or redirection to login
      window.dispatchEvent(new CustomEvent("pashumandi:auth-expired"));
    }
    const error = new Error(body.message || "Request to Milk Marketplace failed");
    error.status = response.status;
    error.errorCode = body.errorCode;
    error.validationErrors = body.validationErrors;
    throw error;
  }

  return body.data;
}

// Example: Fetch Browse Listings (Public)
export function fetchMilkListings(page = 0, size = 10, milkType = null) {
  const query = new URLSearchParams({ page, size });
  if (milkType) query.append("milkType", milkType);
  return milkApiRequest(`/listings?${query.toString()}`);
}

// Example: Place Order (Authenticated)
export function placeMilkOrder(orderData) {
  return milkApiRequest("/orders", {
    method: "POST",
    body: JSON.stringify(orderData)
  });
}
```

---

## Configuration (`application.yml` & `.env`)

```properties
SERVER_PORT=8081
MILK_DB_URL=jdbc:mysql://localhost:3306/pashumandi_milk?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
DEV_DB_USERNAME=root
DEV_DB_PASSWORD=your_mysql_password
JWT_SECRET=dGVzdF9zZWNyZXRfa2V5X2Zvcl9wYXNodW1hbmRpX2F1dGhlbnRpY2F0aW9uX3N5c3RlbV8yNTZiaXRzIQ==
JWT_ACCESS_EXPIRATION=900000
FRONTEND_URL=http://localhost:5173
```

---

## Local Development & Running

### 1. Create MySQL Database:
```sql
CREATE DATABASE IF NOT EXISTS pashumandi_milk CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Run Automated Tests:
From the repository root:
```powershell
.\mvnw.cmd test -f pashumandi-milk-service/pom.xml
```

### 3. Start Milk Microservice on Port 8081:
```powershell
.\mvnw.cmd spring-boot:run -f pashumandi-milk-service/pom.xml
```

### 4. Interactive Swagger UI:
Once started, visit:
```
http://localhost:8081/swagger-ui.html
```
Click **Authorize** at the top right and paste your Bearer token obtained from `POST http://localhost:8080/api/v1/auth/login`.
