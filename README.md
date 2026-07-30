# PayXpress Biller Services API

This is a full assembly of everything designed across our conversation: passwordless auth,
biller catalog + validation + fulfil (airtime, data, electricity, cable, WAEC PIN), Paystack
checkout with a race-guarded confirmation flow, decorative virtual accounts, and global
exception handling.

## ⚠️ What has and hasn't been verified

**Verified:**
- Every `.java` file has balanced braces (a crude but real syntax sanity check)
- Package structure is internally consistent — imports reference real classes within this project

**NOT verified — my sandbox has no Maven and no internet access:**
- This has never been compiled. `mvn compile` has never actually run against this code.
- No dependency resolution has happened — `pom.xml` versions are believed-correct, not tested.
- No database, no real XpressPay/Paystack calls, nothing has executed.

**First thing to do:** open this in IntelliJ as a Maven project and try to build it. Send me
whatever errors come back — that's the fastest way to get this actually working, rather than
me guessing blind from here.

## Update: AuthController now returns userId + phone

`issueTokens()` response now includes `userId` and `phone` alongside `accessToken`/
`refreshToken`. Needed once the frontend was wired for real — there was previously no
way to build a `User` object client-side after login/signup without decoding the JWT.

## Bugs found and fixed during self-audit (after the first zip)

Before starting real work on this, I went back through every file and checked it against
everything we'd actually designed in conversation — not just assumed it was all there
correctly. Found and fixed three real issues:

1. **`SecurityConfig` had `"/billers/**"` in `permitAll()`**, which accidentally also
   covered `/billers/payments/initiate` and `/billers/payments/history` — both of which do
   `(User) auth.getPrincipal()`. An unauthenticated request would have caused a
   `NullPointerException` instead of a clean 401. Fixed with precise `HttpMethod.GET`
   matchers for the browse/verify endpoints only; everything else under `/billers/**`
   now correctly requires a JWT.

2. **The verify-on-redirect endpoint (`GET /billers/payments/verify/{reference}`) never
   actually called `confirmationService.confirmPayment()`** — it just read whatever was
   already in the DB. This silently broke half the race-guard design: if Paystack's
   webhook was slow or never arrived, this endpoint would report `PENDING_PAYMENT`
   forever instead of actively resolving it. Fixed — it now calls `confirmPayment()`
   first (safe to call twice; the PENDING_PAYMENT guard + row lock make it a no-op if
   the webhook already handled it) before reporting status.

3. **`BillerSyncService.syncAllBillers()` was written but never called anywhere.** The
   `Biller` table would have stayed empty forever with no way to populate it. Added
   `BillerSyncController` (`POST /admin/billers/sync`) as a manual trigger — flagged in
   its own comment as needing real admin-role protection before production.

4. **`paystack.callback.url` was defined in config but never sent to Paystack** —
   `initializeTransaction()` didn't include `callback_url` in the request body at all,
   so Paystack would've silently fallen back to whatever default redirect is set in the
   dashboard instead of your app's actual callback. Fixed.

## Known reconciliation points with your real repo

1. **`PaystackService.java` is a reconstruction, not your real one.** I read your actual
   `PaystackService` earlier in our conversation but don't have its exact contents anymore.
   This version matches the shape we discussed (`initializeTransaction`, `verifyTransaction`,
   plus the `verifyWebhookSignature` method that was missing from the original — your webhook
   handler wasn't actually checking signatures before). **Diff this against your real class**
   before replacing it — field names, exact method signatures, and any extra logic your
   original had may not match.

2. **`application.properties` uses env var placeholders** (`${PAYSTACK_SECRET_KEY}`, etc.)
   instead of literal values. Fill in `.env.example` → rename to `.env` (or configure IntelliJ
   run config env vars directly) with your real test keys. **Never commit real keys** — this
   is the same issue as the live Paystack key that was exposed earlier.

3. **The Dockerfile references `./mvnw`**, which doesn't exist yet in this zip (no network
   here to generate the wrapper jar). Run `mvn -N wrapper:wrapper` locally once you have
   Maven, or just build via IntelliJ's Maven integration for now.

4. **Category path mappings in `BillerPaths.java` / `BillerSyncService.java`** are confirmed
   for airtime, data, electricity, cable, and WAEC PIN specifically (pulled from your real
   Postman collection export). Betting, insurance, JAMB, NECO, and NABTECH are NOT implemented
   — out of scope per your "main stuff only" call.

5. **`User.getEmail()` doesn't exist** — `BillerCheckoutController` currently fabricates a
   placeholder email (`{phone}@payxpress.ng`) for Paystack's `initializeTransaction()`. Decide
   if you want a real email field on `User`, or if the placeholder is fine long-term.

## What's deliberately NOT built yet

- Real SMS provider — `MockSmsService` just logs the OTP to console, active under `dev`/`test`/
  `local` profiles only. Swap in Termii/Africa's Talking by implementing `SmsService` with a
  `@Profile("prod")` (or whatever your prod profile is named).
- Rate limiting on OTP request endpoints
- Idempotency guard against double-tapped checkout initiate
- Admin/support tooling for reviewing `NEEDS_RECONCILIATION` transactions
- Mobile client wiring (deep links, secure token storage, refresh-on-401)

## Project structure

```
src/main/java/com/payxpress/
├── PayxpressBillerApiApplication.java
├── model/          — entities + enums (User, OtpCode, UserSession, VirtualAccount, Biller, PaymentTransaction)
├── repository/      — Spring Data JPA repositories
├── security/        — JwtService, JwtAuthFilter
├── config/          — SecurityConfig, WebClientConfig
├── auth/            — OtpService, SessionService, SmsService/MockSmsService, AuthController
├── wallet/          — VirtualAccountService, WalletController
├── biller/          — HMAC signer, XpressPay client, per-category services, dispatcher, sync, DTOs, controllers
├── payment/         — PaystackService, PaymentConfirmationService, checkout/history/webhook controllers
└── exception/        — GlobalExceptionHandler
```

## Endpoints implemented

- `POST /auth/signup/bvn`, `/signup/request-otp`, `/signup/verify-otp`
- `POST /auth/login/request-otp`, `/login/verify-otp`
- `POST /auth/refresh`, `/auth/logout`
- `GET /billers`, `/billers/{categoryPath}`
- `POST /billers/validate` (electricity, cable)
- `POST /billers/payments/initiate`
- `GET /billers/payments/history`, `/billers/payments/verify/{reference}`
- `POST /webhooks/paystack`
- `GET /wallet/balance`, `/wallet/accounts`
