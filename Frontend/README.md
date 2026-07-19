# Frontend — Admin Dashboard

Angular SPA (Phase 7 of `../Plan.md`), containerized with its own
`Dockerfile` (multi-stage build served by nginx), running in its own
container separate from every backend service.

nginx same-origin-proxies `/api/**` to the Gateway so the httpOnly,
`SameSite=Strict` auth cookie set by Auth Service is actually sent by the
browser — this only works because the dashboard and the API appear to be
on the same origin.

## Local development

```bash
npm install
npm start          # ng serve, proxies /api to http://localhost:8080 (see proxy.conf.json)
npm test           # Karma + Jasmine, headless Firefox
```
