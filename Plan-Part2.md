# Traverse — Part 2 (lets-travel) Plan

Part 2 extends the existing Part-1 system (same repo, services, infra) into a
**role-aware** platform (Admin / Travel Manager / Traveler) with Elasticsearch
search, Neo4j recommendations, subscriptions, feedback, reports, and per-role
dashboards. Built on Part 1's foundation (gateway, Eureka, auth, Postgres,
Neo4j, payments, CI/CD, TLS, Vault, logging) — no infra re-grind.

## Architecture decisions
- **Roles**: `ADMIN`, `TRAVEL_MANAGER`, `TRAVELER` (migrate existing `USER` →
  `TRAVELER`). Public registration defaults to `TRAVELER`.
- **travel-service** becomes the engagement hub: travels (have it) +
  subscriptions, feedback, reports, Neo4j recommendations.
- **NEW `search-service`**: Elasticsearch indexing + search/autocomplete,
  kept as an independent service (satisfies the ES-independent-service audit
  point + the ES constraint).
- **payment-service** reused as-is (already multi-provider Stripe/PayPal).
- **Frontend**: role-aware areas — Traveler (search/browse/recommend/
  subscribe/pay/feedback/profile), Manager (create travels/dashboard/
  subscribers/analytics), Admin (extend existing + oversight).

## Build order (audit-weight order)

- [x] **P2-1 — Roles & RBAC foundation**
  - Role enums (auth/user/travel/payment) → ADMIN/TRAVEL_MANAGER/TRAVELER.
  - Flyway V2 migrations: `USER` → `TRAVELER` in auth.users + users.user_profiles.
  - `AuthService.resolveRole`: public signups default to TRAVELER; only an
    existing ADMIN can grant a privileged role (ADMIN or TRAVEL_MANAGER).
  - Frontend `Role` type widened to the 3 roles; user-form role dropdown +
    default updated.
  - All tests updated & green: 4 backend suites + 68 frontend tests.
  - (Endpoint-level role authorization is layered in per feature phase; the
    existing admin-only CRUD stays admin-only.)

- [ ] **P2-2 — Elasticsearch search-service**
  - New Spring Boot service + Elasticsearch container. Index travels;
    search + autocomplete APIs. travel-service publishes travel changes so
    the index stays consistent with Postgres.

- [ ] **P2-3 — Neo4j recommendations**
  - Extend the graph: traveler participation + feedback nodes/edges.
    Recommend travels from ≥3 fields (destination, activities, etc.) based
    on past participation + feedback.

- [ ] **P2-4 — Subscriptions (booking)**
  - Subscribe/unsubscribe to travels; 3-day-before-departure cutoff;
    subscriber lists per travel; ties into payment.

- [ ] **P2-5 — Feedback & Reports**
  - Feedback/ratings on participated travels; reports against managers/
    travelers; visibility to managers + admins.

- [ ] **P2-6 — Dashboards & stats**
  - Admin (top managers/travels, income reports, manager ranking, reports
    review), Manager (income/trips/travelers/analytics/subscribers),
    Traveler (history/stats/recommendations/preferred payment).

- [ ] **P2-7 — Frontend role-aware UI**
  - Traveler, Manager, Admin experiences; responsive; search→booking flow.

- [ ] **P2-8 — Testing, security, docs**
  - Unit/integration/E2E; RBAC enforcement checks; SQLi/XSS/GDPR notes;
    update docs.

## Bonus (optional, later)
- PWA, multilingual (i18n), innovative feature.
