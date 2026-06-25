# CarHub — Architecture & Design Notes

This document explains how the codebase is organised and **why**. It is the
counterpart to the README's "how to run it." The guiding goal is the same one
the original service followed: strict separation of concerns, no magic
strings, and no hardcoded HTTP status codes or user-facing messages — but
expressed idiomatically for Spring Boot.

---

## 1. Package-by-feature

Code is grouped by **feature**, not by technical layer. Everything that changes
together lives together:

```
com.carhub
├── user/        ── User document, repository, AuthService(+Impl),
│   └── dto/        UserService(+Impl), mapper, controller, request/response DTOs
├── car/         ── Car document, repository, CarService(+Impl),
│   └── dto/        CloudinaryService, mapper, controller, DTOs, DriveType
├── booking/     ── Booking document, repository, BookingService(+Impl),
│   └── dto/        mapper, controller, DTOs, BookingStatus, PaymentStatus
├── email/       ── EmailService, EmailProvider strategy (Brevo/SMTP), model
├── ratelimit/   ── @RateLimit, RateLimitAspect, RateLimitService, interceptor
├── abuselog/    ── AbuseLog document + repository
├── common/      ── cross-cutting building blocks (see §3)
│   ├── exception/  ApiException, ErrorCode, GlobalExceptionHandler
│   ├── message/    MessageService, MessageKeys
│   ├── response/   ApiResponse, ResponseFactory
│   └── security/   JwtService, JwtAuthenticationFilter, TokenBlacklist, …
└── config/      ── SecurityConfig, MongoConfig, WebMvcConfig, OpenApiConfig
    └── properties/ typed @ConfigurationProperties beans
```

**Why:** a developer touching "bookings" opens one package and sees the whole
vertical — document, repository, service, controller, DTOs — instead of
hopping across `controllers/`, `services/`, `models/` trees. `common/` and
`config/` hold only what is genuinely shared.

---

## 2. Layered responsibilities within a feature

The same discipline as the   app (`controller → service → model`), enforced
by Spring stereotypes:

| Layer | Type | Responsibility |
|---|---|---|
| **Controller** | `@RestController` | HTTP only: bind/validate input, delegate to service, wrap the result via `ResponseFactory`. No business logic, no try/catch. |
| **Service** | interface + `@Service` impl | All business logic, DB access, orchestration. Throws `ApiException` carrying an `ErrorCode`. |
| **Repository** | `interface extends MongoRepository` | Data access only. Derived queries + `@Query` for overlap checks. |
| **Document** | `@Document` POJO | Mongo schema; Lombok `@Builder`/accessors; auditing timestamps. |
| **DTO** | `record` | Immutable request/response shapes; Bean Validation on requests. |
| **Mapper** | MapStruct / hand-written | Document ↔ DTO translation; keeps documents out of the API surface. |

Controllers never see a `Car` document and the web layer never leaks into
services — DTOs are the boundary in both directions.

---

## 3. Single source of truth: no hardcoded codes or messages

This was an explicit requirement carried over from the   project
(`statusCodes.js`, `messages.js`). Two collaborating pieces enforce it:

### `ErrorCode` (status + message key)
Each error is one enum constant bundling its **HTTP status** and a **message
key** — the only place either is declared:

```java
USER_ALREADY_EXISTS(HttpStatus.CONFLICT,        "error.user.already-exists"),
INVALID_CREDENTIALS (HttpStatus.UNAUTHORIZED,    "error.auth.invalid-credentials"),
RATE_LIMIT_EXCEEDED (HttpStatus.TOO_MANY_REQUESTS,"error.common.rate-limit");
```

Services throw `new ApiException(ErrorCode.USER_ALREADY_EXISTS)` — they never
name a status code or a string. Call sites can pass `args` for message
interpolation (e.g. a retry-after value).

### `messages.properties` + `MessageService`
Every user-facing string — error messages, success messages, and email
subjects — is resolved through Spring's `MessageSource` (i18n-ready):

- **Error keys** live on `ErrorCode`.
- **Success / email keys** are constants in `MessageKeys`, resolved by
  `ResponseFactory.ok(...)` / `created(...)`.

