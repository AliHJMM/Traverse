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

- [x] **Phase 7 — Admin Dashboard (Angular, built last)**
  - Angular 19 standalone app (no NgModules), Angular Material for
    components + **Tailwind CSS v4 for all layout/spacing/styling — no
    hand-written SCSS/CSS anywhere** in the project (a deliberate,
    explicit choice; `angular.json`'s component schematic defaults to
    `style: none` so `ng generate` never scaffolds a stylesheet again).
    JWT auth via the same httpOnly/SameSite=Strict cookie the backend
    already issues — no token handling in JS at all, just a functional
    `authInterceptor` (`withCredentials`) and a functional `adminGuard`
    that calls `/api/auth/me`.
  - Served same-origin: nginx serves the Angular build on `:80` and
    reverse-proxies `/api/` to the Gateway container, so the cookie's
    `SameSite=Strict` still applies (a split origin would have broken
    auth). Stripe's publishable key is injected at container *start*
    (not baked into the build) via an `env.js` + `envsubst`
    entrypoint, so the same image works across environments without a
    rebuild.
  - CRUD screens for Users, Travels (nested `FormArray`s for
    destinations/activities/accommodations/transportations), and
    Payments — the last with a real `@stripe/stripe-js` Elements card
    field (not a fake form), tokenized client-side and only ever
    sending Stripe's token to the backend.
  - Unit tests: Karma + Jasmine, headless Firefox (`karma-firefox-launcher`)
    to match the Chrome+Firefox compatibility requirement. Recurring
    gotcha, fixed consistently across every list component's spec:
    importing `MatDialogModule` into a standalone component's own
    `imports` (when the component only needs the *injectable*
    `MatDialog` service, not template directives) creates a
    component-scoped `MatDialog` provider that silently shadows any
    `TestBed`-level mock — `spyOn(dialog, 'open')` then fails or never
    registers. Fixed by dropping `MatDialogModule` from those
    components' imports and providing the mock via
    `{ provide: MatDialog, useValue: dialogSpy }` instead.
  - **Real backend bug found only by live browser testing** (the
    existing MockMvc `TravelFlowIntegrationTest` structurally couldn't
    catch it — its class-level `@Transactional` keeps one Hibernate
    session open for the whole test, masking the issue): with
    `open-in-view: false`, `TravelService.findAll()`/`findById()`
    returned entities whose four lazy `@OneToMany` collections
    (destinations/activities/accommodations/transportations) blew up
    with `LazyInitializationException` the moment the controller
    touched them to build the response DTO, since the transaction (and
    session) had already closed by then. A single `@EntityGraph`
    across all four wasn't an option — Hibernate throws
    `MultipleBagFetchException` when more than one `List`-typed
    collection is eagerly joined in one query. Fixed by explicitly
    calling `Hibernate.initialize()` on each collection while still
    inside the read-only transaction.
  - Verified for real: full unit suite green, then
    `docker compose up --build` across the entire stack (postgres,
    neo4j, discovery-server, gateway, auth/user/travel/payment
    services, frontend), driven with a real headless-Chromium
    (Playwright) session against `http://localhost` — logged in,
    confirmed the Users/Travels/Payments sidenav, created/edited/
    deleted a user (edit persistence checked both in the UI and via a
    follow-up `curl` against `/api/users`, not just the DOM), created
    and deleted a travel with a nested destination, and confirmed the
    Stripe card element genuinely renders as a `js.stripe.com` iframe
    inside the Add Payment Method dialog. Zero console errors besides
    one expected, benign 401 from the initial `/api/auth/me`
    session-check that fires before any login cookie exists.

