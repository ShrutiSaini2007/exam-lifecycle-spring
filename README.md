# Secure Examination Content Lifecycle — Spring Boot Edition (BIT-55)

This is the mandated-stack implementation: **Java 21, Spring Boot 3, Spring
Security (JWT), PostgreSQL, Docker, GitHub Actions, and an auto-generated
OpenAPI/Swagger spec.** It mirrors the API contract and business rules of the
earlier Python prototype exactly, so the two can be compared as your
capstone's required "baseline vs. advanced implementation" evidence.

## ⚠️ Important: this has not been compiled or run



## What's implemented

Same feature set as the Python version, now on the required stack:

| Feature | Where |
|---|---|
| Admin-only account provisioning, JWT auth | `AuthController`, `UserService`, `JwtService`, `SecurityConfig` |
| Bootstrap admin on first run (no public sign-up) | `BootstrapAdminRunner` |
| Course blueprint | `CourseController`, `BlueprintController` |
| Question bank import | `BlueprintController` (`/questions` sub-route) |
| Draft creation + full version history, SHA-256 content hash | `DraftService.createDraft` / `.newVersion` |
| Live paper review pre-approval (any stage) | `GET /api/drafts/{id}/content` |
| Moderator comments | `DraftController` (`/comments`) |
| Sequential approval gate (moderator → officer) | `DraftService.approve` |
| Final hash + lock (immutable after) | `DraftService.lock` |
| Watermarked export with full attribution (prepared/moderated/approved/issued) | `DraftService.export` |
| Access audit log | `AuditService`, `AuditController`, `DraftController#auditForDraft` |
| OpenAPI/Swagger docs, zero hand-written YAML | springdoc dependency + `OpenApiConfig` |
| Docker + docker-compose (app + Postgres) | `Dockerfile`, `docker-compose.yml` |
| CI (build, test, docker build) | `.github/workflows/ci.yml` |
| Integration test suite mirroring `smoke_test.py` | `DraftWorkflowIntegrationTest` (runs against H2, no live Postgres needed) |

## Running it

**Option A — Docker (closest to how you'd actually deploy it):**

```bash
docker compose up --build
```

Watch the logs for the bootstrap admin password (printed once, on first run
against an empty database):

```
========================================================
First run: created a bootstrap ADMIN account.
  username: admin
  password: <random>
========================================================
```

The API is then at `http://localhost:8080`, and Swagger UI at
`http://localhost:8080/swagger-ui.html`.

To use a password you choose instead of a random one:

```bash
ADMIN_BOOTSTRAP_PASSWORD=your-password docker compose up --build
```

**Option B — locally with Maven + a local Postgres:**

```bash
createdb exam_lifecycle   # or via psql: CREATE DATABASE exam_lifecycle;
export DB_USER=exam_lifecycle DB_PASSWORD=exam_lifecycle
mvn spring-boot:run
```

## Running the tests

```bash
mvn test
```

This runs `DraftWorkflowIntegrationTest` against an in-memory H2 database
(see `src/test/resources/application-test.yml`) — no Docker or live Postgres
needed for testing. It walks the same sequence as the Python prototype's
`smoke_test.py`: admin bootstraps accounts, a non-admin is blocked from
self-registering, the full blueprint → draft → moderation → approval →
lock → export flow runs, the sequential gate is proven to block early
officer approval, immutability is proven post-lock, and the audit trail is
checked for both content and access.



## What's still needed for a complete capstone submission

This delivers the backend, persistence, security, containerization, and CI
— but not yet:
- **Flyway/Liquibase migrations.** `ddl-auto: update` (Hibernate
  auto-schema) is fine for a capstone prototype but call this out as a
  known simplification in your report — a real production system would use
  versioned migrations instead.
- **PDF-rendered watermarking.** Like the Python version, the watermark
  here is structured data (`watermark` string + individual attribution
  fields) rather than a rendered PDF overlay. Wire in Apache PDFBox to
  stamp this onto an actual PDF for the final polish.
- **Threat model / C4 diagrams / Figma mockups.** These are separate
  design-pack deliverables your brief asks for independent of the codebase.
