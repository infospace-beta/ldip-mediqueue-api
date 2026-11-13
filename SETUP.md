
# MediQueue API setup

## Prerequisites

- JDK 21
- PostgreSQL, either locally or through Docker Compose

## Configure PostgreSQL

The application defaults to `jdbc:postgresql://localhost:5432/mediqueue` with the `postgres` username and password. To start the included database:

```powershell
docker compose up -d
```

For a local PostgreSQL installation, create a database named `mediqueue`.

If your database credentials or URL differ from the defaults, copy `.env.example` to `.env` and set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.

The API also requires a private `JWT_SECRET` of at least 32 bytes. Set it in `.env` or as an environment variable; do not commit real credentials or secrets.

## Run the application

```powershell
.\mvnw.cmd spring-boot:run
```

Flyway applies database migrations at startup, and Hibernate validates the resulting schema. The API listens on port `8082` by default.

- Swagger UI: http://localhost:8082/swagger-ui.html
- OpenAPI JSON: http://localhost:8082/v3/api-docs
- Health check: http://localhost:8082/actuator/health

Authenticate in Swagger with a bearer JWT obtained from `POST /api/v1/auth/login`.

Patient registration is available at:

- `POST /api/v1/auth/register`
- `POST /api/v1/patients`

## Run tests

```powershell
.\mvnw.cmd test
```

Controller and unit tests run without Docker. The full application-context test uses Testcontainers and is skipped when Docker is unavailable.

## Development seed data

The development seed is disabled by default and only runs with the `dev` profile.

It creates a development hospital, three departments, and sample accounts for each application role.

Every new sample account receives the password supplied in `MEDIQUEUE_DEV_SEED_PASSWORD`; use a unique password for local development only.

Seed usernames:

- `dev-system-admin`
- `dev-hospital-admin`
- `dev-doctor`
- `dev-receptionist`
- `dev-medical-staff`
- `dev-responder`
- `dev-patient`

In PowerShell, set the password without putting it in the command history, then explicitly enable the dev profile and seed:

```powershell
$securePassword = Read-Host "Development-only seed password" -AsSecureString
$env:MEDIQUEUE_DEV_SEED_PASSWORD = [System.Net.NetworkCredential]::new("", $securePassword).Password
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:APP_DEV_SEED_ENABLED = "true"
.\mvnw.cmd spring-boot:run
```

The seed operation is repeatable and skips matching existing sample accounts.

It fails rather than silently changing an account whose role or hospital assignment conflicts with the expected development data.

Clear the three environment variables after stopping the application.

Roles are represented by the `users.role` enum value and a database check constraint; there is no separate roles lookup table.

## PostgreSQL backup and restore

Install the PostgreSQL client tools (`pg_dump`, `createdb`, `pg_restore`, and `psql`) and make them available on `PATH`.

Backups are custom-format files written outside the repository by default:

```powershell
.\scripts\backup-postgres.ps1 -Database mediqueue
```

The script prompts for the PostgreSQL password and creates a timestamped backup under `%LOCALAPPDATA%\MediQueue\backups`.

### Scheduled backups

Install a daily Task Scheduler job (default time 02:00 local time).

The installer reads the local `.env`, stores the PostgreSQL credential in the standard `%APPDATA%\postgresql\pgpass.conf` (scoped to the local host, port, and configured database user), restricts that file to the current Windows account, and does not put the password in the scheduled command.

The password-file entry uses PostgreSQL's database wildcard so restore tooling can connect to its maintenance database:

```powershell
.\scripts\install-backup-task.ps1
```

The scheduled task runs while that Windows account is logged on, is allowed to run on battery power, and starts missed runs when possible.

To use another daily time:

```powershell
.\scripts\install-backup-task.ps1 -ScheduleTime "03:30"
```

The installer refuses to overwrite an existing task with the same name.

Backups are not automatically deleted; review storage usage and copy important backups to separate protected storage.

### Restore testing

To rehearse a restore safely, restore to a new disposable database whose name ends in `_restore_test`.

The script refuses other target names and `createdb` refuses to overwrite an existing database:

