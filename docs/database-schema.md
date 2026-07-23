# Database Schema

Traverse uses **two databases**, each for what it's genuinely good at:

- **PostgreSQL** — the transactional entities (credentials, profiles,
  itineraries, payment methods). Every microservice owns its **own schema**
  inside one shared Postgres instance; no service reads another service's
  tables (data is only ever crossed via APIs).
- **Neo4j** — the graph-shaped part of travel data: destinations connected
  by routes, used for "find destinations near X" recommendations that would
  be awkward as recursive SQL joins.

All Postgres tables are created and versioned by **Flyway** migrations
(`ddl-auto: validate` — Hibernate never mutates the schema), so the schema
below is exactly what runs.

---

## PostgreSQL

One instance, one database (`traverse`), four schemas — one per service:

| Schema | Owned by | Purpose |
|---|---|---|
| `auth` | auth-service | Login credentials |
| `users` | user-service | User profiles (dashboard-managed users) |
| `travel` | travel-service | Itineraries and their nested parts |
| `payment` | payment-service | Saved payment methods |

### `auth.users` — credentials (auth-service)

The only place a password hash lives. The first account ever created is
bootstrapped as `ADMIN`; public registration can't self-assign `ADMIN`
afterwards.

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `email` | `VARCHAR(255)` | `NOT NULL`, `UNIQUE` |
| `password_hash` | `VARCHAR(255)` | BCrypt hash — never a raw password |
| `role` | `VARCHAR(20)` | `USER` / `ADMIN`, default `USER` |
| `enabled` | `BOOLEAN` | default `TRUE` |
| `created_at` | `TIMESTAMP` | default `now()` |

### `users.user_profiles` — profiles (user-service)

Profile data for users the admin manages through the dashboard. **Doesn't
own credentials** — the `id` is shared with `auth.users` (the user-service
creates the credential via a Feign call to auth-service, then stores the
profile under the same id). `email`/`role` are a denormalized copy kept in
sync so profile reads need no cross-service call.

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGINT` | PK — **same id as `auth.users`** |
| `email` | `VARCHAR(255)` | `NOT NULL`, `UNIQUE` (denormalized) |
| `role` | `VARCHAR(20)` | denormalized copy, default `USER` |
| `full_name` | `VARCHAR(255)` | `NOT NULL` |
| `phone` | `VARCHAR(50)` | |
| `address` | `VARCHAR(500)` | |
| `enabled` | `BOOLEAN` | default `TRUE` |
| `created_at` | `TIMESTAMP` | default `now()` |

### `travel.*` — itineraries (travel-service)

A `travels` aggregate root owns four child tables. Every child has a
`travel_id` FK with **`ON DELETE CASCADE`** — deleting a travel removes all
its destinations, activities, accommodations, and transportations in one
shot (the database enforces the cascade; the JPA layer mirrors it with
`cascade=ALL, orphanRemoval=true`).

```
travels (1) ──┬── (N) destinations      ON DELETE CASCADE
              ├── (N) activities         ON DELETE CASCADE
              ├── (N) accommodations     ON DELETE CASCADE
              └── (N) transportations    ON DELETE CASCADE
