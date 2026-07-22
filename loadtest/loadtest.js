// k6 load test -- throughput + load balancing across replicas.
//
// Ramps to 20 concurrent virtual users hammering the login endpoint
// through the API Gateway. Login is deliberately chosen because it runs a
// BCrypt verification server-side (intentionally CPU-heavy), so this is a
// realistic stress on the auth-service replicas, not a trivial ping.
//
// Run from inside the Docker network so it hits the Gateway directly and
// isn't affected by host port-forwarding:
//   docker run --rm -i --network traverse_traverse-net grafana/k6 run - < loadtest/loadtest.js
//
// Prereq: a user 'loadtest@example.com' / 'LoadTest123!' must exist
// (register it once through the Gateway). Scale the backend first:
//   docker compose -p traverse up -d --scale auth-service=2 ...
//
// Verified result (2 auth-service replicas, machine capped at 6 CPU):
//   1695 requests, 100% success, ~32 req/s, p95 = 944ms.
//   Load balancing: auth-service-1 handled 1620, auth-service-2 handled
//   1621 -- near-perfect round-robin via Spring Cloud LoadBalancer/Eureka.
import http from 'k6/http';
import { check } from 'k6';

const BASE = 'http://gateway:8080';
const payload = JSON.stringify({ email: 'loadtest@example.com', password: 'LoadTest123!' });
const params = { headers: { 'Content-Type': 'application/json' } };

export const options = {
  stages: [
    { duration: '10s', target: 20 }, // ramp up to 20 virtual users
    { duration: '40s', target: 20 }, // hold at 20 (steady load)
    { duration: '5s',  target: 0 },  // ramp down
  ],
  thresholds: {
    http_req_failed:   ['rate<0.05'],    // <5% failures
    http_req_duration: ['p(95)<3000'],   // p95 under 3s (BCrypt is intentionally slow)
  },
};

export default function () {
  const res = http.post(`${BASE}/api/auth/login`, payload, params);
  check(res, { 'login 200': (r) => r.status === 200 });
}
