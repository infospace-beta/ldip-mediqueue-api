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

## Troubleshooting

| Problem | Fix |
|---|---|
| Database connection refused | Start PostgreSQL or run `docker compose up -d`. |
| Database does not exist | Create `mediqueue` and confirm `DB_URL` points to it. |
| Authentication startup error | Set `JWT_SECRET` to a private value of at least 32 bytes. |
| Port 8082 is already in use | Start with `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8083"`. |
