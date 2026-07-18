# Traverse — Travel Management System: Phase 1 Plan

Phase 1 scope: infrastructure, automation, and the Admin Dashboard. The full
Travel Management System (customer-facing app) is a later phase — this repo
lays the foundation it will run on.

Backend services are built first, one by one; the Admin Dashboard frontend is
built last, once every backend service is done.

---

## 1. Tech Stack

| Layer | Choice |
|---|---|
| Backend services | Java + Spring Boot (Spring Cloud Gateway, Spring Security + JWT, Spring Data JPA, Spring Data Neo4j) |
| Frontend (Admin Dashboard) | Angular — conventional pairing with Spring Boot, built-in HTTP client/forms/router/DI fit a CRUD-heavy admin UI without assembling extra libraries |
| Relational DB | PostgreSQL |
| Graph DB | Neo4j |
| CI/CD | Jenkins + SonarQube |
| Containers/Provisioning | Docker, Ansible |
| Logging | ELK or Loki (TBD in Phase 10) |

---

## 2. Architecture Overview

### Microservices (bounded by business domain)

| Service | Responsibility | Database |
|---|---|---|
| **Discovery Server** | Eureka registry — every service registers itself here; the Gateway resolves and load-balances across replicas through it instead of fixed URIs | — |
| **API Gateway** | Single entry point, request routing via Eureka (`lb://service-name`), auth-token verification, rate limiting (Spring Cloud Gateway) | — |
| **Auth Service** | Login/signup, JWT/OAuth2 issuance, role-based access control (admin vs user) | PostgreSQL |
| **User Service** | User CRUD, profile data | PostgreSQL |
| **Travel Service** | Itinerary CRUD: destinations, dates, duration, activities, accommodation, transportation. Uses Neo4j to model relationships between destinations/routes/activities (e.g. "connected to", "bookable with") for recommendations and route lookups | PostgreSQL + Neo4j |
| **Payment Service** | Payment method CRUD, Stripe & PayPal integration | PostgreSQL |
| **Admin Dashboard** | Angular SPA consuming the above via the Gateway (built last) | — |

Each service:
- Owns its own data (no shared tables across services).
- Is independently deployable, containerized, and run with ≥2 replicas behind
  the gateway for load balancing/failover (`docker compose up --scale
  <service>=N`) — the Gateway discovers and round-robins across all of them
  via Eureka, not a hardcoded host:port.
- Exposes health-check endpoints so failures don't cascade.
- Registers with the Discovery Server (Eureka) except the Discovery Server
  and Gateway themselves.

