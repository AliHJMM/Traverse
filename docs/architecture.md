# Architecture

Traverse is a microservices-based Travel Management System with an Admin
Dashboard, built to be scalable, observable, and secure. This document
covers the runtime architecture; for the data model see
[`database-schema.md`](./database-schema.md).

---

## Services

Each service is bounded by a **business domain**, owns its **own data**, and
is **independently deployable and scalable**.

| Service | Responsibility | Data store |
|---|---|---|
| **Discovery Server** | Netflix Eureka registry — every service registers here; the Gateway resolves and load-balances through it | — |
| **API Gateway** | Single entry point. Routing via Eureka (`lb://<service>`), JWT verification, correlation-ID stamping | — |
| **Auth Service** | Register / login / logout / `/me`, JWT issuance, role-based access (ADMIN vs USER) | PostgreSQL (`auth`) |
| **User Service** | Admin CRUD on user profiles; delegates credentials to Auth Service via Feign | PostgreSQL (`users`) |
| **Travel Service** | Itinerary CRUD (destinations, dates, activities, accommodation, transport) + Neo4j route graph | PostgreSQL (`travel`) + Neo4j |
| **Payment Service** | Payment-method CRUD, Stripe & PayPal integration | PostgreSQL (`payment`) |
| **Admin Dashboard** | Angular SPA consuming everything through the Gateway | — |

Stateless backend services (auth/user/travel/payment) run with **≥2
replicas** behind the Gateway for load balancing and failover.

```
                         Browser (HTTPS)
                              │
                    ┌─────────▼──────────┐
                    │  Frontend (nginx)  │  TLS, serves SPA,
                    │  same-origin :443  │  reverse-proxies /api → Gateway
                    └─────────┬──────────┘
                              │
                    ┌─────────▼──────────┐
                    │    API Gateway     │  JWT check + correlation-id
                    │ (Spring Cloud GW)  │
                    └─────────┬──────────┘
                              │  lb://<service>  (Eureka round-robin)
        ┌──────────┬──────────┼───────────┬──────────────┐
        ▼          ▼          ▼           ▼              ▼
    Auth ×2    User ×2    Travel ×2   Payment ×2    Discovery (Eureka)
      │          │          │  │          │
   Postgres   Postgres   Postgres Neo4j Postgres      (+ Stripe / PayPal)
   (auth)     (users)    (travel)      (payment)
```

## Service discovery & load balancing

Services register with **Eureka** (Discovery Server) on startup. The Gateway
routes to `lb://<service-name>`; **Spring Cloud LoadBalancer** round-robins
across whatever replicas are currently registered — no hardcoded host:port.

- **Independent scalability:** `docker compose up --scale auth-service=N`
  and the Gateway immediately spreads traffic across the new instances.
- **Failover:** if a replica dies, Eureka evicts it and the Gateway
  reconverges on the survivors. Verified under load — see
  [`../loadtest/README.md`](../loadtest/README.md): 20 concurrent users,
  a replica killed mid-traffic, 96.9% success with a brief reconvergence
  dip, then full recovery. Load balancing measured as a near-even split
  (1620 vs 1621 requests across two replicas).

## Authentication & authorization

- **JWT in an httpOnly, `Secure`, `SameSite=Strict` cookie** — issued by
  Auth Service, never returned in a JSON body, never readable by JS (XSS
  can't steal it). `SameSite=Strict` is the CSRF mitigation for this
  stateless API.
- The **Gateway** verifies the token on every non-public request; each
  service re-verifies independently (defense in depth) and enforces
  **`ADMIN`-only** access on its `/api/**` endpoints.
- Service-to-service calls (User → Auth) forward the caller's own JWT as a
  Bearer header, so role checks apply to the real caller.

## Request tracing (centralized logging)

Every request gets a **correlation ID** at the Gateway (generated if absent),
propagated on the forwarded request and every downstream Feign call, and
included in every log line via SLF4J MDC. Logs are shipped to **Loki** by
**Promtail** (auto-discovers all containers) and searched in **Grafana** —
so a single request is traceable across services by its correlation ID.

See `docs/Grafana.png`: the same correlation ID appearing in both the
Gateway's and Auth Service's logs for one request.

## CI/CD

**Jenkins** (multibranch, configured entirely via Configuration-as-Code)
builds every branch and PR; **SonarQube** gates code quality. Pipeline:

```
Checkout → Backend build & test (6 services) → Frontend build & test
        → SonarQube analysis (7 modules) → Quality gate → Deploy (main only)
```

- **Unit + integration tests** run on every PR; a real commit-status check
  (`continuous-integration/jenkins/pr-merge`) **gates merges into `main`**.
- **SonarQube**: all 7 projects pass their quality gate (see
  `docs/SonarQube.png`), with JaCoCo (backend) + lcov (frontend) coverage.
- **Deploy** (main only) runs the **Ansible** playbook (see below), scoped
  to the application services so it never recreates the CI containers.

See `docs/Stages.png` (green pipeline) and `docs/Branches.png` (per-branch
/ per-PR builds).

## Provisioning & deployment (Ansible)

`ansible/` holds idempotent playbooks (`provision.yml`, `deploy.yml`) run
either manually from a shell or automatically by Jenkins' deploy stage.
`deploy.yml` runs three roles in order:

1. **`tls_certs`** — generates the self-signed TLS cert (idempotent).
2. **`vault_secrets`** — syncs secrets between Vault and `.env`.
3. **`compose_deploy`** — `docker compose up -d --build --wait`, scaling the
   backend services to 2 replicas, then health-checks Gateway + frontend.

Re-running is safe (idempotent): a second run reports `changed=0`. See
[`../ansible/README.md`](../ansible/README.md).

## Security

- **TLS/HTTPS** on the frontend; all `:80` traffic 301-redirects to `:443`.
  (Self-signed cert — Let's Encrypt needs a public domain, documented in
  [`../certs/README.md`](../certs/README.md).)
- **HashiCorp Vault** stores the secrets (JWT secret, DB passwords,
  Stripe/PayPal keys) — see `docs/Vault.png`.
- **Network restriction:** Postgres and Neo4j have **no host ports** —
  reachable only inside the Docker network.
- **Least privilege:** every backend container runs as a **non-root** user;
  APIs are ADMIN-only.
- **Patching:** Dependabot opens weekly, grouped, capped dependency PRs,
  each gated through the same pipeline.

## Tech stack

| Layer | Choice |
|---|---|
| Backend | Java 21, Spring Boot 3.3, Spring Cloud Gateway / Eureka / LoadBalancer, Spring Security, Spring Data JPA, Spring Data Neo4j, OpenFeign |
| Frontend | Angular 19 (standalone), Angular Material, Tailwind CSS v4, Stripe Elements |
| Data | PostgreSQL 16, Neo4j 5 |
| CI/CD | Jenkins, SonarQube |
| Infra | Docker Compose, Ansible, HashiCorp Vault |
| Observability | Loki, Promtail, Grafana |
| Load testing | k6 |