The result: changing wording or adding a locale is a properties-file edit; no
Java recompile of business logic, and grepping for a status code finds exactly
one declaration.

### `ApiResponse<T>` envelope
A single response shape across the whole API:

```json
{ "success": true, "message": "Login successful", "data": { ... } }
```

`@JsonInclude(NON_NULL)` keeps `data` out of error payloads.
`GlobalExceptionHandler` (a `@RestControllerAdvice`) is the **only** place
exceptions become responses, so no controller carries error-handling noise.

---

## 4. Design patterns used (and why)

| Pattern | Where | Why |
|---|---|---|
| **Dependency Injection (constructor)** | everywhere via Lombok `@RequiredArgsConstructor` | Immutable `final` collaborators, trivially testable, no field injection. |
| **DTO + Mapper** | every feature's `dto/` + `*Mapper` | Decouples the persistence model from the API contract; MapStruct generates the boilerplate at compile time. |
| **Service interface + impl** | `AuthService`/`AuthServiceImpl`, etc. | Dependency-inversion: controllers and cross-feature callers depend on the abstraction, enabling mocking and alternate impls. |
| **Repository** | Spring Data `*Repository` | Declarative data access; no DAO plumbing. |
| **Strategy** | `EmailProvider` → `BrevoEmailProvider` / `SmtpEmailProvider` | Swap email transport via `EMAIL_PROVIDER` env var (`@ConditionalOnProperty`) without touching `EmailService`. |
| **Template Method (templates)** | Thymeleaf `templates/email/*.html` | OTP/booking emails rendered from templates + a model object, not string concatenation. |
| **AOP + custom annotation** | `@RateLimit` + `RateLimitAspect` | Per-route limits are declarative on the controller method; the cross-cutting Redis logic lives in one aspect. A `HandlerInterceptor` enforces the global limit. |
| **Filter chain** | `JwtAuthenticationFilter (OncePerRequestFilter)` | Stateless token authentication runs once per request before the controller. |
| **Centralised advice** | `GlobalExceptionHandler` | One translation point from exceptions to `ApiResponse`. |
| **Typed configuration** | `@ConfigurationProperties` beans in `config/properties/` | `JwtProperties`, `CloudinaryProperties`, `RateLimitProperties`, … give type-safe, validated config instead of scattered `@Value`. |
| **Builder** | Lombok `@Builder` on documents/models | Readable construction of multi-field documents. |
| **Factory** | `ResponseFactory` | Single helper to assemble success/created envelopes with resolved messages. |

---

## 5. Security model

- **Stateless JWT.** HS256 via jjwt. Access token (`sub` = userId, `jti`, 15 min)
  + an **opaque refresh token** (random, stored only as a SHA-256 hash, 7 days,
  rotated on every refresh).
- **Logout** blacklists the access token's `jti` in Redis (`bl:{jti}`) for its
  remaining TTL and revokes the refresh token.
- `SecurityConfig` is stateless: public auth routes + public `GET /api/cars` +
  docs/health; everything else requires a valid bearer token.
  `@AuthenticationPrincipal String userId` exposes the caller's id to controllers.
- Passwords hashed with BCrypt. Login returns a single `INVALID_CREDENTIALS`
  for both unknown-user and wrong-password (anti-enumeration).

---

## 6. Reliability & concurrency

- **Booking overlap protection:** a Redis distributed lock
  (`lock:booking:{carId}`, `SET NX EX`) guards the create path, and the
  repository runs an active-overlap query before persisting — defence in depth
  against double-booking the same dates.
- **Rate limiting + abuse logging:** Redis `INCR`/`EXPIRE` counters; every
  violation is also written to the `abuse_logs` collection for auditing.
- **Async email:** sending is `@Async` and failures are logged rather than
  thrown — an email outage never fails a booking or registration. (The  
  version awaited inline; this is a deliberate improvement.)
- **Mongo auditing:** `@CreatedDate` / `@LastModifiedDate` populate timestamps
  automatically.

---

