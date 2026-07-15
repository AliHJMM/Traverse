# Traverse — Travel Management System: Phase 1 Plan

Phase 1 scope: infrastructure, automation, and the Admin Dashboard. The full
Travel Management System (customer-facing app) is a later phase — this repo
lays the foundation it will run on.

---

## 1. Architecture Overview

### Microservices (bounded by business domain)

| Service | Responsibility | Database |
|---|---|---|
| **API Gateway** | Single entry point, request routing, auth-token verification, rate limiting | — |
| **Auth Service** | Login/signup, JWT/OAuth2 issuance, role-based access control (admin vs user) | PostgreSQL |
| **User Service** | User CRUD, profile data | PostgreSQL |
| **Travel Service** | Itinerary CRUD: destinations, dates, duration, activities, accommodation, transportation. Uses Neo4j to model relationships between destinations/routes/activities (e.g. "connected to", "bookable with") for recommendations and route lookups | PostgreSQL + Neo4j |
| **Payment Service** | Payment method CRUD, Stripe & PayPal integration | PostgreSQL |
| **Admin Dashboard** | Frontend SPA consuming the above via the Gateway | — |

Each service:
- Owns its own data (no shared tables across services).
- Is independently deployable, containerized, and run with ≥2 replicas behind
  the gateway for load balancing/failover.
- Exposes health-check endpoints so failures don't cascade.

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

## 2. Build Order

We build incrementally, one phase at a time, each phase merged via its own
PR(s) before the next starts.

- [ ] **Phase 0 — Git workflow & repo hygiene**
  - Branch protection on `main`: PRs required, ≥1 approval, status checks
    must pass, no direct pushes, no force-push.
  - Branch naming convention (`feature/...`, `fix/...`, `chore/...`).
  - PR template, CONTRIBUTING.md, commit message convention.
  - Initial repo scaffold (monorepo layout below) + first commit.

- [ ] **Phase 1 — Infra skeleton**
  - `docker-compose.yml` for PostgreSQL + Neo4j, isolated Docker network.
  - Volumes for persistence, `.env`-based config, seed/init scripts.

- [ ] **Phase 2 — API Gateway**
  - Routing to downstream services, JWT verification middleware.

- [ ] **Phase 3 — Auth Service**
  - Signup/login, JWT issuance (+ OAuth2 if time permits), password hashing,
    role claims (admin/user).

- [ ] **Phase 4 — User Service**
  - Admin CRUD on users, cascading deletes (e.g. removing a user cleans up
    their bookings/payment methods).

- [ ] **Phase 5 — Travel Service**
  - Postgres schema for itineraries; Neo4j graph for destination/route
    relationships; admin CRUD.

- [ ] **Phase 6 — Payment Service**
  - Payment method CRUD, Stripe integration, PayPal integration (sandbox
    keys via secrets, never hardcoded).

- [ ] **Phase 7 — Admin Dashboard (frontend)**
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

## 3. Git Workflow (mandatory PRs)

- `main` is protected: no direct commits/pushes; merges only via approved PR
  with passing CI (Jenkins + SonarQube gate).
- One feature/fix/chore per branch, one branch per PR:
  `feature/<short-desc>`, `fix/<short-desc>`, `chore/<short-desc>`.
- PR description explains *why*, links the relevant plan phase/task.
- At least one review approval required before merge; reviewer checks
  correctness, test coverage, and the SonarQube report.
- Squash or rebase merge (no merge commits cluttering history) — TBD, confirm
  preference when repo is scaffolded.

---

## 4. Repo Layout (proposed)

```
Traverse/
├── services/
│   ├── gateway/
│   ├── auth-service/
│   ├── user-service/
│   ├── travel-service/
│   └── payment-service/
├── admin-dashboard/
├── docker/
│   └── docker-compose.yml
├── ansible/
│   └── playbooks/
├── jenkins/
│   └── Jenkinsfile(s)
├── docs/
│   ├── architecture.md
│   └── db-schema.md
└── Plan.md
```

---

## 5. Constraints Checklist (from requirements)

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

Set up Phase 0 (branch protection + repo scaffold), then proceed to Phase 1.