### Why Neo4j here
PostgreSQL handles the transactional entities (users, bookings, payments).
Neo4j models the graph-shaped part of travel data — destinations connected by
routes, activities linked to accommodations/transportation — which is a
natural fit for graph queries (e.g. "find connected destinations within N
hops") that would be awkward as SQL joins.

### Request tracing
Every request gets a correlation ID at the Gateway, propagated through
service-to-service calls and included in all log lines, so a single request
can be traced across services in the centralized logging stack.

---

## 3. Build Order

We build incrementally, one phase at a time, each phase merged via its own
PR(s) before the next starts.

- [x] **Phase 0 — Git workflow & repo hygiene**
  - Branch protection ruleset on `main`: PR required before merging, status
    checks slot reserved for Jenkins (Phase 8), no direct pushes, no
    force-push, no deletions.
  - Required approvals set to 0 — GitHub blocks PR authors from approving
    their own PRs, and this is a solo project with no second reviewer.
    Status checks become the real validation gate once Jenkins exists.
  - Branch naming convention (`feature/...`, `fix/...`, `chore/...`).
  - Initial repo scaffold + first commit.

- [x] **Phase 1 — Infra skeleton**
  - `docker-compose.yml` for PostgreSQL + Neo4j, isolated Docker network.
  - Volumes for persistence, `.env`-based config.
  - Verified: both containers reach `healthy` and accept queries.

- [x] **Phase 2 — API Gateway**
  - Spring Boot + Spring Cloud Gateway. Routes to the 4 downstream services
    via Eureka (`lb://service-name`, see Phase 3.5). JWT verification filter
    reads the token from an httpOnly cookie (falls back to `Authorization:
    Bearer` for non-browser clients).
  - Verified: ran the built jar, exercised it with curl (401 on missing/bad
    token, correctly routed-through on valid token/public paths).

- [x] **Phase 3 — Auth Service**
  - Spring Boot + Spring Security + Spring Data JPA + Flyway. Register
    (first-ever account bootstraps as ADMIN; public registrations can't
    self-assign ADMIN afterwards), login, logout, `/me`.
  - JWT is set as an **httpOnly, Secure(prod)/SameSite=Strict cookie** —
    never returned in the JSON body and never readable by JS, to prevent
    XSS token theft. CSRF is mitigated by `SameSite=Strict` since the API
    is stateless (no CSRF token needed).
  - BCrypt password hashing. `users` table lives in its own `auth` Postgres
    schema, managed by Flyway migrations (not `hibernate.ddl-auto`).
  - Verified: 8 automated tests (JWT roundtrip/expiry + full MockMvc
    integration flow), then a real run against Postgres in Docker, then the
    full chain through the Gateway via `docker compose up --build`
    (container-to-container, not localhost) — register/login/me all work
    end to end with the real httpOnly cookie.

- [x] **Phase 3.5 — Eureka Discovery Server**
  - New `discovery-server` module (Spring Boot + Netflix Eureka Server,
    standalone/single-node, port 8761). Gateway and Auth Service converted
    from static routes to Eureka clients; Gateway routes now use
    `lb://<service-name>` (Spring Cloud LoadBalancer resolves + round-robins
    across whatever replicas are currently registered).
  - Verified for real, not just config: ran
    `docker compose up --scale auth-service=2`, confirmed both replicas
    registered in Eureka independently, confirmed the Gateway actually
    distributed requests across both (each initialized its request pipeline
    at a different point in the request sequence, not just one). Then
    killed one replica (`docker stop`) and confirmed the Gateway recovered
    to 100% success against the survivor within ~12s with no manual
    reconfiguration — real failover, not just "the config supports it."
  - Eureka client disabled in all test profiles (`eureka.client.enabled:
    false`) so test runs don't depend on network/timing.
  - Trade-off: default Eureka failover has a detection window (renewal
    interval, then eviction) — a killed instance can produce a handful of
    failed/slow requests for several seconds before the Gateway's local
    registry cache catches up. Acceptable for this phase; tunable later
    (`lease-renewal-interval`, retry filters) if needed.

- [x] **Phase 4 — User Service**
  - Spring Boot + Spring Data JPA + Flyway + OpenFeign + Eureka client.
    ADMIN-only CRUD on users (`/api/users`), own `users.user_profiles`
    Postgres schema/table (full name, phone, address, plus a denormalized
    copy of email/role for reads with no cross-service call).
  - Doesn't own credentials -- Auth Service still does. Create/update/delete
    call Auth Service via a Feign client (`lb://auth-service`, load-balanced
    through Eureka), forwarding the calling admin's own JWT as a Bearer
    header (`FeignAuthForwardingConfig`) so Auth Service's own
    role-escalation rules apply to the real caller, not to User Service.
  - Added matching ADMIN-only `PATCH /api/auth/users/{id}` and
    `DELETE /api/auth/users/{id}` to Auth Service for this; its
    `JwtCookieAuthenticationFilter` now also accepts a Bearer header (not
    just the cookie) for this kind of service-to-service call.
  - **Cascading delete**: deleting a user removes the local profile *and*
    calls Auth Service to remove the credential — verified for real (see
    below), not just implemented. Cascading into travels/payments will
    extend this once those services exist (Phases 5-6).
  - Bug caught during live verification: Feign's default HTTP client (JDK
    `HttpURLConnection`) silently can't send `PATCH` at all — the update
    call failed with no request ever reaching Auth Service. Fixed by
    switching Feign to Apache HttpClient5 (`feign-hc5` +
    `spring.cloud.openfeign.httpclient.hc5.enabled`). Direct-service curl
    testing (bypassing User Service) is what isolated this to the client,
    not Auth Service.
  - Verified for real: 5 automated tests (MockMvc + mocked Feign client),
    then full `docker compose up --build` across postgres,
    discovery-server, auth-service, user-service, gateway — logged in as
    the bootstrap admin, created a user, escalated another to ADMIN
    (proving JWT forwarding), listed, updated (email+role, confirmed the
    renamed login works), then deleted and confirmed *both* the profile
    (404) and the credential (login now fails) were gone.

