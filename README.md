# Spring Boot Web API Template

A starter REST API built with **Spring Boot 4.1**, **Java 25** and **PostgreSQL**.
It ships with complete CRUD for four related resources (users, authors, books and bookmarks).
Use them as working examples when you add your own features.

## Contents

- [Tech stack](#tech-stack)
- [Getting started](#getting-started)
- [Project structure](#project-structure)
- [How a request flows](#how-a-request-flows)
- [Conventions](#conventions)
- [Adding a new feature](#adding-a-new-feature)
- [API reference](#api-reference)
- [Next steps](#next-steps)

## Tech stack

| Concern         | Choice                                            |
|-----------------|---------------------------------------------------|
| Language        | Java 25                                           |
| Framework       | Spring Boot 4.1 (Spring MVC)                      |
| Persistence     | Spring Data JPA / Hibernate, PostgreSQL           |
| Validation      | Jakarta Bean Validation (`@NotBlank`, `@Email`, …) |
| API docs        | springdoc-openapi (Swagger UI)                    |
| Monitoring      | Spring Boot Actuator                              |
| Build           | Maven (wrapper included: `./mvnw`)                |
| Tests           | JUnit 5, Testcontainers                           |

## Getting started

> **New to the project?** Follow [SETUP.md](SETUP.md) for step-by-step first-time setup and troubleshooting.

### 1. Prerequisites

- JDK 25
- A PostgreSQL database, from either of these:
  - **Docker:** run `docker compose up -d` to start one from [compose.yaml](compose.yaml).
  - **A local install:** create an empty database called `springapi`:
    ```sql
    CREATE DATABASE springapi;
    ```

### 2. Configure the connection

The defaults in [application.properties](src/main/resources/application.properties) are
`jdbc:postgresql://localhost:5432/springapi` with username `postgres` and password `postgres`.
They match `compose.yaml`. If your database is different, put your settings in a `.env` file
in the project root instead of editing `application.properties`. The app loads `.env` automatically.

```bash
cp .env.example .env          # PowerShell: Copy-Item .env.example .env
```

| Key           | Default                                       |
|---------------|-----------------------------------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5432/springapi`  |
| `DB_USERNAME` | `postgres`                                    |
| `DB_PASSWORD` | `postgres`                                    |

Write values as `KEY=value`, with no quotes. Any key you leave out uses the default.
`.env` is git-ignored, so credentials stay on your machine. Real environment variables
(for example in CI) override values in `.env`.

### 3. Run

```bash
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
```

Hibernate creates the tables on first start. Then open:

- Welcome page, with live system checks (API, database, docs) and build info: http://localhost:8080/
- Swagger UI, to try every endpoint in the browser: http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/actuator/health

### 4. Test

```bash
./mvnw test
```

`ApplicationTests` starts the whole app against a temporary PostgreSQL in Docker (Testcontainers).
On machines without Docker, it is **skipped** rather than failed.

## Project structure

The code is organized **by feature**. Everything about books lives in `book/`, everything about
users lives in `user/`, and so on. As the project grows you add new folders, and existing
folders don't get bigger and harder to navigate.

```
src/main/java/com/springapi
├── Application.java                  # Entry point
│
├── config/                           # App-wide Spring configuration
│   └── OpenApiConfig.java            #   Swagger UI title/description
│
├── common/                           # Shared code used by every feature
│   ├── dto/PageResponse.java         #   Standard JSON shape for paginated lists
│   ├── entity/BaseEntity.java        #   id, createdAt, updatedAt for every table
│   └── exception/
│       ├── ResourceNotFoundException #   → 404
│       ├── ConflictException         #   → 409
│       └── GlobalExceptionHandler    #   Converts exceptions to JSON error responses
│
├── user/                             # One folder per feature
│   ├── User.java                     #   Entity (database table)
│   ├── UserRepository.java           #   Database queries
│   ├── UserService.java              #   Business logic + transactions
│   ├── UserController.java           #   HTTP endpoints
│   └── dto/
│       ├── UserRequest.java          #   What clients send (validated)
│       └── UserResponse.java         #   What clients receive
│
├── author/                           # Same layout as user/
├── book/                             # Same layout as user/
└── bookmark/                         # Same layout as user/

src/main/resources/application.properties   # Configuration
src/test/java/com/springapi/                 # Tests (mirror the main package structure)
compose.yaml                                 # Optional local PostgreSQL via Docker
```

### Domain model

```
Author 1 ──── * Book 1 ──── * Bookmark * ──── 1 User
```

- A **book** has exactly one **author**. An author can have many books.
- A **bookmark** links a **user** to a **book**, with an optional note. A user can bookmark the same book only once.
- Deleting a user or a book also deletes their bookmarks (the database does this with `ON DELETE CASCADE`).
- An author who still has books **cannot** be deleted. The API returns `409 Conflict`.

## How a request flows

```
HTTP request
   │
   ▼
Controller   validates input (@Valid), maps URL → method, returns DTOs + status codes
   │
   ▼
Service      business rules, transactions (@Transactional), entity ↔ DTO mapping
   │
   ▼
Repository   database access (Spring Data JPA)
   │
   ▼
PostgreSQL
```

When something goes wrong, the service throws an exception (for example `ResourceNotFoundException`).
`GlobalExceptionHandler` turns it into a JSON error, so controllers contain no try/catch code.

## Conventions

These rules keep the codebase consistent as more people join.

### Layers

| Layer          | Does                                                    | Does **not**                                 |
|----------------|---------------------------------------------------------|----------------------------------------------|
| **Controller** | HTTP mapping, `@Valid`, status codes, calls *its own* service | Business logic, call repositories             |
| **Service**    | Business rules, `@Transactional`, builds response DTOs  | Know about HTTP (no `ResponseEntity`)        |
| **Repository** | Database queries                                        | Business logic                               |
| **Entity**     | Map a table                                             | Get returned from a controller               |
| **DTO**        | Define the API's JSON (Java `record`s)                  | Contain logic beyond simple mapping          |

### Dependencies between features

- A feature may use **another feature's service**, never its repository.
  For example, `BookService` calls `AuthorService.getAuthorEntity(id)`.
- Dependencies point in **one direction**: `bookmark → book → author` and `bookmark → user`.
  Never add a dependency that points back up, such as `AuthorService` → `BookService`.
  Spring refuses to start when two services depend on each other.
- Anything shared by several features goes in `common/`.

### Naming

| Thing          | Pattern                                    | Example                                   |
|----------------|--------------------------------------------|-------------------------------------------|
| Package        | singular, lowercase                        | `book`                                    |
| Entity         | singular noun                              | `Book`                                    |
| Table          | plural, snake_case                         | `books`                                   |
| Request DTO    | `XRequest` (or `CreateXRequest`/`UpdateXRequest` when they differ) | `BookRequest`   |
| Response DTO   | `XResponse`                                | `BookResponse`                            |
| URL            | `/api/v1/` + plural, kebab-case            | `/api/v1/books`, `/api/v1/reading-lists`  |

### REST

| Action        | Method & path                | Success status   |
|---------------|------------------------------|------------------|
| List (paged)  | `GET /api/v1/books`          | `200 OK`         |
| Get one       | `GET /api/v1/books/{id}`     | `200 OK`         |
| Create        | `POST /api/v1/books`         | `201 Created` + `Location` header |
| Update        | `PUT /api/v1/books/{id}`     | `200 OK`         |
| Delete        | `DELETE /api/v1/books/{id}`  | `204 No Content` |

### Other rules

- Use **constructor injection**, never `@Autowired` on fields.
- Mark services `@Transactional(readOnly = true)` at class level and add `@Transactional` to methods that write data.
- Don't change `ddl-auto` or drop tables on a shared database.

## Adding a new feature

Example: adding **reviews** (a user reviews a book).

1. **Create the package** `com.springapi.review` with a `dto/` subfolder.
2. **Entity** `Review extends BaseEntity`. Annotate it with `@Entity` and `@Table(name = "reviews")`, and add fields and relations (`@ManyToOne(fetch = LAZY)`).
3. **Repository** `ReviewRepository extends JpaRepository<Review, Long>`.
   Spring generates queries from method names, for example `findByBookId(Long bookId, Pageable pageable)`.
4. **DTOs**: a `ReviewRequest` record with validation annotations, and a `ReviewResponse` record with a `static from(Review)` method.
5. **Service** `ReviewService`. Copy the shape of `BookService`: `findAll`, `findById`, `create`, `update`, `delete`, `getReviewEntity`.
   Use `UserService` and `BookService` to load related entities.
6. **Controller** `ReviewController` at `/api/v1/reviews`. Copy the shape of `BookController` and add `@Tag(name = "Reviews")`.
7. **Run the app** and try the endpoints in Swagger UI.
8. **Write tests** under `src/test/java/com/springapi/review/`.

## API reference

The full, interactive reference is in Swagger UI (`/swagger-ui.html`).
The raw OpenAPI document is at `/v3/api-docs`.

| Resource  | Endpoints                                           | Filters                    |
|-----------|-----------------------------------------------------|----------------------------|
| Users     | `GET/POST /api/v1/users`, `GET/PUT/DELETE /api/v1/users/{id}`         | —            |
| Authors   | `GET/POST /api/v1/authors`, `GET/PUT/DELETE /api/v1/authors/{id}`     | —            |
| Books     | `GET/POST /api/v1/books`, `GET/PUT/DELETE /api/v1/books/{id}`         | `?authorId=` |
| Bookmarks | `GET/POST /api/v1/bookmarks`, `GET/PUT/DELETE /api/v1/bookmarks/{id}` | `?userId=`   |

### Trying the API in Swagger UI

1. Start the app (`./mvnw spring-boot:run`) and wait for `Started Application`.
2. Open http://localhost:8080/swagger-ui.html. The endpoints are grouped by resource.
3. Expand an endpoint, click **Try it out**, edit the example body or parameters, and click **Execute**.
   Swagger shows the response code, the response body and the equivalent `curl` command.

Create records in dependency order, because books need an author and bookmarks need a user and a book.
Use the `id` from each response in the next request:

| Step | Request                  | Body                                                                                                 |
|------|--------------------------|------------------------------------------------------------------------------------------------------|
| 1    | `POST /api/v1/authors`   | `{"name": "Ursula K. Le Guin", "bio": "Author of Earthsea."}`                                        |
| 2    | `POST /api/v1/books`     | `{"title": "A Wizard of Earthsea", "isbn": "978-0547773742", "publishedYear": 1968, "authorId": 1}` |
| 3    | `POST /api/v1/users`     | `{"username": "joe", "email": "joe@example.com", "fullName": "Joe Dev"}`                             |
| 4    | `POST /api/v1/bookmarks` | `{"userId": 1, "bookId": 1, "note": "Read next"}`                                                    |

List endpoints take `page`, `size` and `sort` (see [Pagination and sorting](#pagination-and-sorting)).
Invalid input returns a `400` that names each bad field (see [Errors](#errors)).
Records are saved to your database, so they're still there after a restart.

To use Postman or Insomnia instead, import the OpenAPI document from http://localhost:8080/v3/api-docs.

### Pagination and sorting

Every list endpoint accepts `page` (0-based), `size` (default 20, max 100) and `sort`:

```
GET /api/v1/books?page=0&size=10&sort=title,asc
```

```json
{
  "content": [ { "id": 1, "title": "A Wizard of Earthsea", "...": "..." } ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

### Errors

All errors use the standard [Problem Details](https://www.rfc-editor.org/rfc/rfc9457) format:

```json
{
  "status": 400,
  "title": "Validation failed",
  "detail": "One or more fields are invalid.",
  "instance": "/api/v1/users",
  "errors": {
    "email": "must be a well-formed email address",
    "username": "size must be between 3 and 50"
  }
}
```

| Status | When                                                        |
|--------|-------------------------------------------------------------|
| 400    | Invalid body, bad JSON, unknown sort field                  |
| 404    | Record (or URL) does not exist                              |
| 409    | Duplicate (username, email, ISBN, bookmark) or record still in use |
| 500    | Unexpected error. The details are logged, never sent to the client |

### Example: create a book

```bash
curl -X POST http://localhost:8080/api/v1/authors \
  -H "Content-Type: application/json" \
  -d '{"name": "Ursula K. Le Guin", "bio": "American author"}'

curl -X POST http://localhost:8080/api/v1/books \
  -H "Content-Type: application/json" \
  -d '{"title": "A Wizard of Earthsea", "isbn": "978-0547773742", "publishedYear": 1968, "authorId": 1}'
```

## Next steps

Likely additions as the project grows, roughly in order:

1. **Database migrations (Flyway).** Replace `ddl-auto=update` with versioned SQL scripts before any shared or production database.
2. **Tests per feature.** Add service unit tests (Mockito) and controller tests (`@WebMvcTest`, or `@SpringBootTest` with Testcontainers).
3. **Security.** Add Spring Security with JWT or OAuth2, and add a password (hashed) to `User`.
4. **Profiles.** Add `application-dev.properties` and `application-prod.properties` for environment-specific settings.
5. **CI pipeline.** Run `./mvnw verify` on every pull request.