- [x] **Phase 8 — CI/CD**
  - Jenkins (custom image: Maven, Node 20, headless Firefox, Docker CLI,
    sonar-scanner, all baked in) + SonarQube (Community Edition, own
    Postgres) added to `docker-compose.yml`. Jenkins provisioned entirely
    declaratively via Configuration-as-Code (`jenkins/casc.yaml`) — admin
    user, GitHub/SonarQube credentials, SonarQube server connection, and a
    multibranch pipeline job (`jenkins/Jenkinsfile`) that discovers every
    branch and PR via the GitHub API and reports a real commit status back
    (`continuous-integration/jenkins/branch`) — no setup wizard, no manual
    clicking. Trigger is a periodic scan (every 2 min) rather than a
    GitHub webhook, since Jenkins has no public endpoint to receive one
    on a local machine — deliberate choice over standing up ngrok, see
    the discussion below.
  - Pipeline: 6 backend services build+test sequentially → Angular
    build+test (Firefox headless) → SonarQube analysis on all 7 modules →
    quality gate wait (resolves via a SonarQube→Jenkins webhook) → deploy
    (`docker compose up -d --build`, `main` only).
  - **Real bugs found and fixed, in the order hit** (this phase was almost
    entirely live debugging against the actual pipeline, not just writing
    config):
    1. `timestamps()` in `options {}` failed to parse — the Timestamper
       plugin was never added to `plugins.txt`.
    2. Every backend module failed to compile with "release version 21
       not supported" — the Jenkins image was `jdk17`, but the project
       targets Java 21. Switched to `jenkins/jenkins:lts-jdk21` and
       pinned `JAVA_HOME`/`PATH` explicitly, since Debian's `maven`
       package would otherwise pull in its own (older) default JDK
       alongside the one Jenkins itself bundles.
    3. Maven's local repo (`/root/.m2`) wasn't a named volume, so every
       container recreate re-downloaded the entire dependency tree for
       all 6 modules from scratch. Added a dedicated volume.
    4. The `matrix` stage's 6 backend builds running 3-wide in parallel
       pushed the Jenkins container to ~3.3GB / 44% of Docker Desktop's
       entire 7.4GB VM allocation (shared with the other 13 already-running
       containers) and silently OOM-killed a build's own child process —
       Jenkins kept reporting "building" for a process that no longer
       existed. Setting `numExecutors: 1` turned out not to fix this:
       Declarative `parallel`/`matrix` branches run as concurrent threads
       inside the one node the pipeline already allocated, not gated by
       executor count at all. Fixed for real by rewriting the backend
       stage as a genuine sequential loop.
    5. The deploy stage's `docker compose up -d --build` would have run
       from Jenkins's own git checkout, which never has `.env` (gitignored,
       so a fresh clone can't produce it) — every `${VAR}` in
       `docker-compose.yml` would have resolved empty. Fixed by bind-mounting
       the real host project directory (`.:/host-project:ro`) into the
       Jenkins container and pointing the deploy stage at it instead —
       Jenkins already has `docker.sock` mounted for docker-outside-of-docker,
       so this reaches the same Docker engine already running the real stack.
    6. The GitHub branch source's Job DSL config had an empty `<traits/>`
       block — `github {}` doesn't auto-add branch/PR discovery, it has to
       be requested explicitly (`gitHubBranchDiscovery`/
       `gitHubPullRequestDiscovery`), and the required-field combination
       for `configuredByUrl`/`repositoryUrl`/`repoOwner`/`repository` took
       several iterations of real validation-error messages to get right.
    7. `karma.conf.js` hardcoded a **Windows** Firefox path
       (`C:\Program Files\Mozilla Firefox\firefox.exe`) as its
       `FIREFOX_BIN` fallback — correct for local dev on this machine,
       fatal on the Linux Jenkins container. Fixed to only apply that
       fallback on `process.platform === 'win32'`.
    8. Docker Desktop's engine itself went unresponsive (500s on basic
       `docker ps`/`docker info` calls) under the combined load of the
       full 14-container stack plus 6 parallel JVMs — a real host resource
       ceiling, not a config bug. Resolved by the user restarting Docker
       Desktop; all state survived since everything lives in named volumes.
    9. SonarQube's bundled Elasticsearch needs a raised `vm.max_map_count`;
       on Docker Desktop/WSL2 this isn't namespaced per-container, so a
       compose-level `sysctls:` override crashes container creation
       outright rather than being ignored. Fixed at the host level instead
       (`wsl -d docker-desktop sysctl -w vm.max_map_count=262144`) —
       doesn't persist across a Docker Desktop restart, documented as a
       one-time-per-restart step.
    10. The `sonarqube:community` image doesn't ship `wget`, so its
        Docker healthcheck always failed even though the service worked
        fine — switched to `curl` with an actual status-body check.
    11. A genuinely wedged Jenkins executor (queued build stuck
        "waiting for next available executor" with nothing actually
        running) needed a container restart to clear — state persisted
        fine since it's all in the `jenkins_home` volume.
    12. A real merge conflict between `main` (which had gotten an earlier,
        pre-fix version of these same Jenkins files pushed directly) and
        this branch's fixes — resolved by taking the newer/fixed side for
        every conflicting file, verified no markers remained, then handed
        back to the user for the actual `git add`/commit/push (git
        operations stay theirs throughout this project).
    13. **The deploy stage itself killed Jenkins mid-pipeline (build #13 on
        `main`)**: `docker compose -p traverse up -d --build` with no
        service list recreates *every* service in the compose file,
        including `jenkins`/`sonarqube`/`sonarqube-db`, which live in the
        same compose file as CI infra. Compose stops the old container
        before starting its replacement — which killed the very Jenkins
        process running the pipeline, mid-command. The pipeline paused for
        the Jenkins restart, resumed, then failed
        (`wrapper script does not seem to be touching the log file`,
        `ERROR: script returned exit code -1`) since its shell wrapper
        process had already been killed. Fixed by scoping the deploy
        stage's `up -d --build` to an explicit list of the 9 actual
        application services only. Also hit a real squash-merge divergence
        while landing this: since every PR into `main` is squash-merged
        (one flattened commit, no shared ancestry with the feature
        branch), a second PR from the same long-lived `feature/jenkins-cicd`
        branch conflicted against `main` in `jenkins/Jenkinsfile` even
        though the tree content was identical — GitHub reported
        `mergeable_state: dirty` until `main` was explicitly re-merged into
        the feature branch (trivial conflict, one side was a strict
        superset of the other) and repushed.
  - **Live-verified for real**: a full pipeline run on `feature/jenkins-cicd`
    went genuinely green end to end — all 6 backend services built and
    tested, all 68 Angular tests passed on the real Linux CI container,
    SonarQube analyzed all 7 modules successfully, and the quality gate
    resolved via the webhook in under a second (`Quality gate is 'OK'`).
    Deploy correctly skipped (branch-gated to `main` only). GitHub branch
    protection (a ruleset, not classic branch protection) was then updated
    via the API to add a `required_status_checks` rule for
    `continuous-integration/jenkins/branch`, alongside the existing
    no-force-push/no-deletion/PR-required/linear-history rules from
    Phase 0 — the slot that was reserved back then is now actually filled.
  - **Deploy stage re-verified after the bug #13 fix (PR #15, merged into
    `main`)**: a real `main` build (build #14) ran the corrected,
    scoped `docker compose -p traverse up -d --build <9 app services>`
    against the live stack — all 9 application containers recreated with
    fresh images (confirmed via each container's new start time), while
    `traverse-jenkins`/`traverse-sonarqube`/`traverse-sonarqube-db` stayed
    up the entire time, completely untouched. Pipeline finished `SUCCESS`,
    quality gate `OK`. Post-deploy smoke check: gateway `/actuator/health`
    → 200, frontend → 200, discovery-server responsive. The deploy stage
    is now confirmed safe to run repeatedly without taking down the CI
    infrastructure that runs it.
  - **CI/CD requirements audit pass (PR #16)**: went through the project's
    own audit checklist against the live SonarQube dashboard, not just the
    quality gate, and found three real gaps the gate doesn't catch because
    its default condition only looks at new-code violations:
    1. **Coverage reporting was completely disconnected** — all 7 projects
       showed 0.0% coverage in SonarQube despite every test suite actually
       passing. Root cause: no JaCoCo plugin in any of the 6 backend
       `pom.xml`s, and no coverage reporter wired into `karma.conf.js` for
       the Angular suite, so there was never a report for Sonar to read.
       Fixed by adding `jacoco-maven-plugin` (bound to the `test` phase,
       `prepare-agent` + `report` goals) to every backend service and
       passing `-Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml`
       in the Jenkinsfile's sonar loop; added `karma-coverage` (already a
       devDependency, just never wired in) to `karma.conf.js` with an
       `lcov` reporter, `ng test --code-coverage`, and
       `-Dsonar.javascript.lcov.reportPaths=coverage/lcov.info`. Verified
       locally before pushing: real JaCoCo XML generated for all 6 backend
       services, real `lcov.info` generated for the frontend (83% statement
       coverage, 68/68 tests passing).
    2. **4 backend services carried an open CRITICAL "CSRF disabled"
       vulnerability** (`java:S4502`) in SonarQube, sitting unresolved.
       This is Sonar correctly flagging `.csrf().disable()` — but it's a
       deliberate, already-documented design choice (Phase 3: stateless
       JWT in an httpOnly `SameSite=Strict` cookie, which is itself the
       CSRF mitigation — there's no ambient session for a forged
       cross-site request to ride on, so re-enabling Spring Security's
       CSRF filter would add no real protection). Marked all 4 as
       resolved/`WONTFIX` in SonarQube via the API with a justification
       comment, rather than changing correct code to satisfy a scanner
       that can't see the actual auth architecture.
    3. **Frontend had 30 open "bugs"**: 29 were `Web:InputWithoutLabelCheck`
       findings on `<input matInput>` elements that already sit inside a
       `<mat-form-field>` with a sibling `<mat-label>` — Angular Material
       wires a real `for`/`id` pair between them at render time, but
       Sonar's static HTML analyzer only sees the raw template source and
       doesn't know custom elements like `<mat-label>` resolve to a real
       `<label>` at runtime. Fixed by adding an explicit `aria-label`
       matching each field's visible label text across all 29 inputs
       (login, users, travels, payments forms) — redundant with Material's
       own wiring but makes the association explicit in the literal
       markup, which is what the static check needed. The 30th was a real
       bug: `payment-form-dialog.component.ts`'s `ngAfterViewInit` was
       declared `async ... : Promise<void>`, violating the `AfterViewInit`
       interface's `void` contract and meaning a rejected promise there
       would become a silent unhandled rejection instead of surfacing.
       Fixed to match the same `void this.mountStripeCardElement();`
       pattern already used correctly elsewhere in the same file
       (`onProviderChange`).
  - Re-verified after all of the above: all 6 backend `mvn clean verify`
    runs green with real JaCoCo output, all 68 Angular tests + `ng build`
    green with real lcov output, pushed through the same PR → Jenkins
    green → squash-merge → live `main` build flow as PR #15.

- [x] **Phase 9 — Ansible**
  - `ansible/` — single-host project (everything runs as containers on one
    Docker Desktop engine), so inventory is one `traverse_host` group
    (`localhost`, `ansible_connection=local`) rather than remote SSH targets
    — a deliberate, honest choice documented in `ansible/README.md` rather
    than faking multi-host SSH for no functional reason. Kept as a real
    inventory group (not hardcoded `hosts: localhost`) so pointing this at
    an actual remote Docker host later is an inventory change, not a
    playbook rewrite.
  - Two playbooks: `provision.yml` (host prerequisites — manual, WSL only)
    and `deploy.yml` (`docker compose up -d --build --wait`, scoped to the
    9 application services only, same exclusion of `jenkins`/`sonarqube`/
    `sonarqube-db` as Phase 8's deploy stage, for the same reason —
    recreating CI infra would kill the pipeline running it). `deploy.yml`
    now also scales the 4 stateless backend services to 2 replicas each
    via `--scale` on every deploy (`group_vars/all.yml`), which is what
    actually gives the "multiple replicas for load balancing/failover"
    requirement a standing, automated home instead of the one-off manual
    `--scale` flag used back in Phase 3.5's verification.
  - **Integrated into Jenkins, not left standalone**: `jenkins/Dockerfile`
    now installs `ansible-core`, and the Jenkinsfile's deploy stage
    (`main` only) calls `ansible-playbook playbooks/deploy.yml` instead of
    a raw `docker compose` shell line, with `-e health_check_gateway_url=
    http://gateway:8080/...` / `-e health_check_frontend_url=
    http://frontend:80/` overrides — "localhost" inside the Jenkins
    container means the Jenkins container itself, not the gateway/frontend
    containers, so the container-network service names have to be passed
    explicitly for that context (the default group_vars values assume a
    WSL/host run instead, where published ports on `localhost` are
    correct).
  - **Real bugs found during live verification** (not just written and
    assumed correct):
    1. `group_vars/all.yml` placed at the `ansible/` top level was silently
       never loaded — Ansible only auto-discovers `group_vars/`/
       `host_vars/` alongside the *playbook* file's directory or the
       *inventory* file's directory, not an arbitrary parent. Every
       variable came back "undefined." Fixed by moving it to
       `ansible/playbooks/group_vars/all.yml`.
    2. A real WSL/DrvFs quirk: running `ansible-playbook` from
       `/mnt/c/Users/AliHa/Traverse/ansible` prints "Ansible is being run
       in a world writable directory" and silently skips loading
       `ansible.cfg` from the cwd entirely (DrvFs reports all files as
       world-writable to WSL's Linux permission model, regardless of real
       NTFS ACLs) — `roles_path`/`inventory` never applied, "role not
       found" even though the file is right there. Fixed by setting
       `ANSIBLE_CONFIG` explicitly (bypasses the cwd auto-discovery check),
       both for manual WSL runs and in the Jenkinsfile's deploy stage.
  - **Live-verified for real, three separate ways**:
    1. `provision.yml` against the real host from WSL: Docker/Compose
       presence confirmed, `.env` checked (no missing/placeholder values
       at the time), `vm.max_map_count` found already at 262144 from an
       earlier manual fix and correctly left untouched — no spurious
       `changed`.
    2. `deploy.yml` from WSL, run twice back-to-back: **first run** did a
       real `docker compose up -d --build --scale ...` — confirmed via
       `docker compose ps` that `auth-service`/`user-service`/
       `travel-service`/`payment-service` each came up as 2 replicas,
       while `jenkins`/`sonarqube`/`sonarqube-db` stayed completely
       untouched (`Up 24 minutes` unchanged) the entire time; Gateway
       health check retried ~6 times (~30s) while the new containers
       finished booting/registering with Eureka, then passed, frontend
       passed immediately. **Second run**: `changed=0` across every task —
       no containers recreated, both health checks passed on the first
       try with zero retries. Confirms "safe to re-run without side
       effects" as an observed fact, not just an intended property.
    3. Rebuilt `jenkins/Dockerfile` with `ansible-core` added, recreated
       only the `traverse-jenkins` container (state intact via the
       `jenkins_home` volume, Jenkins came back up clean), confirmed
       `ansible-playbook [core 2.19.4]` runs inside it, then ran the
       *exact* command the Jenkinsfile's deploy stage now runs — from
       inside that real Jenkins container, against `/host-project`, with
       the container-network health-check URL overrides — and it
       correctly reached `http://gateway:8080/actuator/health` and
       `http://frontend:80/` by container name (not localhost), while
       `jenkins`/`sonarqube`/`sonarqube-db` again stayed untouched. This
       exercises the actual code path the pipeline will run on the next
       `main` build, without needing to merge first just to find out.

- [x] **Phase 10 — Centralized logging**
  - **Loki + Promtail + Grafana**, not ELK — chosen deliberately over a
    second Elasticsearch on a host that already runs one for SonarQube
    (Phase 8 bug #9), which mattered given the real memory/CPU pressure hit
    repeatedly during Phase 9. Single-binary Loki (filesystem storage,
    `logging/loki-config.yml`) since this is a single-host project, same
    reasoning as Ansible's inventory choice. Promtail auto-discovers every
    container via the mounted `docker.sock` (docker-outside-of-docker, same
    approach Jenkins already uses) — no per-service scrape config, new
    services/replicas are picked up automatically. Grafana's Loki
    datasource is provisioned declaratively
    (`logging/grafana-provisioning-datasources`), not clicked through the
    UI.
  - **Correlation ID propagation, not just log shipping**: a request is
    only actually traceable if the same ID appears in every service that
    touched it, which needed real code, not just infra. Gateway's new
    `CorrelationIdFilter` (a `GlobalFilter`, `HIGHEST_PRECEDENCE` — ahead of
    `JwtAuthenticationFilter`, so even rejected/401 requests get one) reads
    or generates the ID, stamps it on the forwarded request, and logs an
    access-log-style line. Each of the 4 request-handling backend services
    (auth/user/travel/payment — Discovery Server has no business-request
    filter chain to hook into and was deliberately skipped) got a matching
    Servlet `CorrelationIdFilter` that reads the header into MDC
    (`addFilterBefore` the existing `JwtCookieAuthenticationFilter`) so
    `%X{correlationId}` shows up in every log line via a new
    `logging.pattern.console` in each `application.yml`. User Service's
    `FeignAuthForwardingConfig` got a second `RequestInterceptor` forwarding
    the MDC value onto its Auth Service calls, so correlation survives that
    service-to-service hop too.
  - **Two real bugs found during live verification** (not just written and
    assumed correct):
    1. The correlation ID showed up **twice**, comma-joined, in the client
       response header — Gateway's filter unconditionally added it, and
       Spring Cloud Gateway separately copies back the same header from the
       proxied downstream response (which had *also* set it, to the same
       value). Fixed by only adding it in the Gateway if the downstream
       response doesn't already carry it.
    2. Bigger one: the first version of each downstream service's filter
       only populated MDC — it never logged anything itself. A plain
       successful CRUD request often doesn't hit any other log statement in
       that service, so a correlation ID that traced fine through the
       Gateway's own access-log line had **nothing to find** in that
       service's logs at all. Caught by literally registering a real test
       user through the Gateway and querying Loki for the returned
       correlation ID — Auth Service's log stream came back empty. Fixed by
       having each service's filter log its own access-log-style line too
       (method/path/status/duration), matching what the Gateway already
       did.
  - **Live-verified for real**: registered a real user through the running
    Gateway, captured the single (post-fix) `X-Correlation-Id` response
    header, then queried Loki's HTTP API directly (not just eyeballing
    Grafana) for that exact ID — it came back with matching log lines from
    **both** `traverse-gateway` and `traverse-auth-service-1`, same
    timestamp, same request. Confirmed the Loki datasource is live and
    default in Grafana via its own API. The User Service → Auth Service
    Feign-forwarding hop is code-reviewed and consistent with the
    already-proven Gateway → service mechanism, but wasn't independently
    live-traced this session (would need real admin credentials this
    session didn't have on hand) — noted here rather than silently assumed,
    same honesty standard as Phase 6's PayPal Vault gap.
  - All 5 modified backend modules (auth/user/travel/payment/gateway)
    recompiled and re-ran their full existing test suites clean with the
    new filter in the Spring Security chain before any deploy — no
    regressions from adding it.

- [x] **Phase 11 — Security hardening**
  - **SSL/TLS**: self-signed cert, not Let's Encrypt — Let's Encrypt can
    only issue for a real, publicly-resolvable domain, which a local
    Docker Desktop deployment doesn't have. A deliberate, documented choice
    (`certs/README.md`) rather than faking it or skipping TLS entirely,
    structured so a real Let's Encrypt cert drops in later as a volume-mount
    change, not a redesign. `Frontend/nginx.conf` now 301-redirects all
    `:80` traffic to `:443`; nothing is ever served over plain HTTP.
    `COOKIE_SECURE` flipped to `true` now that real HTTPS exists.
  - **HashiCorp Vault** (dev mode — in-memory storage, fixed root token;
    an honest, documented trade-off, not treated as production-grade) is
    now the source of truth for 13 real secrets (JWT secret, DB passwords,
    Stripe/PayPal keys, CI tool admin passwords/tokens) — not per-service
    Spring Cloud Vault integration, but Ansible itself: the new
    `vault_secrets` role reads them from Vault and syncs them into `.env`
    before every deploy, via `lineinfile` per key so every comment and
    every non-secret line in `.env` survives untouched. Self-healing across
    a Docker Desktop restart (which wipes dev-mode's in-memory store): if
    Vault comes up empty, it's reseeded *from* `.env`'s last-known values;
    otherwise `.env` is updated *from* Vault.
  - **Network restriction**: `postgres` and `neo4j` lost their host port
    mappings entirely — reachable only from other containers on
    `traverse-net`, confirmed via a real `Test-NetConnection` from the host
    (`TcpTestSucceeded: False` on both 5432 and 7687 post-deploy). Vault's
    port stays published deliberately: its security model is
    authentication (every read needs a valid token), not network
    isolation, so host access doesn't violate "internal network or
    authenticated endpoints only" — the requirement explicitly allows
    either.
  - **Least privilege**: all 6 backend Dockerfiles (discovery-server,
    gateway, auth/user/travel/payment-service) previously ran as root by
    default (no `USER` directive) — now run as a dedicated non-root `app`
    user, confirmed via `docker exec ... whoami` returning `app` on the
    real running containers. Postgres/Neo4j/nginx already run as non-root
    by default in their official images, no change needed there. Full
    per-service Postgres role scoping (each backend service authenticating
    as its own least-privilege DB role instead of one shared admin
    account) is a known, documented limitation deferred past this session
    — noted honestly rather than silently skipped, same standard as
    Phase 6's PayPal Vault gap.
  - **Patching cadence**: `.github/dependabot.yml` — weekly automated PRs
    across all 6 Maven modules, the Angular frontend, all 7 Dockerfiles,
    and the Jenkins image, each still gated through the same Jenkins
    pipeline (build/test/SonarQube/quality gate) as any other PR. A real
    enforced mechanism, not a written policy nobody follows.
  - **Real bugs found during live verification**:
    1. `select('length')` in the new `vault_secrets` role's `.env`-parsing
       task — `length` is a Jinja *filter*, not a *test*; using it as a
       test threw `KeyError: 'length'`. Fixed to bare `select` (default
       truthy test), since `regex_findall` already returns `[]` (falsy)
       for non-matching lines.
    2. A `docker compose up -d --build` mid-run failure (`ERRNO2: No such
       file or directory`) during the very first full live-deploy attempt
       — didn't reproduce on an immediate clean re-run (which succeeded
       fully, `exit 0`), and all containers from the "failed" run had
       actually come up fine underneath. Consistent with the same
       Docker-Desktop-under-host-pressure pattern hit repeatedly earlier
       this session (Phase 8 bug #8, and a real Docker Desktop restart
       needed mid-Phase-11 after the engine returned 500s on basic `docker
       ps` calls) rather than a logic bug — noted honestly as "not fully
       explained, resolved on retry" instead of claiming a root cause I
       don't actually have evidence for.
  - **Live-verified for real, not just written**: full `ansible-playbook
    playbooks/deploy.yml` run end-to-end (cert generation, Vault
    seed-then-sync, scoped `docker compose up`) completed clean twice in a
    row (second run correctly idempotent — cert and Vault-seed steps both
    skipped, only `auth-service` recreated to pick up a `.env` change).
    Registered a real user and logged in through `https://localhost` (not
    a mocked request) — got back a real `Set-Cookie` header reading
    `Secure; HttpOnly; SameSite=Strict`, and Vault's own API confirmed all
    13 managed secrets present with real values matching `.env`.

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
│   ├── inventory/
│   ├── playbooks/         (provision.yml, deploy.yml, site.yml, group_vars/)
│   └── roles/             (host_provision, tls_certs, vault_secrets, compose_deploy)
├── jenkins/
│   └── Jenkinsfile(s)
├── logging/
│   ├── loki-config.yml
│   ├── promtail-config.yml
│   └── grafana-provisioning-datasources/
├── certs/                 (Ansible-generated self-signed TLS cert, gitignored)
├── .github/
│   └── dependabot.yml
├── docs/
│   ├── architecture.md
│   └── db-schema.md
├── docker-compose.yml
└── Plan.md
```

---

## 6. Constraints Checklist (from requirements)

- [x] PostgreSQL + Neo4j, both containerized
- [x] Jenkins CI/CD, SonarQube quality gate
- [x] Docker for all services, Ansible for provisioning/deployment
- [x] Centralized logging with request tracing
- [ ] Admin Dashboard: user/travel/payment CRUD with cascading
      updates/deletes
- [ ] Stripe + PayPal integration
- [ ] JWT/OAuth2 auth on the dashboard, admin-only API access
- [ ] Responsive UI, Chrome + Firefox compatible
- [ ] Unit tests for every feature
- [x] All changes via PR, code-reviewed, CI-gated before merge into `main`
- [x] SSL/TLS (self-signed, documented Let's Encrypt path), Vault-managed
      secrets, least privilege, restricted DB/service network access,
      patch hygiene

---

## Phase 7 hardening pass (post-build correctness + design)

Before moving into Phase 8, did a dedicated pass across the whole stack to
verify correctness (no 500s) and raise the UI from functional to genuinely
polished. Real issues found and fixed:

- **Reversed travel dates silently accepted**: `endDate` before `startDate`
  produced a negative `durationDays` instead of being rejected. Added a
  cross-field `@AssertTrue` validator on `CreateTravelRequest`/
  `UpdateTravelRequest`.
- **Misleading 401s on malformed requests, across all 4 services**: bad
  JSON, wrong HTTP method, non-numeric path IDs, invalid enum values, and
  missing `Content-Type` were each correctly resolved as 400/405/415
  *internally* by Spring's `DefaultHandlerExceptionResolver`, but that
  resolver's `response.sendError(...)` triggers a servlet-container error
  forward back through the same Spring Security filter chain — and that
  second pass reports the request as unauthenticated, silently turning the
  correct status into a misleading 401. Fixed by adding explicit
  `@ExceptionHandler`s for `HttpMessageNotReadableException`,
  `MethodArgumentTypeMismatchException`, `HttpRequestMethodNotSupportedException`,
  and `HttpMediaTypeNotSupportedException` to every service's
  `GlobalExceptionHandler`, so the correct status returns directly with no
  forward. Not a crash and not reachable through the real UI (which always
  sends well-typed requests), but a real correctness gap for direct API
  callers.
- **Misleading conflict message on oversized input**: a 5000-character
  `fullName` overflowed its `VARCHAR(255)` column and surfaced as "Email
  already registered" (the generic `DataIntegrityViolationException`
  handler hardcoded that message for any cause). Added `@Size` validation
  on `fullName`/`phone`/`address` so it's now a clear 400 instead of a
  confusing DB-level conflict.
- **UI was functionally correct but visually bare**: added a real Material
  theme (see Phase 7's Tailwind/Material notes above — the theme file was
  missing structural CSS for form-field outlines, causing a stray border
  line), consistent icon-chip headers on every dialog, a proper red delete
  button (`color="warn"` silently fell back to primary blue because the
  stripped-down M3 prebuilt theme doesn't define per-variant button
  colors — fixed with direct Tailwind utility classes instead of patching
  Material's theme internals), icon-labeled sections in the travel form,
  user avatar initials, and icon+text empty states across all three list
  screens.

Re-verified after every fix: all 4 backend test suites, all 68 Angular
unit tests, and the full Playwright CRUD flow (users/travels/payments,
including the Stripe iframe) all still pass clean.

## Next Step

Phases 0-8 are done. Every backend service exists, the Angular Admin
Dashboard is built and live-verified, and Jenkins + SonarQube now run a
genuinely green CI pipeline on every branch/PR with a real GitHub status
check gating merges into `main` (see Phase 8 above for the thirteen real
bugs found and fixed along the way — this phase was almost entirely live
debugging against the actual running pipeline, not just writing config).
The deploy stage's most dangerous bug (#13, killing Jenkins mid-pipeline)
is fixed, merged to `main` via PR #15, and live-verified on a real `main`
build — the whole stack now redeploys cleanly without touching the CI
containers that run the pipeline.

Stripe is fully live-verified, including through the real Elements card
field in the browser. PayPal is OAuth-verified live; full Vault flow
verification remains blocked on PayPal's own "Advanced Credit and Debit
Card Payments" business approval (a review process on their end, not
something fixable in this codebase) — noted as a known limitation.

Phase 9 (Ansible) is done: `ansible/provision.yml` + `deploy.yml` are
live-verified idempotent (a real back-to-back WSL run showed `changed=0`
on the second pass), the deploy playbook now bakes in the 2-replica-per-
backend-service scaling that was previously only a manual `--scale` flag,
and it's wired into Jenkins itself (not left standalone) — the deploy
stage calls `ansible-playbook` instead of a raw `docker compose` line,
verified by running that exact command from inside the real Jenkins
container.

Phase 10 (Centralized logging) is done: Loki + Promtail + Grafana are live,
every request-handling backend service now carries a correlation ID through
MDC into its own logs, and the Gateway threads that same ID onto every
forwarded request. Live-verified by registering a real user through the
Gateway and finding the exact same correlation ID in both the Gateway's and
Auth Service's logs via a direct Loki API query — real cross-service
traceability, not just infra running with nothing proven to flow through it.

Phase 11 (Security hardening) is done: the frontend serves real HTTPS
(self-signed, documented Let's Encrypt path for a real domain later),
HashiCorp Vault is the live source of truth for 13 real secrets synced into
`.env` by a new Ansible role on every deploy, Postgres/Neo4j lost their host
port mappings entirely (confirmed genuinely unreachable from the host), all
6 backend Dockerfiles now run as a non-root user, and Dependabot enforces a
real weekly patching cadence across every ecosystem in the repo. Live-verified
end-to-end: a real login through `https://localhost` came back with a
`Secure; HttpOnly; SameSite=Strict` cookie, and Vault's own API confirmed all
13 managed secrets present. Full per-service Postgres role scoping is a
known, documented limitation deferred past this session.

Next: Phase 12 — Testing (unit tests per feature already exist and run in
CI; integration/E2E tests are the remaining bonus pass).
