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

- [ ] **Phase 4 — User Service**
  - Spring Boot + Spring Data JPA. Admin CRUD on users, cascading deletes
    (e.g. removing a user cleans up their bookings/payment methods).

- [ ] **Phase 5 — Travel Service**
  - Spring Boot + Spring Data JPA (Postgres) + Spring Data Neo4j. Itinerary
    CRUD; Neo4j graph for destination/route relationships.

- [ ] **Phase 6 — Payment Service**
  - Spring Boot + Spring Data JPA. Payment method CRUD, Stripe integration,
    PayPal integration (sandbox keys via secrets, never hardcoded).

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
│   ├── user-service/     (Spring Boot + Spring Data JPA)
│   ├── travel-service/   (Spring Boot + Spring Data JPA + Spring Data Neo4j)
│   └── payment-service/  (Spring Boot + Spring Data JPA)
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

Phases 0-3.5 are done (git workflow, infra skeleton, API Gateway, Auth
Service, Eureka discovery + verified load balancing/failover). On
`feature/eureka-discovery`, pending user add/commit/push + PR.

Next: Phase 4 — User Service.