- [x] **Phase 5 — Travel Service**
  - Spring Boot + Spring Data JPA (Postgres) + Spring Data Neo4j. ADMIN-only
    CRUD on `/api/travels`: a `Travel` aggregate (title, dates, computed
    `durationDays`) owning `destinations`/`activities`/`accommodations`/
    `transportations` as JPA one-to-many children with `cascade=ALL,
    orphanRemoval=true` — deleting or updating a travel cascades to all of
    them in Postgres, verified against a real DB (row counts confirmed 0
    after delete, not just "no error").
  - Neo4j graph: every itinerary's consecutive destinations get MERGEd into
    a `Destination` node + `CONNECTED_TO` relationship with a `tripCount`
    that increments on repeat routes, so the graph is a byproduct of real
    itineraries, not seed data. `GET /api/travels/destinations/{city}/nearby`
    does a real 1-2 hop Cypher traversal for recommendations — verified with
    `cypher-shell` directly against the graph, not just via the API.
  - **Two real bugs found and fixed during live verification** (neither
    surfaced by the mocked-Neo4j unit tests, only by hitting the actual
    running stack):
    1. `@EnableJpaRepositories`/`@EnableNeo4jRepositories` explicitly added
       to the application class turned out to be unnecessary — Spring
       Boot's autoconfiguration already separates JPA vs Neo4j repositories
       on its own ("entering strict repository configuration mode" in the
       logs) — and the explicit annotations interfered with it.
    2. The real root cause: combining `spring-boot-starter-data-jpa` and
       `spring-boot-starter-data-neo4j` in one app means Spring Boot's
       Neo4j auto-config backs off from creating a transaction manager
       entirely once it sees JPA already claiming the bean name
       `transactionManager` (both auto-config classes use that exact bean
       method name). Neo4j repository calls then fail with a
       `NullPointerException` deep in Spring Data's internal
       `TransactionTemplate` plumbing. Fixed by hand-defining both
       `PlatformTransactionManager` beans explicitly: JPA's marked
       `@Primary` (so every *unqualified* `@Transactional` — the vast
       majority of the codebase — keeps working exactly as before), and a
       distinctly-named `neo4jTransactionManager` that
       `DestinationGraphService` opts into explicitly
       (`@Transactional("neo4jTransactionManager")`).
  - Verified for real: 6 automated tests (MockMvc + mocked
    `DestinationGraphService`, so Postgres/JPA + security get exercised
    without needing a live Neo4j in the fast suite), then full
    `docker compose up --build` across postgres, neo4j, discovery-server,
    auth-service, travel-service, gateway — created a real 2-destination
    itinerary, confirmed the graph edge and `tripCount` via `cypher-shell`,
    confirmed `nearby` resolves both directions, then deleted the travel
    and confirmed every child row count dropped to 0 in Postgres.