## 7. Sequence & flow diagrams

These render natively on GitHub. They show the runtime collaboration between the
components described above.

### 7.1 Request pipeline (every request)

```mermaid
flowchart TD
    A["HTTP request"] --> B["DispatcherServlet<br/>(Front Controller)"]
    B --> C["CorsFilter"]
    C --> D["JwtAuthenticationFilter<br/>OncePerRequestFilter"]
    D -->|"Bearer token"| E{"JwtService.parse()<br/>+ blacklist check"}
    E -->|"valid"| F["SecurityContext = userId"]
    E -->|"invalid / blacklisted"| X1["401<br/>RestAuthenticationEntryPoint"]
    D -->|"no token"| F2["anonymous"]
    F --> G["Authorization rules<br/>(SecurityConfig)"]
    F2 --> G
    G -->|"protected & anonymous"| X1
    G -->|"allowed"| H["GlobalRateLimitInterceptor<br/>enforce(global)"]
    H -->|"over limit"| X2["429 + AbuseLog"]
    H -->|"ok"| I["@RateLimit Aspect (AOP)<br/>enforce(policy)"]
    I -->|"over limit"| X2
    I -->|"ok"| J["Controller @RestController"]
    J --> K["Service @Service impl"]
    K --> L["Repository (Mongo)"]
    K --> M["EmailService @Async"]
    K --> N["JwtService / CloudinaryService / Redis lock"]
    K -->|"success"| O["ApiResponse envelope"]
    K -->|"throws ApiException"| P["GlobalExceptionHandler<br/>ApiResponse.error"]
```

### 7.2 Registration + OTP verification

```mermaid
sequenceDiagram
    autonumber
    actor C as Client
    participant UC as UserController
    participant AS as AuthServiceImpl
    participant UR as UserRepository
    participant ES as EmailService
    participant DB as MongoDB

    C->>UC: POST /api/user/register
    Note over UC: @RateLimit(register)
    UC->>AS: register(request)
    AS->>UR: findByEmail / findByPhonenumber
    UR->>DB: query
    alt email or phone already used
        AS-->>UC: throw ApiException(USER_ALREADY_EXISTS)
        UC-->>C: 409 ApiResponse.error
    else available
        AS->>AS: bcrypt(password) + 6-digit OTP (10m expiry)
        AS->>UR: save User (emailVerified=false)
        UR->>DB: insert
        AS->>ES: sendOtp(...) @Async
        AS-->>UC: UserResponse
        UC-->>C: 201 ApiResponse(USER_REGISTERED)
    end

    C->>UC: POST /api/user/verify-otp
    UC->>AS: verifyOtp(request)
    AS->>UR: findByEmail
    alt otp matches and not expired
        AS->>UR: set emailVerified=true, clear otp
        AS-->>UC: UserResponse
        UC-->>C: 200 ApiResponse(OTP_VERIFIED)
    else invalid or expired
        AS-->>UC: throw ApiException(INVALID_OTP)
        UC-->>C: 400 ApiResponse.error
    end
```

### 7.3 Login + token issuance

```mermaid
sequenceDiagram
    autonumber
    actor C as Client
    participant UC as UserController
    participant AS as AuthServiceImpl
    participant UR as UserRepository
    participant JS as JwtService

    C->>UC: POST /api/user/login
    Note over UC: @RateLimit(login)
    UC->>AS: login(request)
    AS->>UR: findByEmail
    alt missing user OR bad password OR not verified
        AS-->>UC: throw ApiException(INVALID_CREDENTIALS)
        UC-->>C: 401 ApiResponse.error
    else valid and verified
        AS->>JS: generateAccessToken(userId)
        JS-->>AS: IssuedToken (token, jti, exp 15m)
        AS->>AS: issueRefreshToken() random hex, store SHA-256 hash 7d
        AS->>UR: save refresh-token hash
        AS-->>UC: AuthResponse (access, refresh)
        UC-->>C: 200 ApiResponse(LOGIN_SUCCESS)
    end
```

### 7.4 Authenticated request (JWT filter)

