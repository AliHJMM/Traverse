FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.31-alpine
RUN apk add --no-cache gettext
COPY --from=build /app/dist/admin-dashboard/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY env.js.template /usr/share/nginx/html/env.js.template
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN sed -i 's/\r$//' /docker-entrypoint.sh && chmod +x /docker-entrypoint.sh
# Baked in at build time, not runtime-bind-mounted (Plan.md Phase 11): a
# relative bind-mount source (./certs) resolves against whatever process
# invokes `docker compose`, which is Jenkins' own /host-project view when
# triggered from CI -- a path the actual Docker daemon (running on the
# real host) has no knowledge of, since it only exists inside the Jenkins
# container's own mount namespace. Build context, unlike a runtime bind
# mount, gets tarred and sent to the daemon as a data stream, so it's
# context-agnostic and works identically from WSL or from Jenkins.
# ansible/roles/tls_certs copies the generated cert here (Frontend/certs/)
# before every build, always in sync with the real one in certs/.
COPY certs/traverse.crt certs/traverse.key /etc/nginx/certs/
EXPOSE 80 443
ENTRYPOINT ["/docker-entrypoint.sh"]
