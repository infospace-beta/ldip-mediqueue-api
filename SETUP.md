# First-Time Setup

Follow these steps after you clone the repository. They take about 10 minutes.
For how the project is organized and how to add features, see [README.md](README.md).

## 1. Install the prerequisites

- **JDK 25.** Check with `java -version`.
- **PostgreSQL**, either:
  - **Docker**, which runs PostgreSQL for you (step 2, option A), or
  - **a local PostgreSQL install** (step 2, option B).

## 2. Set up the database

The app needs a PostgreSQL database called `springapi`. Hibernate creates the tables
automatically the first time the app starts. You only need to create the empty database.

### Option A: Docker

From the project folder, run:

```bash
docker compose up -d
```

This starts PostgreSQL with the database `springapi`, username `postgres` and password `postgres`.
These match the app's defaults, so **skip step 3** and go straight to step 4.

To stop it, run `docker compose down`. Add `-v` to also delete the data.

### Option B: Local PostgreSQL install

Open pgAdmin or `psql` and create the database:

```sql
CREATE DATABASE springapi;
```

## 3. Tell the app your database password

> **Skip this step if you used Docker.**

By default, the app connects as user `postgres` with password `postgres`.
If your local PostgreSQL uses a different username or password (it usually does),
put your settings in a `.env` file in the project folder. The app loads it automatically.

```bash
cp .env.example .env          # PowerShell: Copy-Item .env.example .env
```

Then edit `.env`:

| Key           | Default                                       | Set it when                        |
|---------------|-----------------------------------------------|------------------------------------|
| `DB_PASSWORD` | `postgres`                                    | Your password is different         |
| `DB_USERNAME` | `postgres`                                    | You use a different user           |
| `DB_URL`      | `jdbc:postgresql://localhost:5432/springapi`  | Different host, port or database   |

Write values as `KEY=value`, with no quotes and no spaces around `=`.

`.env` is git-ignored, so your password stays on your machine.
**Do not put your password in `application.properties`.** That file is committed to Git.

Real environment variables override `.env`. This is handy in CI or for a one-off change:

```powershell
$env:DB_PASSWORD = "your-password"     # PowerShell, current terminal only
```

## 4. Run the app

```bash
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
```

You should see `Started Application` in the console. Then open:

- Welcome page, which shows whether the API, the database and the docs are working: http://localhost:8080/
- Swagger UI, to try every endpoint: http://localhost:8080/swagger-ui.html
- Health check, which should show `"status":"UP"`: http://localhost:8080/actuator/health

On the first start, a few `WARN ... constraint "uk..." does not exist, skipping` lines are normal.
That's Hibernate creating the tables.

## 5. Run the tests

```bash
./mvnw test
```

> **Important: without Docker, the tests are skipped, not run.**
>
> `ApplicationTests` starts its own temporary PostgreSQL in Docker (Testcontainers).
> On a machine without Docker it is **skipped**, and Maven still prints `BUILD SUCCESS`.
> So a green build without Docker does **not** mean the app was tested.
> Look for `Skipped: 1` in the output. If you see it, the test did not run.
>
> To actually run the tests, install Docker Desktop and make sure it's running.

## Troubleshooting

| Problem                                                     | Fix                                                                 |
|-------------------------------------------------------------|---------------------------------------------------------------------|
| `password authentication failed for user "postgres"`        | Set `DB_USERNAME` / `DB_PASSWORD` in `.env` (step 3).               |
| `database "springapi" does not exist`                       | Create the database (step 2).                                       |
| `Connection to localhost:5432 refused`                      | PostgreSQL isn't running. Start the service, or run `docker compose up -d`. |
| `Port 8080 was already in use`                              | Stop the other app, or run with `--server.port=8081`: `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` |
| `release version 25 not supported` / `invalid target release` | Your JDK is older than 25. Install JDK 25 and point `JAVA_HOME` at it. |