```mermaid
sequenceDiagram
    autonumber
    actor C as Client
    participant F as JwtAuthenticationFilter
    participant JS as JwtService
    participant BL as TokenBlacklist
    participant SC as SecurityContext
    participant Ctl as Controller

    C->>F: request + Authorization Bearer token
    F->>JS: parse(token)
    alt signature or expiry invalid
        F-->>C: 401 RestAuthenticationEntryPoint
    else valid
        F->>BL: isBlacklisted(jti)? key bl:{jti}
        alt blacklisted (logged out)
            F-->>C: 401
        else active
            F->>SC: set principal = userId
            F->>Ctl: continue filter chain
            Ctl-->>C: 200 ApiResponse
        end
    end
```

### 7.5 Booking creation (distributed lock + overlap check)

```mermaid
sequenceDiagram
    autonumber
    actor C as Client
    participant BC as BookingController
    participant BS as BookingServiceImpl
    participant R as Redis
    participant CR as CarRepository
    participant BR as BookingRepository
    participant ES as EmailService

    C->>BC: POST /api/booking/new-booking
    BC->>BS: createBooking(userId, request)
    BS->>CR: findById(carId)
    BS->>R: SET lock:booking:{carId} NX EX 10s
    alt lock not acquired
        BS-->>BC: throw ApiException(BOOKING_IN_PROGRESS)
        BC-->>C: 409 ApiResponse.error
    else lock acquired
        BS->>BR: existsActiveOverlap(car, start, end)
        alt dates overlap an active booking
            BS-->>BC: throw ApiException(DATES_UNAVAILABLE)
        else available
            BS->>BS: price = days * car.price + extras (gps+10, babySeat+5)
            BS->>BR: save Booking (status PENDING)
            BS->>ES: sendBookingConfirmation(...) @Async
            BS-->>BC: BookingResponse
        end
        BS->>R: DEL lock:booking:{carId}
        BC-->>C: 201 ApiResponse(BOOKING_CREATED)
    end
```

### 7.6 Rate limiting + error translation

```mermaid
flowchart TD
    A["Limiter (global interceptor<br/>or @RateLimit aspect)"] --> B["RateLimitService.enforce(policy)"]
    B --> C["Redis INCR counter (prefix + clientIp)"]
    C --> D{"first hit in window?"}
    D -->|"yes"| E["EXPIRE key = windowSeconds"]
    D -->|"no"| Fr["read TTL"]
    E --> G["set X-RateLimit-* headers"]
    Fr --> G
    G --> H{"count > limit?"}
    H -->|"no"| I["proceed to controller"]
    H -->|"yes"| J["save AbuseLog (ip, route, policy)"]
    J --> K["throw ApiException(RATE_LIMIT_EXCEEDED, retryAfter)"]
    K --> L["GlobalExceptionHandler<br/>429 ApiResponse.error + Retry-After"]
```

### 7.7 Domain data model

References are app-level id strings (no DB-enforced joins); the FK notation
below communicates intent.

```mermaid
erDiagram
    USER ||--o{ CAR : "owns (addedBy)"
    USER ||--o{ BOOKING : "places (user)"
    CAR  ||--o{ BOOKING : "booked as (car)"

    USER {
        string id PK
        string email "unique"
        long phonenumber "unique"
        string password "bcrypt"
        boolean emailVerified
    }
    CAR {
        string id PK
        string addedBy FK
        string title
        double price
        string driveType "LWD or RWD"
    }
    BOOKING {
        string id PK
        string user FK
        string car FK
        date startDate
        date endDate
        string status
        string paymentStatus
        boolean deleted
    }
```

---

## 8. Relationship to the   service

This is a behavioural port of `car_hub_backend`. Endpoints, validation rules,
OTP/booking flows, rate-limit policies, and the response envelope match the
original. Where Spring offers a cleaner idiom, it is used: typed configuration
properties over `process.env` lookups, an exception-advice over repeated
`sendError`, MapStruct over manual mapping, and async email over inline
`await`. The RabbitMQ / BullMQ queues that were commented out in the   app
are intentionally **not** ported.
