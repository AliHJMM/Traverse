// k6 failover-under-load test.
//
// Runs steady login load at 15 VUs for ~50s. While it's running, kill one
// auth-service replica (from another shell):
//   # ~15s into the test:
//   docker kill traverse-auth-service-2
//
// This proves the system keeps serving when a replica dies mid-traffic.
// No hard thresholds here on purpose -- we WANT the run to complete
// through the failure so the dip + recovery are measurable.
//
// Run:
//   docker run --rm -i --network traverse_traverse-net grafana/k6 run - < loadtest/loadtest-failover.js
//
// Verified result: killed auth-service-2 mid-load -->
//   835 requests, 96.88% success (809 ok / 26 failed).
//   The 26 failures land in a ~few-second window: the Gateway's Eureka
//   registry cache briefly keeps routing to the dead instance before
//   evicting it (those requests hit their timeout -- see the ~29s max
//   duration). After eviction, 100% of traffic recovers on the surviving
//   replica. This is the documented Eureka detection-window trade-off
//   (see Plan.md Phase 3.5), i.e. real failover with a brief reconvergence,
//   not a claim of zero-downtime.
//
// Restore afterwards:
//   docker compose -p traverse up -d --scale auth-service=2 auth-service
import http from 'k6/http';
import { check } from 'k6';

const BASE = 'http://gateway:8080';
const payload = JSON.stringify({ email: 'loadtest@example.com', password: 'LoadTest123!' });
const params = { headers: { 'Content-Type': 'application/json' } };

export const options = {
  stages: [
    { duration: '5s',  target: 15 },
    { duration: '40s', target: 15 }, // kill a replica partway through this hold
    { duration: '5s',  target: 0 },
  ],
};

export default function () {
  const res = http.post(`${BASE}/api/auth/login`, payload, params);
  check(res, { 'login 200': (r) => r.status === 200 });
}
