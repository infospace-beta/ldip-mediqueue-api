# MediQueue API setup

## Prerequisites

- JDK 21
- PostgreSQL, either locally or through Docker Compose

## Configure PostgreSQL

The application defaults to `jdbc:postgresql://localhost:5432/mediqueue` with
the `postgres` username and password. To start the included database:

```powershell
docker compose up -d
```

For a local PostgreSQL installation, create a database named `mediqueue`.
If your database credentials or URL differ from the defaults, copy
`.env.example` to `.env` and set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.

The API also requires a private `JWT_SECRET` of at least 32 bytes. Set it in
`.env` or as an environment variable; do not commit real credentials or secrets.

## Run the application

```powershell
.\mvnw.cmd spring-boot:run
```

Flyway applies the database migrations at startup, and Hibernate validates the
resulting schema. The API listens on port `8082` by default:

- Swagger UI: <http://localhost:8082/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8082/v3/api-docs>
- Health check: <http://localhost:8082/actuator/health>

Authenticate in Swagger with a bearer JWT obtained from
`POST /api/v1/auth/login`. Patient registration is available at
`POST /api/v1/auth/register` and `POST /api/v1/patients`.

## Run tests

```powershell
.\mvnw.cmd test
```

Controller and unit tests run without Docker. The full application-context test
uses Testcontainers and is skipped when Docker is unavailable.

## Development seed data

The development seed is disabled by default and only runs with the `dev`
profile. It creates a development hospital, three departments, and sample
accounts for each application role. Every new sample account receives the
password supplied in `MEDIQUEUE_DEV_SEED_PASSWORD`; use a unique password for
local development only. Seed usernames are `dev-system-admin`,
`dev-hospital-admin`, `dev-doctor`, `dev-receptionist`, `dev-medical-staff`,
`dev-responder`, and `dev-patient`.

In PowerShell, set the password without putting it in the command history, then
explicitly enable the dev profile and seed:

```powershell
$securePassword = Read-Host "Development-only seed password" -AsSecureString
$env:MEDIQUEUE_DEV_SEED_PASSWORD = [System.Net.NetworkCredential]::new("", $securePassword).Password
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:APP_DEV_SEED_ENABLED = "true"
.\mvnw.cmd spring-boot:run
```

The seed operation is repeatable and skips matching existing sample accounts.
It fails rather than silently changing an account whose role or hospital
assignment conflicts with the expected development data. Clear the three
environment variables after stopping the application.

Roles are represented by the `users.role` enum value and a database check
constraint; there is no separate roles lookup table.

## PostgreSQL backup and restore

Install the PostgreSQL client tools (`pg_dump`, `createdb`, `pg_restore`, and
`psql`) and make them available on `PATH`. Backups are custom-format files
written outside the repository by default:

```powershell
.\scripts\backup-postgres.ps1 -Database mediqueue
```

The script prompts for the PostgreSQL password and creates a timestamped
backup under `%LOCALAPPDATA%\MediQueue\backups`.

Install a daily Task Scheduler job (default time 02:00 local time). The
installer reads the local `.env`, stores the PostgreSQL credential in the
standard `%APPDATA%\postgresql\pgpass.conf` (scoped to the local host, port, and
configured database user), restricts that file to the current Windows account,
and does not put the password in the scheduled command. The password-file
entry uses PostgreSQL's database wildcard so restore tooling can connect to
its maintenance database:

```powershell
.\scripts\install-backup-task.ps1
```

The scheduled task runs while that Windows account is logged on, is allowed
to run on battery power, and starts missed runs when possible. To use another
daily time:

```powershell
.\scripts\install-backup-task.ps1 -ScheduleTime "03:30"
```

The installer refuses to overwrite an existing task with the same name.
Backups are not automatically deleted; review storage usage and copy important
backups to separate protected storage.

To rehearse a restore safely, restore to a **new disposable database** whose
name ends in `_restore_test`. The script refuses other target names and
`createdb` refuses to overwrite an existing database:

```powershell
.\scripts\restore-postgres-test.ps1 `
  -BackupFile "$env:LOCALAPPDATA\MediQueue\backups\mediqueue-YYYYMMDDTHHMMSSZ.dump" `
  -TargetDatabase mediqueue_restore_test
```

The restore script verifies the Flyway migration history after restoring. It
does not delete the test database; inspect it first and remove it manually
only when you are certain it is the disposable restore target. Schedule the
backup script with Windows Task Scheduler for the required backup frequency,
and keep copies on storage separate from the machine holding the database.

## Database checks and performance

Flyway migrations are versioned in `src/main/resources/db/migration`; do not
edit a migration that has already run in a shared database. Add a new
versioned migration for future schema changes. Hibernate uses `ddl-auto=validate`
so application startup checks entity mappings against the migrated schema.
Existing databases receive new migrations on the next application startup.

The queue migrations include indexes for department/status/creation-time
lookups, patient history, and preventing duplicate active tickets. Emergency
cases have a general hospital/status/creation-time index and a partial,
creation-time-ordered index for open cases. The Docker-backed integration
tests verify migration history and those index definitions.

For query tuning, load representative data into a disposable database and
review the plans for the queue and emergency list queries with PostgreSQL's
`EXPLAIN (ANALYZE, BUFFERS)`. The benchmark below uses synthetic data; its
measurements are development checks, not production capacity guarantees.

The benchmark script creates a new database named with the required
`_perf_test` suffix, copies schema only (never source rows), and inserts
synthetic hospitals, users, 200,000 queue tickets, and 50,000 emergency cases:

```powershell
.\scripts\benchmark-postgres.ps1 `
  -SourceDatabase mediqueue `
  -TargetDatabase mediqueue_local_perf_test
```

It prints measured execution plans and leaves the synthetic database in place
for inspection. It never overwrites an existing target database. Remove only
the specifically named disposable `_perf_test` database after inspecting it.
The local PostgreSQL 18 run with 200,000 synthetic queue tickets and 50,000
emergency cases used the intended indexes for all three queries. Its observed
execution times were about 0.25 ms for a waiting-ticket lookup, 0.08 ms for
patient history, and 0.48 ms for open emergencies; these synthetic timings are
not production capacity guarantees.

## Frontend/API integration

This repository contains the backend API, not the frontend application, and
does not contain a separate API handshake document. Use the live OpenAPI
definition at `/v3/api-docs` as the current backend contract. Frontend
integration and comparison with the agreed handshake require the frontend
repository and the team's handshake document.

## Troubleshooting

| Problem | Fix |
|---|---|
| Database connection refused | Start PostgreSQL or run `docker compose up -d`. |
| Database does not exist | Create `mediqueue` and confirm `DB_URL` points to it. |
| Authentication startup error | Set `JWT_SECRET` to a private value of at least 32 bytes. |
| Port 8082 is already in use | Start with `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8083"`. |
