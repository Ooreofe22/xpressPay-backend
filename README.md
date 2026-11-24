# PayXpress API

The backend for **PayXpress**, a Nigerian bill payments and wallet app. Live at **[paypei.online](https://paypei.online)**.

It handles sign up and login with one time codes, wallets, the biller catalogue, and paying for airtime, data, electricity, cable TV and exam PINs through Paystack checkout. Each biller request is signed with HMAC SHA512.

Web app repo: [XpressPay](https://github.com/Ooreofe22/XpressPay) (Next.js).

## Highlights

- **Pays out exactly once.** A payment can be confirmed by the Paystack webhook *and* by the user's redirect. Both paths go through one confirmation service that locks the transaction row (`SELECT ... FOR UPDATE`) and only moves a payment out of `PENDING_PAYMENT` once. If the alert arrives twice, the second one does nothing.
- **Signed requests.** Every call to the biller API is signed with HMAC SHA512, and every Paystack webhook signature is checked before it is trusted.
- **Passwordless auth.** One time codes by SMS or email, short lived JWT access tokens and refresh tokens, one active session per user.
- **Customer validation** for electricity meters and cable smart cards before the user pays.
- **Admin endpoints** for the dashboard: transactions, accounts, billers and reconciliation.
- **Global error handling** so every error comes back as clean JSON.

## Tech stack

| Area | Tools |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3 (Web, WebFlux client, Data JPA, Security, Validation) |
| Database | PostgreSQL |
| Auth | JWT (jjwt) |
| Payments | Paystack, Xpress Payments biller API |
| Hosting | Docker on Render |

## Project structure

```
src/main/java/com/payxpress/
  auth/         OTP, sessions, sign up and login
  security/     JWT service and filter
  config/       security and HTTP client config
  wallet/       wallet balance and accounts
  biller/       biller catalogue, validation, HMAC signer, API client
  payment/      Paystack checkout, webhook, confirmation, history
  admin/        dashboard endpoints
  model/        JPA entities
  repository/   Spring Data repositories
  exception/    global error handler
```

## Main endpoints

| Method | Path | What it does |
|---|---|---|
| POST | `/auth/signup/request-otp`, `/auth/signup/verify-otp` | Sign up with a one time code |
| POST | `/auth/login/request-otp`, `/auth/login/verify-otp` | Log in |
| POST | `/auth/refresh`, `/auth/logout` | Refresh or end a session |
| GET | `/billers`, `/billers/{category}` | Browse billers |
| POST | `/billers/validate` | Check a meter or smart card number |
| POST | `/billers/payments/initiate` | Start a Paystack checkout |
| GET | `/billers/payments/verify/{reference}` | Confirm a payment after redirect |
| GET | `/billers/payments/history` | Payment history |
| POST | `/webhooks/paystack` | Paystack webhook |
| GET | `/wallet/balance`, `/wallet/accounts` | Wallet info |

## Run locally

You need Java 21, Maven and PostgreSQL.

```bash
cp .env.example .env      # fill in your own local and test values
mvn spring-boot:run
```

The API starts on http://localhost:8080. Or build the Docker image with `docker build -t payxpress-api .`

## Configuration

All secrets come from environment variables. See `.env.example` for the full list:
`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, `PAYSTACK_SECRET_KEY`, `PAYSTACK_CALLBACK_URL`, `XPRESSPAY_PUBLIC_KEY`, `XPRESSPAY_PRIVATE_KEY`, `XPRESSPAY_BASE_URL`, `RESEND_API_KEY`, `ADMIN_API_KEY`, `SPRING_PROFILE`.

Real values live in `.env` or on the host, never in git.

## Author

**Ooreofeoluwa Popoola**, full stack software engineer (backend focused).
[LinkedIn](https://www.linkedin.com/in/ooreofeoluwa-popoola-8260ba3b4/) · [GitHub](https://github.com/Ooreofe22)
