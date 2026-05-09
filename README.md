44 files committed cleanly. 🎉

Now let's create the README — this is the file recruiters and hiring managers actually read first when they open your repo.

I'll write you a strong one based on everything we built. Create a new file called `README.md` in the **root of your project** (the same folder as `.git`, NOT inside `pocketpayapi/`). Paste this in:

```markdown
# PocketPay — Wallet API

A Nigerian fintech wallet API built with **Java 17** and **Spring Boot 3.5**. Inspired by how products like Kuda, OPay, and PiggyVest work under the hood — closed-loop wallet system with Paystack integration for funding.

## Features

- **JWT-based authentication** with Spring Security
- **Auto wallet creation** on user registration
- **Internal transfers** between users (instant, free — pure database operations)
- **Paystack integration** for funding wallets via real card payments
- **Transaction history** with pagination and type filtering
- **BigDecimal precision** for all monetary values (no floating point errors)
- **Idempotency checks** to prevent double-processing of payments
- **Global exception handling** with consistent JSON error responses
- **Swagger/OpenAPI documentation** auto-generated from code

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5 |
| Security | Spring Security + JWT (jjwt 0.11.5) |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA + Hibernate |
| Payments | Paystack API |
| Build | Maven |
| Docs | Springdoc OpenAPI 2.8.5 |

## Architecture

The API follows a clean layered architecture:

```
Controller → Service → Repository → Database
```

Each layer has one responsibility:
- **Controller** — handles HTTP requests/responses, never contains business logic
- **Service** — business rules, transactions, third-party integrations
- **Repository** — database access via Spring Data JPA
- **Entity** — JPA-mapped database tables
- **DTO** — request/response shapes that never expose internal entities

## Key Design Decisions

**Closed-loop transfers** — internal user-to-user transfers don't go through Paystack. They're database-only operations wrapped in `@Transactional`, making them instant and fee-free. Only wallet funding (money entering the system) goes through Paystack.

**BigDecimal everywhere for money** — `double` and `float` cause rounding errors that compound at scale. All amounts use `BigDecimal` with precision 19, scale 4.

**Pending → Success transaction model** — when a user initiates a Paystack payment, a PENDING transaction is saved immediately, tied to their wallet. On verify, ownership is checked and the transaction is upgraded to SUCCESS. This prevents users from claiming each other's payments.

**`@Transactional` on money operations** — wallet transfers debit the sender and credit the receiver in a single atomic transaction. If anything fails, both operations roll back.

## Endpoints

### Auth
- `POST /api/auth/register` — create account + auto-generate wallet
- `POST /api/auth/login` — get JWT token

### Wallet
- `GET /api/wallets` — get current user's wallet
- `POST /api/wallets/fund` — manual funding (dev/testing)
- `POST /api/wallets/transfer` — transfer to another user

### Transactions
- `GET /api/transactions?page=0&size=10&type=TRANSFER` — paginated history
- `GET /api/transactions/{reference}` — single transaction by reference

### Paystack
- `POST /api/paystack/initiate?amount=5000` — start a real payment, returns checkout URL
- `GET /api/paystack/verify/{reference}` — verify and credit wallet

Full interactive documentation available at `/swagger-ui.html` when running.

## Running Locally

### Prerequisites
- Java 17
- PostgreSQL 16
- Maven 3.9+
- A free Paystack account ([paystack.com](https://paystack.com)) for the test API key

### Setup

1. Clone the repo
   ```bash
   git clone https://github.com/Dannc137/pocketpay-api.git
   cd pocketpay-api
   ```

2. Create the PostgreSQL database
   ```sql
   CREATE DATABASE pocketpay_db;
   ```

3. Set environment variables
   ```bash
   PAYSTACK_SECRET_KEY=sk_test_your_key_here
   DB_PASSWORD=your_postgres_password
   JWT_SECRET=any-long-random-string-min-32-chars
   ```

4. Run the app
   ```bash
   ./mvnw spring-boot:run
   ```

5. Open Swagger
   ```
   http://localhost:8080/swagger-ui.html
   ```

## Testing the Flow

1. Register a user via `/api/auth/register`
2. Copy the JWT from the response
3. Click **Authorize** in Swagger and paste the token
4. Initiate a Paystack payment via `/api/paystack/initiate?amount=5000`
5. Open the `authorizationUrl` from the response
6. Pay with the Paystack test card: `4084 0840 8408 4081`, any future date, CVV `408`, OTP `123456`
7. Verify via `/api/paystack/verify/{reference}` — wallet gets credited

## What Could Be Added

This is a portfolio project, not a production system. Things that would be needed for production:

- Webhook endpoint for Paystack (right now we only verify on demand)
- Optimistic locking on wallet balance (`@Version`) to prevent race conditions
- Refresh tokens for longer sessions
- Rate limiting (Bucket4j)
- Database migrations (Flyway/Liquibase) instead of `ddl-auto=update`
- Audit logging for compliance
- Withdrawal endpoint via Paystack Transfers API
- KYC (NIN/BVN) verification — required by CBN for real Nigerian fintech
- Unit and integration tests

## About

Built by Daniel as a learning project to deepen Spring Boot, fintech architecture, and third-party API integration skills.

[LinkedIn](https://www.linkedin.com/in/daniel-ifie-a656b2408) · [GitHub](https://github.com/Dannc137)
