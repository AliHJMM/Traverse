# Ansible (Plan.md Phase 9)

Idempotent provisioning/deployment automation for the Traverse stack. This
is a **single-host project** — Postgres, Neo4j, every microservice, Jenkins
and SonarQube all run as containers on one Docker Desktop engine — so there
is exactly one inventory target (`localhost`, `ansible_connection=local`),
reached from wherever `ansible-playbook` itself runs:

- **manually**, from a WSL Linux shell on the Windows dev machine, against
  `/mnt/c/Users/AliHa/Traverse`
- **from Jenkins' deploy stage**, running inside the `traverse-jenkins`
  container against the `/host-project` bind mount (same engine, reached via
  the mounted `docker.sock` — see `docker-compose.yml`'s `jenkins` service)

Kept as a real inventory group rather than `hosts: localhost` hardcoded in
each playbook, so pointing this at an actual remote Docker host later is an
inventory change, not a playbook rewrite.

## Layout

```
ansible/
├── ansible.cfg                  # inventory + roles_path
├── inventory/hosts.ini          # single `traverse_host` group, local connection
├── playbooks/
│   ├── group_vars/all.yml       # service lists, replica counts, project dir resolution
│   ├── provision.yml            # host prerequisites (manual, WSL only)
│   ├── deploy.yml               # docker compose deploy (manual or Jenkins)
│   └── site.yml                 # provision.yml + deploy.yml, for a full local run
└── roles/
    ├── host_provision/tasks/main.yml
    └── compose_deploy/tasks/main.yml
```

`group_vars/all.yml` lives under `playbooks/`, not at the `ansible/` top
level — Ansible only auto-loads `group_vars/`/`host_vars/` from directories
alongside the playbook file or the inventory file, not an arbitrary parent
directory. Putting it at the top level silently produced "variable is
undefined" errors; this was caught during Phase 9's live verification (see
Plan.md).

## Usage

```bash
# from a WSL shell, in this directory
cd ansible

# host-level prerequisites -- run this once per Docker Desktop restart
# (see provision.yml for why); never run from inside Jenkins
ansible-playbook playbooks/provision.yml

# deploy/redeploy the application stack (idempotent, safe to re-run)
ansible-playbook playbooks/deploy.yml

# both, in order
ansible-playbook playbooks/site.yml
```

### A real WSL/DrvFs quirk hit during verification

Running `ansible-playbook` directly from `/mnt/c/...` prints:

```
[WARNING]: Ansible is being run in a world writable directory ..., ignoring
it as an ansible.cfg source.
```

Files under a DrvFs mount (`/mnt/c/...`) report as world-writable to WSL's
Linux permission model regardless of their real NTFS ACLs, and Ansible
refuses to auto-load an `ansible.cfg` from a world-writable directory as a
safety measure. Auto-discovery is skipped, `roles_path`/`inventory` aren't
applied, and the run fails with "role not found" even though the file is
right there. Worked around by setting `ANSIBLE_CONFIG` explicitly, which
bypasses the cwd-based auto-discovery check entirely:

```bash
ANSIBLE_CONFIG=$(pwd)/ansible.cfg ansible-playbook playbooks/deploy.yml
```

This is why the Jenkinsfile deploy stage sets `ANSIBLE_CONFIG` explicitly
too, even though `/host-project` is a different (read-only Docker Desktop
file-sharing) mount than the WSL DrvFs one — better to not depend on
auto-discovery working in either context.

## What each playbook does

**`provision.yml`** (`roles/host_provision`) — host-level, run manually,
never from Jenkins:
- Asserts the Docker CLI and Compose plugin are present.
- Asserts `.env` exists (never creates or overwrites it — it holds live
  secrets); warns (non-fatal) about any values still left as
  `change_me`/`not_configured` placeholders.
- Re-checks and, if needed, re-raises `vm.max_map_count` to 262144 on the
  `docker-desktop` WSL VM via Windows-interop `wsl.exe` — SonarQube's
  bundled Elasticsearch needs this (Plan.md Phase 8, bug #9), it doesn't
  persist across a Docker Desktop restart, and it used to be a manual
  "remember to run this again" step. Now it's just re-run as part of
  provisioning.

**`deploy.yml`** (`roles/compose_deploy`) — runs `docker compose up -d
--build --wait`, scoped to the 9 application services only
(`core_services` + `scalable_services` in `group_vars/all.yml`) —
deliberately excluding `jenkins`/`sonarqube`/`sonarqube-db`, which live in
the same `docker-compose.yml` as CI infrastructure (see Plan.md Phase 8,
bug #13, for what happens when a deploy recreates the Jenkins container
that's running the deploy). The four stateless backend services
(auth/user/travel/payment) are scaled to their configured replica count
(`service_replicas`, default 2 each) on every deploy via `--scale`, not
just as an ad-hoc manual flag — this is what actually gives the "multiple
replicas for load balancing/failover" requirement a standing, automated
home instead of a one-off manual `--scale` command. Finishes by polling the
Gateway's `/actuator/health` and the frontend until both respond `200`.

## Verified live (not just written)

- `provision.yml`: ran clean against the real host — Docker/Compose
  presence confirmed, `.env` checked, `vm.max_map_count` already at 262144
  from a prior manual fix and correctly left untouched (no unnecessary
  `changed`).
- `deploy.yml`, first run: real `docker compose up -d --build --scale ...`
  executed — `auth-service`/`user-service`/`travel-service`/
  `payment-service` each came up as 2 replicas, confirmed via `docker
  compose ps`. `jenkins`/`sonarqube`/`sonarqube-db` stayed up the entire
  time (`Up 24 minutes`, unchanged) while the 4 backend services + gateway
  + frontend recreated (`Up 8 seconds`). Gateway health check retried ~6
  times (~30s) while the new containers finished booting/registering with
  Eureka, then passed; frontend passed immediately.
- `deploy.yml`, second run (idempotency check): `changed=0` across the
  board — `docker compose up -d --build` detected no actual image/config
  changes and recreated nothing, both health checks passed on the first
  try with no retries needed. Confirms "safe to re-run without side
  effects," not just "should be."
