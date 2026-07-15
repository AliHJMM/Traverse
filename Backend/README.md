# Backend

Houses the microservices, each in its own subfolder with its own
`Dockerfile`, running in its own container:

- `gateway/` — API Gateway (Phase 2)
- `auth-service/` — Auth (JWT/OAuth2, roles) (Phase 3)
- `user-service/` — User CRUD (Phase 4)
- `travel-service/` — Travel/itinerary CRUD, Postgres + Neo4j (Phase 5)
- `payment-service/` — Payment methods, Stripe/PayPal (Phase 6)

See `../Plan.md` for the full build order.
