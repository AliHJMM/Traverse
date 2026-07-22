# TLS certificates

`traverse.crt` / `traverse.key` are generated here by
`ansible/roles/tls_certs` (idempotent — only regenerated if missing or
expiring within 30 days) and gitignored, same treatment as `.env`: they're
either machine-generated or environment-specific, never committed.

The same role also copies both files into `Frontend/certs/` (also
gitignored) before every build — `Frontend/Dockerfile` `COPY`s them into
the image at build time. That's deliberate, not incidental: a runtime
bind-mount (`./certs:/etc/nginx/certs`) resolves relative to whatever
process invokes `docker compose`, which is Jenkins' own `/host-project`
view when the deploy is triggered from CI — a path the real Docker daemon
(running on the actual host) has no knowledge of, since it only exists
inside the Jenkins container's own mount namespace. A build context,
unlike a runtime bind mount, gets tarred and sent to the daemon as a data
stream, so it works identically regardless of which context triggers the
build. Confirmed live: `main` build #20 hit exactly this — nginx failed to
start with "no such file" for a cert that was genuinely sitting right
there in `certs/`, just not at the path the daemon actually saw.

## Why self-signed, not Let's Encrypt

Let's Encrypt can only issue a certificate for a real, publicly-resolvable
domain name (via an HTTP-01 or DNS-01 challenge) — it has no mechanism to
validate `localhost` or a private/NAT'd IP, which is all this project has
on a local Docker Desktop machine. So the honest options were: fake it (bad),
skip TLS entirely (worse — leaves "Enforce SSL/TLS encryption" unverified
like everything else in this project is verified live), or generate a real,
working self-signed certificate and structure things so a genuine Let's
Encrypt certificate drops in later with zero redesign. Went with the third.

## Swapping in a real Let's Encrypt certificate later

If this ever runs on a host with a real domain pointed at it:

1. Run certbot (standalone or webroot mode) to obtain `fullchain.pem` /
   `privkey.pem` for that domain.
2. Copy them into this directory as `traverse.crt` / `traverse.key` (or
   change the two `ssl_certificate*` paths in `Frontend/nginx.conf`).
3. Rebuild the frontend (`docker compose up -d --build frontend`, or just
   the next normal deploy) — nginx's TLS config is already domain-agnostic,
   only the underlying cert files change.

Renewal would then need certbot's usual cron/systemd-timer + a rebuild,
which isn't wired up here since it only applies once a real domain exists.
