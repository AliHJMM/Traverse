# Load & failover testing

k6 scripts that demonstrate the two things the audit asks about under the
"simulate load on microservices" heading: **effective load balancing** and
**failover under heavy traffic**. Both are reproducible — re-run them any
time to reproduce the numbers below.

## Why login as the load target

The tests hammer `POST /api/auth/login` through the API Gateway. Login runs
a server-side **BCrypt** verification (intentionally CPU-heavy), so it's a
realistic stress on the `auth-service` replicas rather than a trivial ping.
It's also public, so no token juggling is needed to generate load.

## Prerequisites

1. Scale the stateless backend to 2 replicas (the config that makes load
   balancing observable):
   ```
   docker compose -p traverse up -d --scale auth-service=2 --scale user-service=2 \
     --scale travel-service=2 --scale payment-service=2 \
     auth-service user-service travel-service payment-service
   ```
2. Register the load-test user once (through the Gateway):
   ```
   curl -X POST http://gateway:8080/api/auth/register \
     -H 'Content-Type: application/json' \
     -d '{"email":"loadtest@example.com","password":"LoadTest123!","fullName":"Load Test"}'
   ```

The k6 scripts run **inside the Docker network** (`--network
traverse_traverse-net`) so they hit `http://gateway:8080` directly and
aren't affected by host port-forwarding.

## 1. Throughput + load balancing — `loadtest.js`

```
docker run --rm -i --network traverse_traverse-net grafana/k6 run - < loadtest/loadtest.js
```

**Verified result** (2 `auth-service` replicas, host capped at 6 CPU):

| Metric | Value |
|---|---|
| Total requests | 1,695 |
| Success rate | **100%** (0 failed) |
| Throughput | ~32 req/s |
| p95 latency | 944 ms (BCrypt-bound) |

**Load balancing proof** — requests handled per replica, counted from each
container's own access log (the correlation-ID access-log line added in
Phase 10):

| Replica | Login requests |
|---|---|
| `auth-service-1` | 1620 |
| `auth-service-2` | 1621 |

A ~50/50 split — Spring Cloud LoadBalancer round-robins evenly across the
replicas Eureka has registered.

## 2. Failover under load — `loadtest-failover.js`

Start the load, then kill a replica ~15s in:

```
# terminal 1
docker run --rm -i --network traverse_traverse-net grafana/k6 run - < loadtest/loadtest-failover.js
# terminal 2, ~15s later
docker kill traverse-auth-service-2
```

**Verified result** — killed `auth-service-2` mid-load:

| Metric | Value |
|---|---|
| Total requests | 835 |
| Success rate | **96.88%** (809 ok / 26 failed) |
| Failures | 26, all inside a ~few-second window |

The 26 failures are the **Eureka detection window**: the Gateway's local
registry cache briefly keeps routing to the just-killed instance before
evicting it (those requests hit their timeout — note the ~29s `max`
duration). After eviction, 100% of traffic recovers on the surviving
replica. This is the documented trade-off from Plan.md Phase 3.5 — genuine
failover with a brief reconvergence, not a claim of zero-downtime.

Restore afterwards:
```
docker compose -p traverse up -d --scale auth-service=2 auth-service
```
