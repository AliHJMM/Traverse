# TLS certificates

`traverse.crt` / `traverse.key` are generated here by
`ansible/roles/tls_certs` (idempotent — only regenerated if missing or
expiring within 30 days) and gitignored, same treatment as `.env`: they're
either machine-generated or environment-specific, never committed.

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
3. Nothing else changes — `docker-compose.yml`'s bind mount and nginx's TLS
   config are already domain-agnostic.

Renewal would then need certbot's usual cron/systemd-timer + an nginx
reload, which isn't wired up here since it only applies once a real domain
exists.