```powershell
.\scripts\restore-postgres-test.ps1 `
  -BackupFile "$env:LOCALAPPDATA\MediQueue\backups\mediqueue-YYYYMMDDTHHMMSSZ.dump" `
  -TargetDatabase mediqueue_restore_test
```

The restore script verifies the Flyway migration history after restoring.

It does not delete the test database; inspect it first and remove it manually only when you are certain it is the disposable restore target.

Keep copies on storage separate from the machine holding the database.

## Database checks and performance

Flyway migrations are versioned in `src/main/resources/db/migration`; do not edit a migration that has already run in a shared database.

Add a new versioned migration for future schema changes.

Hibernate uses `ddl-auto=validate` so application startup checks entity mappings against the migrated schema.

Existing databases receive new migrations on the next application startup.

The queue migrations include indexes for department/status/creation-time lookups, patient history, and preventing duplicate active tickets.

Emergency cases have a general hospital/status/creation-time index and a partial, creation-time-ordered index for open cases.

The Docker-backed integration tests verify migration history and those index definitions.

### Query performance benchmark

For query tuning, load representative data into a disposable database and review the plans for the queue and emergency list queries with PostgreSQL's `EXPLAIN (ANALYZE, BUFFERS)`.

The benchmark uses synthetic data; its measurements are development checks, not production capacity guarantees.

The benchmark script creates a new database named with the required `_perf_test` suffix, copies schema only (never source rows), and inserts synthetic hospitals, users, 200,000 queue tickets, and 50,000 emergency cases:

```powershell
.\scripts\benchmark-postgres.ps1 `
  -SourceDatabase mediqueue `
  -TargetDatabase mediqueue_local_perf_test
```

It prints measured execution plans and leaves the synthetic database in place for inspection.

It never overwrites an existing target database.

The local PostgreSQL 18 run with 200,000 synthetic queue tickets and 50,000 emergency cases used the intended indexes for all three queries.

Observed execution times were about:

- 0.25 ms for a waiting-ticket lookup
- 0.08 ms for patient history
- 0.48 ms for open emergencies

These synthetic timings are not production capacity guarantees.

## Frontend/API integration

This repository contains the backend API, not the frontend application.

The MediQueue Vue frontend is maintained in the separate `ldip-mediqueue-app` repository.

The API contract is the live OpenAPI definition at `/v3/api-docs`.

The frontend uses `VITE_API_URL` (default `http://localhost:8082`) and the backend must allow the frontend's origin through `APP_CORS_ALLOWED_ORIGINS` (default `http://localhost:5173`).

For a different Vite host/port, set the backend property to the matching origin or a comma-separated list of trusted origins.

### Running the frontend

Run the frontend from its application directory:

```powershell
Copy-Item .env.example .env
npm run dev
```

### Integrated functionality

The frontend connects to backend functionality for:

- Patient registration, login, and current-user lookup
- Active hospital and department directories
- On-duty doctor directories
- Queue operations
- Emergency requests and status
- Appointment booking
- Patient clinical history
- In-app notifications
- Staff, responder, doctor, and hospital dashboards

### Appointments, clinical records, and notifications

Flyway V11 adds appointment, clinical-record, and notification tables.

Patients can book a future UTC start time with an on-duty doctor and active department at the same hospital.

Duplicate bookings for the same doctor and exact start time are rejected.

Appointments are fixed time slots; this version does not model appointment duration or detect overlapping slots.

Doctors and medical staff can add one clinical record to each completed queue ticket. Patients can read their own records.

Queue responses include a one-based position while waiting and an estimated wait based on the department's historical average wait.

Notifications are in-app only. They are created for queue, appointment, emergency, and clinical events and can be marked read by their recipient.

No SMS or email delivery is configured.

Frontend integration and comparison with the agreed handshake should use the frontend repository and the team's handshake document.

## Troubleshooting

| Problem | Fix |
|---|---|
| Database connection refused | Start PostgreSQL or run `docker compose up -d`. |
| Database does not exist | Create `mediqueue` and confirm `DB_URL` points to it. |
| Authentication startup error | Set `JWT_SECRET` to a private value of at least 32 bytes. |
| Port 8082 is already in use | Start with `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8083"`. |
