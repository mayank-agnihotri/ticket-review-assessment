# Ticket Management System

Spec-driven **Spring Boot** backend (Java 21, PostgreSQL + PGVector, Ollama via Spring AI) and **Next.js** frontend for support tickets and grounded AI Q&A over ticket history.

Assessment source: `docs/assessment.pdf`. Specifications: `spec/`. Implementation plan: `plan/implementation-plan.md`.

## Project structure

| Path | Purpose |
|------|---------|
| `src/main/java` | Spring Boot API (tickets, comments, transitions, RAG ingestion, `/api/ai/ask`) |
| `src/test/java` | Unit, API, and integration tests (H2 + optional PostgreSQL/PGVector) |
| `src/main/resources` | `application.yml`, Flyway migrations, RAG prompt |
| `spec/` | API, RAG, UI, state machine, evaluation specs |
| `rules/` | Engineering and review rules |
| `frontend/` | Next.js App Router UI |
| `docker-compose.yml` | Local PostgreSQL 16 + pgvector |
| `docs/` | Assessment PDF, prompt history, RAG testing notes, submission notes |

## Prerequisites

- **Java 21** (Gradle wrapper: `./gradlew`)
- **Node.js 20+** and npm (for `frontend/`)
- **Docker** (recommended) for PostgreSQL + pgvector
- **Ollama** (for live embeddings/chat and optional E2E tests), with models pulled locally:
  - `nomic-embed-text` (embeddings, 768 dimensions)
  - `llama3.2` (chat)

```bash
ollama pull nomic-embed-text
ollama pull llama3.2
```

## PostgreSQL / PGVector setup

Start the database (default user/db/password `ticket` / `ticketmgmt` / `ticket`):

```bash
docker compose up -d postgres
```

Default JDBC URL (matches `application.yml`):

`jdbc:postgresql://localhost:5432/ticketmgmt`

Flyway applies migrations on backend startup, including PGVector setup under `src/main/resources/db/postgres/` when using PostgreSQL.

Optional overrides: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.

Integration tests that need Postgres can use env vars:

- `PGVECTOR_IT_JDBC_URL` (default `jdbc:postgresql://localhost:5432/ticketmgmt`)
- `PGVECTOR_IT_DB_USER` (default `ticket`)
- `PGVECTOR_IT_DB_PASSWORD` (default `ticket`)

## Backend startup

RAG and `/api/ai/ask` require **pgvector** vector store (default in `application.yml`). Use PostgreSQL, not H2-only profiles, for full AI features.

```bash
export SPRING_DATASOURCE_PASSWORD=ticket   # if using docker-compose defaults
./gradlew bootRun
```

API base: `http://localhost:8080`  
Health: `http://localhost:8080/actuator/health`

Copy `src/main/resources/application-local.example.yml` to `application-local.yml` (gitignored) for local overrides if needed.

Default RAG-related config (overridable via env): `APP_RAG_TOP_K`, `APP_RAG_SIMILARITY_THRESHOLD`, `OLLAMA_BASE_URL`, `OLLAMA_EMBEDDING_MODEL`, `OLLAMA_CHAT_MODEL` — see `src/main/resources/application.yml`.

## Frontend startup

```bash
cd frontend
npm install
cp .env.example .env.local   # optional
npm run dev
```

UI: `http://localhost:3000` (redirects to `/tickets`).

Production build: `npm run build` then `npm start`.

## Frontend → backend proxy

The UI calls same-origin `/api/*`. Next.js rewrites those requests to the backend (`frontend/next.config.ts`):

- Server env: `BACKEND_URL` (default `http://localhost:8080`)
- No browser CORS configuration required on Spring Boot for local dev

Ensure the backend is running on the host/port given by `BACKEND_URL` before using the UI.

## Tests

### Backend (default)

```bash
./gradlew test
```

- Runs unit and API tests on **H2** (`repository-test` profile) for tickets, state machine, mocked RAG, etc.
- **Excludes** JUnit tag `ollama-e2e` (see `build.gradle.kts`).

### Backend (Ollama full-stack E2E)

Requires **PostgreSQL + pgvector**, **Ollama** with `nomic-embed-text` and `llama3.2`:

```bash
./gradlew ollamaE2eTest
```

Runs `RagEndToEndIntegrationTest` only (`@Tag("ollama-e2e")`).

### Environment-gated PGVector / R1–R5 tests

These use `@EnabledIf` and are **skipped** (not failed) when PostgreSQL is unreachable:

| Test class | Gate |
|------------|------|
| `PgVectorTicketKnowledgeVectorStoreIntegrationTest` | `RagIntegrationConditions#isPostgresReady` |
| `RagRetrievalEvaluationIntegrationTest` (R1–R5) | same |
| `TicketCreateTransitionIngestionIntegrationTest` | same |
| `RagEndToEndIntegrationTest` | `isEnvironmentReady` (Postgres **and** Ollama models); only in `ollamaE2eTest` |

See `docs/SUBMISSION.md` and `docs/rag-evaluation-testing.md`.

### Frontend

```bash
cd frontend && npm test && npm run lint && npm run build
```

## Basic full-stack run sequence

1. `docker compose up -d postgres`
2. `ollama serve` (if not already running) and pull models (see above)
3. `./gradlew bootRun` (port 8080)
4. `cd frontend && npm install && npm run dev` (port 3000)
5. Open `http://localhost:3000` — create tickets, comment, transition status, use **AI Q&A**
6. Note: new tickets are indexed for RAG after **PATCH**, **comment**, or **status transition** (not immediately on create)

## Further reading

- `docs/prompt-history.md` — documented AI mistake during development
- `docs/rag-evaluation-testing.md` — deterministic vs Ollama test split
- `frontend/README.md` — UI-only details