```

**`travel.travels`**

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `title` | `VARCHAR(255)` | `NOT NULL` |
| `start_date` | `DATE` | `NOT NULL` |
| `end_date` | `DATE` | `NOT NULL` (validated `>= start_date`) |
| `created_at` | `TIMESTAMP` | default `now()` |

> `durationDays` is **computed** from `start_date`/`end_date`, not stored.

**`travel.destinations`** — one or many per travel

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `travel_id` | `BIGINT` | FK → `travels(id)` `ON DELETE CASCADE` |
| `city` | `VARCHAR(255)` | `NOT NULL` |
| `country` | `VARCHAR(255)` | `NOT NULL` |
| `arrival_date` / `departure_date` | `DATE` | |

**`travel.activities`**

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `travel_id` | `BIGINT` | FK → `travels(id)` `ON DELETE CASCADE` |
| `name` | `VARCHAR(255)` | `NOT NULL` |
| `description` | `VARCHAR(1000)` | |
| `destination_city` | `VARCHAR(255)` | which stop it belongs to |
| `date` | `DATE` | |
| `cost` | `NUMERIC(10,2)` | |

**`travel.accommodations`**

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `travel_id` | `BIGINT` | FK → `travels(id)` `ON DELETE CASCADE` |
| `name` | `VARCHAR(255)` | `NOT NULL` |
| `type` | `VARCHAR(100)` | `NOT NULL` (Hotel, Ryokan, …) |
| `address` | `VARCHAR(500)` | |
| `check_in` / `check_out` | `DATE` | |

**`travel.transportations`**

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `travel_id` | `BIGINT` | FK → `travels(id)` `ON DELETE CASCADE` |
| `type` | `VARCHAR(100)` | `NOT NULL` (Flight, Train, …) |
| `provider` | `VARCHAR(255)` | |
| `from_location` / `to_location` | `VARCHAR(255)` | `NOT NULL` |
| `departure_time` / `arrival_time` | `TIMESTAMP` | |

### `payment.*` — payment methods (payment-service)

**Never stores raw card numbers** — only the opaque token/id the provider's
own SDK produced, plus safe display metadata (brand, last4, expiry) or a
PayPal payer email.

**`payment.payment_methods`**

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `user_id` | `BIGINT` | `NOT NULL`, indexed (`idx_payment_methods_user_id`) |
| `provider` | `VARCHAR(20)` | `STRIPE` / `PAYPAL` |
| `external_id` | `VARCHAR(255)` | provider's token/PM id — **not a card number** |
| `brand` | `VARCHAR(50)` | e.g. `visa` |
| `last4` | `VARCHAR(4)` | display only |
| `expiry_month` / `expiry_year` | `INTEGER` | |
| `payer_email` | `VARCHAR(255)` | PayPal only |
| `is_default` | `BOOLEAN` | default `FALSE` — only one default per user |
| `created_at` | `TIMESTAMP` | default `now()` |

**`payment.stripe_customers`** — maps each user to one reusable Stripe
Customer (created on their first saved card, reused after), so Stripe's
`attach()`/`detach()` have a customer to work against.

| Column | Type | Notes |
|---|---|---|
| `user_id` | `BIGINT` | PK |
| `customer_id` | `VARCHAR(255)` | Stripe Customer id (`cus_…`) |

---

## Neo4j — the destination graph (travel-service)

Alongside its Postgres tables, travel-service maintains a graph in Neo4j.
It is **not seed data** — it's a byproduct of real itineraries: whenever a
travel visits city A immediately before city B, that route is recorded.

### Node: `Destination`

| Property | Notes |
|---|---|
| `city` | the destination city (natural key used in MERGE) |
| `country` | set on first creation |

### Relationship: `CONNECTED_TO`

Directed `(:Destination)-[:CONNECTED_TO]->(:Destination)`, one per
consecutive-city pair in an itinerary.

| Property | Notes |
|---|---|
| `tripCount` | how many itineraries used this route; starts at 1, increments on repeat |

### How it's written

Each time a travel is created, consecutive destinations are `MERGE`d and
their `CONNECTED_TO` `tripCount` bumped (idempotent — repeat routes just
increment the counter):

```cypher
MERGE (a:Destination {city: $cityA}) ON CREATE SET a.country = $countryA
MERGE (b:Destination {city: $cityB}) ON CREATE SET b.country = $countryB
MERGE (a)-[r:CONNECTED_TO]->(b)
  ON CREATE SET r.tripCount = 1
  ON MATCH  SET r.tripCount = r.tripCount + 1
```

### How it's queried — `GET /api/travels/destinations/{city}/nearby`

A 1–2 hop traversal returns destinations reachable from a given city — the
kind of "what's near here" recommendation that's natural in a graph and
awkward as recursive SQL:

```cypher
MATCH (a:Destination {city: $city})-[:CONNECTED_TO*1..2]-(b:Destination)
WHERE a <> b
RETURN DISTINCT b
```

### Why split Postgres + Neo4j (in travel-service)

The two stores are wired up with **separate transaction managers** in the
same app: JPA's is `@Primary` (every unqualified `@Transactional` uses it),
and a distinctly-named `neo4jTransactionManager` is opted into explicitly
by the graph service. This is required because Spring Boot's Neo4j
auto-config otherwise backs off from creating a transaction manager once
JPA claims the default bean name — see Plan.md Phase 5 for the full story.