- [x] **Phase 6 — Payment Service**
  - Spring Boot + Spring Data JPA. ADMIN-only CRUD on `/api/payments`. Never
    stores raw card numbers — only the opaque token/id the provider's own
    client-side SDK (Stripe.js / PayPal JS SDK) already produced, plus safe
    display metadata (brand/last4/expiry, or PayPal payer email).
  - Real integrations behind a `PaymentGatewayClient` interface: Stripe via
    the official `stripe-java` SDK, PayPal via a direct `RestClient` against
    their real v3 Vault "Payment Tokens" REST API (their full SDK is
    heavier than this needs). One `@Transactional` method per operation;
    "only one default payment method per user" cascades an unset onto
    whichever one previously held it.
  - Got free Stripe and PayPal sandbox accounts and put real credentials
    in `.env` for both. Stripe is **fully live-verified** end to end (see
    bug #2 below). PayPal is **OAuth-verified but not vault-verified**: the
    real `client_id`/`client_secret` correctly exchange for a real access
    token (confirmed live), but creating a Vault payment token from a raw
    card number requires PayPal's separate "Advanced Credit and Debit Card
    Payments" business approval — a review process, not a settings
    checkbox — which isn't available on a fresh sandbox app. This isn't a
    gap in our integration: PayPal's intended flow is for the browser
    (their JS SDK / hosted card fields) to produce the vault token, and
    our backend already correctly expects a finished token as input. Full
    live verification of the PayPal path is deferred to Phase 7, once the
    Angular frontend can produce a real token the intended way.
  - Bug #1 caught during live verification: the Flyway migration declared
    `expiry_month`/`expiry_year` as `SMALLINT`, but the JPA entity uses
    `Integer` (maps to `INTEGER`) — `ddl-auto: validate` caught the exact
    mismatch on startup (`found int2, expecting integer`) and crash-looped
    the container. Fixed by aligning the migration to `INTEGER`.
  - Bug #2 caught during live verification (with the real Stripe key): the
    first live create+delete round-trip worked for create, but delete
    failed with a real Stripe error — *"The payment method you provided is
    not attached to a customer so detachment is impossible."* `attach()`
    was only retrieving the PaymentMethod's card details, never actually
    attaching it to a Stripe Customer, so there was nothing for `detach()`
    to later undo. Fixed by adding a `payment.stripe_customers` table
    mapping each `userId` to one Stripe Customer (created on their first
    saved card, reused after that) and calling Stripe's real `.attach()`
    against it. Re-verified live: create → list → delete (real detach,
    confirmed on Stripe's side the PaymentMethod's `customer` field went
    back to `null`) → a second card for the same user correctly reused the
    same stored Customer instead of creating a new one.
  - Verified for real: 6 automated tests (MockMvc + mocked
    `StripePaymentGatewayClient`/`PaypalPaymentGatewayClient`), then full
    `docker compose up --build` with the real Stripe key — bootstrap admin
    login, full payment-method lifecycle against the actual Stripe API
    (not mocked), 401 unauthenticated, 404 on a missing id, and the PayPal
    path's real (currently credential-less) API call failing gracefully
    with a clean 502.

- [ ] **Phase 7 — Admin Dashboard (Angular, built last)**
  - Responsive UI (Chrome + Firefox), JWT-authenticated, CRUD screens for
    users/travels/payments, tested against the live services.

- [ ] **Phase 8 — CI/CD**
  - Jenkins pipeline: build → unit tests → SonarQube scan → (on main)
    deploy. Pipeline runs on every PR; merge blocked until it's green.

- [ ] **Phase 9 — Ansible**
  - Idempotent playbooks for provisioning hosts and deploying containers
    consistently (safe to re-run without side effects).

- [ ] **Phase 10 — Centralized logging**
  - ELK or Loki stack; every service ships structured logs with correlation
    IDs.

- [ ] **Phase 11 — Security hardening**
  - SSL/TLS via Let's Encrypt, HashiCorp Vault for secrets, network-level
    restriction of DB/internal service access, least-privilege service
    accounts, dependency patching cadence.

- [ ] **Phase 12 — Testing**
  - Unit tests per feature (required for CI to pass); integration/E2E tests
    as a bonus pass.

- [ ] **Phase 13 — Documentation**
  - Setup guide, DB schema docs (Postgres + Neo4j), deployment steps,
    architecture diagram.

- [ ] **Phase 14 — Bonus: Kubernetes**
  - Replace/augment Docker Compose with k8s manifests via Ansible for
    orchestration, load balancing, HA — if time permits after core phases.

---

## 4. Git Workflow (mandatory PRs)

- `main` is protected via a GitHub ruleset: no direct commits/pushes, no
  force-push, no deletions; merges only via PR.
- Required approvals = 0 (GitHub blocks self-approval and this is a solo
  project); once Jenkins exists (Phase 8), "require status checks to pass"
  becomes the real merge gate.
- One feature/fix/chore per branch, one branch per PR:
  `feature/<short-desc>`, `fix/<short-desc>`, `chore/<short-desc>`.
- Squash merge only (linear history required) — every PR becomes exactly one
  commit on `main`.

---

## 5. Repo Layout (proposed)

```
Traverse/
├── Backend/
│   ├── discovery-server/ (Spring Boot + Netflix Eureka Server)
│   ├── gateway/          (Spring Boot + Spring Cloud Gateway + Eureka client)
│   ├── auth-service/     (Spring Boot + Spring Security + Eureka client)
│   ├── user-service/     (Spring Boot + Spring Data JPA + OpenFeign + Eureka client)
│   ├── travel-service/   (Spring Boot + Spring Data JPA + Spring Data Neo4j)
│   └── payment-service/  (Spring Boot + Spring Data JPA + Stripe SDK + PayPal REST)
├── Frontend/              (Angular Admin Dashboard — built last)
├── ansible/
│   └── playbooks/
├── jenkins/
│   └── Jenkinsfile(s)
├── docs/
│   ├── architecture.md
│   └── db-schema.md
├── docker-compose.yml
└── Plan.md
```

---

## 6. Constraints Checklist (from requirements)

- [ ] PostgreSQL + Neo4j, both containerized
- [ ] Jenkins CI/CD, SonarQube quality gate
- [ ] Docker for all services, Ansible for provisioning/deployment
- [ ] Centralized logging with request tracing
- [ ] Admin Dashboard: user/travel/payment CRUD with cascading
      updates/deletes
- [ ] Stripe + PayPal integration
- [ ] JWT/OAuth2 auth on the dashboard, admin-only API access
- [ ] Responsive UI, Chrome + Firefox compatible
- [ ] Unit tests for every feature
- [ ] All changes via PR, code-reviewed, CI-gated before merge into `main`
- [ ] SSL/TLS (Let's Encrypt), Vault-managed secrets, least privilege,
      restricted DB/service network access, patch hygiene

---

## Next Step

Phases 0-6 are done — every backend service now exists (git workflow,
infra skeleton, API Gateway, Auth Service, Eureka discovery, User Service,
Travel Service, Payment Service). On `feature/payment-service`, pending
user add/commit/push + PR.

Stripe is fully live-verified with a real sandbox key. PayPal credentials
are in `.env` and OAuth-verified live; full Vault flow verification is
deferred to Phase 7 (needs a real browser-produced token, which requires
PayPal business approval to create any other way).

Next: Phase 7 — Admin Dashboard (Angular). This is the last remaining
backend-adjacent phase before moving into Phases 8+ (CI/CD, Ansible,
logging, security hardening).
