# Implementation Plan — Ticket Management System

**Sources:** `spec/*`, `rules/*`, `commands/*`  
**Status:** Plan only — no implementation in this document.

## Principles

- Follow SDD workflow (`rules/sdd-workflow.md`): implement against `spec/`, review with `commands/`.
- Layering per `rules/java-springboot.md`: `api` → `service` → `domain` → `repository`.
- No auth, DELETE ticket API, agents, or notifications (`spec/requirements.md` §9).
- Configurable RAG top-K/threshold; Ollama model tags externalized (`spec/architecture.md` §6–8).

---

## Phase 1 — Project / backend setup

### Task 1.1 — Gradle Spring Boot skeleton

| | |
|--|--|
| **Objective** | Java 21 + Spring Boot + Spring Web + Validation + Spring Data JPA; package layout per `rules/java-springboot.md`. |
| **Files / components** | `build.gradle.kts` (or `build.gradle`), `settings.gradle`, `src/main/java/.../Application.java`, base packages: `ticket`, `ai`, `common`. |
| **Dependencies** | None (first task). |
| **Verification** | `./gradlew bootRun` starts (may fail on DB until 1.2); `./gradlew compileJava`. |

### Task 1.2 — Configuration profiles and properties

| | |
|--|--|
| **Objective** | `application.yml` + `application-local.yml`; externalize DB, Ollama URLs, `app.rag.*`, `app.tickets.list-max` (`spec/architecture.md`, `spec/rag-ingestion.md`, `spec/api-contract.md`). |
| **Files / components** | `src/main/resources/application*.yml`, `@ConfigurationProperties` classes in `common.config`. |
| **Dependencies** | 1.1 |
| **Verification** | Context loads with test profile; no secrets in repo (`rules/security.md`). |

### Task 1.3 — Global API error handling

| | |
|--|--|
| **Objective** | `@RestControllerAdvice` returning `spec/api-contract.md` error shape (`code`, `message`, `fieldErrors`). |
| **Files / components** | `common.api.ErrorResponse`, `GlobalExceptionHandler`, domain exceptions. |
| **Dependencies** | 1.1 |
| **Verification** | Unit test or slice test: validation error → 400 + JSON shape. |

### Task 1.4 — Docker Compose for local Postgres + pgvector (dev only)

| | |
|--|--|
| **Objective** | Runnable PostgreSQL with pgvector for local dev (`spec/architecture.md` §5). |
| **Files / components** | `docker-compose.yml` at repo root (infrastructure, not app code). |
| **Dependencies** | 1.2 |
| **Verification** | `docker compose up -d`; JDBC connects from app. |

---

## Phase 2 — Database and entities

### Task 2.1 — JPA entities and enums

| | |
|--|--|
| **Objective** | `Ticket`, `Comment`; enums `TicketStatus`, `Priority` per `spec/data-model.md`. |
| **Files / components** | `ticket.domain.Ticket`, `Comment`, enums; no REST exposure of entities. |
| **Dependencies** | 1.1, 1.2 |
| **Verification** | Unit tests for enum values (`LOW`–`CRITICAL`); entity mapping smoke with H2 or Testcontainers. |

### Task 2.2 — Repositories and ticket ID generation

| | |
|--|--|
| **Objective** | `TicketRepository`, `CommentRepository`; generate public `ticketId` (`TKT-{sequence}`) — **Design Decision** per `spec/data-model.md`. |
| **Files / components** | `ticket.repository.*`, ID generator service. |
| **Dependencies** | 2.1 |
| **Verification** | Integration test: save ticket, assert `ticketId` format and `OPEN` initial status (`spec/state-machine.md` §2). |

### Task 2.3 — Schema strategy (Flyway/Liquibase or ddl-auto for dev)

| | |
|--|--|
| **Objective** | Persist tickets/comments; data survives restart (`spec/requirements.md` AC). |
| **Files / components** | Migration scripts **or** documented `ddl-auto` for local only (team choice at implement time). |
| **Dependencies** | 2.1, 1.4 |
| **Verification** | Integration test: insert data, restart context, read again (`spec/test-strategy.md` §4). |

---

## Phase 3 — Ticket APIs

### Task 3.1 — DTOs and mappers

| | |
|--|--|
| **Objective** | Request/response records for `spec/api-contract.md` (`TicketResponse`, `TicketDetailResponse`, create/patch DTOs). |
| **Files / components** | `ticket.api.dto.*`, mapper (manual or MapStruct). |
| **Dependencies** | 2.1 |
| **Verification** | Unit tests mapping entity ↔ DTO. |

### Task 3.2 — Create and get ticket

| | |
|--|--|
| **Objective** | `POST /api/tickets`, `GET /api/tickets/{ticketId}`; validation on create. |
| **Files / components** | `TicketController`, `TicketService` (create, getByTicketId). |
| **Dependencies** | 2.2, 2.3, 3.1, 1.3 |
| **Verification** | API tests: 201 + 200; 404; 400 blank title (`spec/test-strategy.md` §5). |

### Task 3.3 — List tickets (simple array)

| | |
|--|--|
| **Objective** | `GET /api/tickets` returns JSON array; optional `list-max` cap (`spec/api-contract.md` §2.2). |
| **Files / components** | `TicketController.list`, repository query. |
| **Dependencies** | 3.2 |
| **Verification** | API test: empty list `[]`; multiple tickets returned. |

### Task 3.4 — Patch ticket fields

| | |
|--|--|
| **Objective** | `PATCH /api/tickets/{ticketId}` for title, description, priority, assignee, category, resolutionNotes; at least one field required. |
| **Files / components** | `TicketController.patch`, `TicketService.update`. |
| **Dependencies** | 3.2 |
| **Verification** | API tests per assessment update fields; 400 on empty patch. |

---

## Phase 4 — State machine

### Task 4.1 — Transition table and service

| | |
|--|--|
| **Objective** | Encode allowed transitions (`spec/state-machine.md` §3); reject all others with 409. |
| **Files / components** | `ticket.domain.TicketStateMachine` or `TicketTransitionService`. |
| **Dependencies** | 2.1 |
| **Verification** | Unit tests: all §3 transitions pass; §4 invalid pairs fail. |

### Task 4.2 — Transition API

| | |
|--|--|
| **Objective** | `POST /api/tickets/{ticketId}/transitions` with `{ "status" }`. |
| **Files / components** | `TicketController.transition`, wire to state machine. |
| **Dependencies** | 4.1, 3.2, 1.3 |
| **Verification** | API tests: assessment invalid examples `CLOSED→OPEN`, `RESOLVED→OPEN`, `CANCELLED→OPEN` → 409 (`spec/test-strategy.md` §3). |

---

## Phase 5 — Comments, search and filtering

### Task 5.1 — Add comment API

| | |
|--|--|
| **Objective** | `POST /api/tickets/{ticketId}/comments`; comments on detail response. |
| **Files / components** | `CommentController` or nested in `TicketController`, `CommentService`. |
| **Dependencies** | 3.2, 3.1 |
| **Verification** | API test: 201; comment appears on `GET` detail. |

### Task 5.2 — Keyword search

| | |
|--|--|
| **Objective** | `GET /api/tickets?q=` matches title, description, `ticketId` only (**DD** — not comments) (`spec/api-contract.md`). |
| **Files / components** | Repository query or specification API. |
| **Dependencies** | 3.3 |
| **Verification** | API test: seed tickets, `q` returns expected subset. |

### Task 5.3 — Status filter

| | |
|--|--|
| **Objective** | `GET /api/tickets?status=` filter. |
| **Files / components** | Extend list query. |
| **Dependencies** | 3.3 |
| **Verification** | API test: filter `OPEN` excludes `CLOSED`. |

### Task 5.4 — Combined q + status

| | |
|--|--|
| **Objective** | Support both query params together (UI needs — `spec/ui-flow.md` §7–8). |
| **Files / components** | List endpoint query composition. |
| **Dependencies** | 5.2, 5.3 |
| **Verification** | API test with both params. |

---

## Phase 6 — RAG ingestion

### Task 6.1 — Knowledge document builder

| | |
|--|--|
| **Objective** | Assemble text from description, comments, resolution notes + title (**DD**) per `spec/data-model.md`, `spec/rag-ingestion.md`. |
| **Files / components** | `ai.ingestion.KnowledgeDocumentBuilder`. |
| **Dependencies** | 2.1, 5.1 |
| **Verification** | Unit test: fixture ticket → expected document sections. |

### Task 6.2 — Chunking service

| | |
|--|--|
| **Objective** | Paragraph-based chunking + max chars (`spec/rag-ingestion.md` §3, `spec/architecture.md` §7). |
| **Files / components** | `ai.ingestion.ChunkingService`. |
| **Dependencies** | 6.1 |
| **Verification** | Unit tests: paragraph splits, oversized paragraph split. |

### Task 6.3 — Ingestion orchestrator + hooks

| | |
|--|--|
| **Objective** | Re-ingest on PATCH, comment add, status transition (incl. closed) — sync after commit (`spec/rag-ingestion.md` §6). |
| **Files / components** | `TicketIngestionService`; call from `TicketService`, `CommentService`, transition path. |
| **Dependencies** | 6.2, 3.4, 4.2, 5.1 |
| **Verification** | Integration test stub (mock vector store) records ingest invoked on update/comment/close. |

---

## Phase 7 — Vector store and embeddings

### Task 7.1 — Spring AI + Ollama embedding client

| | |
|--|--|
| **Objective** | Configure embedding model via properties (`nomic-embed-text` default); dimensions match PGVector (`spec/architecture.md` §6). |
| **Files / components** | `build.gradle` Spring AI Ollama + pgvector starters; `ai.config`. |
| **Dependencies** | 1.2, 1.4 |
| **Verification** | Manual or integration test with Testcontainers + Ollama test double/mock. |

### Task 7.2 — PGVector store initialization

| | |
|--|--|
| **Objective** | Vector store with metadata keys: ticketId, status, priority, assignee, category (`spec/rag-ingestion.md` §2). |
| **Files / components** | `ai.vector.VectorStoreConfig`, metadata mapping. |
| **Dependencies** | 7.1 |
| **Verification** | Integration test: add/delete by `ticketId` filter. |

### Task 7.3 — Embed and upsert pipeline

| | |
|--|--|
| **Objective** | Delete-by-ticketId then embed chunks and insert (`spec/rag-ingestion.md` procedure). |
| **Files / components** | `ai.ingestion.VectorIngestionService` implementing 6.3 hooks. |
| **Dependencies** | 6.3, 7.2 |
| **Verification** | Integration test: after ticket update, search retrieves new text (`spec/test-strategy.md` §7). |

---

## Phase 8 — `/api/ai/ask`

### Task 8.1 — Retrieval service

| | |
|--|--|
| **Objective** | Similarity search with configurable `app.rag.top-k` and `similarity-threshold`; no hardcoding. |
| **Files / components** | `ai.rag.RetrievalService`. |
| **Dependencies** | 7.2, 1.2 |
| **Verification** | Unit/integration tests with fixture vectors; threshold forces empty (`spec/test-strategy.md` §8). |

### Task 8.2 — Grounded generation + citations

| | |
|--|--|
| **Objective** | Single LLM call with context; populate `citedTicketIds` from chunk metadata (`spec/rag-api-contract.md` §3, §5). |
| **Files / components** | `ai.rag.RagService`, prompt template (resource), Ollama chat client. |
| **Dependencies** | 8.1, 7.1 |
| **Verification** | API test with mocked LLM: citations ⊆ retrieved IDs. |

### Task 8.3 — No-match path (skip LLM)

| | |
|--|--|
| **Objective** | No chunks ≥ threshold → HTTP 200, `noMatch: true`, exact answer text, empty citations, **no** LLM call (`spec/rag-api-contract.md` §4). |
| **Files / components** | Branch in `RagService`. |
| **Dependencies** | 8.1 |
| **Verification** | API test: empty retrieval → no-match JSON; mock LLM never called. |

### Task 8.4 — Ask controller

| | |
|--|--|
| **Objective** | `POST /api/ai/ask` request validation; wire `RagService`. |
| **Files / components** | `ai.api.AskController`, `AskRequest`/`AskResponse` DTOs. |
| **Dependencies** | 8.2, 8.3, 1.3 |
| **Verification** | Full API tests per `spec/test-strategy.md` §6. |

---

## Phase 9 — Backend tests

### Task 9.1 — State-machine integration suite

| | |
|--|--|
| **Objective** | Mandatory assessment AC: all allowed + invalid transitions (`spec/test-strategy.md` §3). |
| **Files / components** | `*StateMachine*IntegrationTest` with Testcontainers Postgres. |
| **Dependencies** | 4.2 |
| **Verification** | `./gradlew test` — all pass. |

### Task 9.2 — Ticket API suite

| | |
|--|--|
| **Objective** | CRUD, validation, search, filter, comments (`spec/test-strategy.md` §5). |
| **Files / components** | `TicketApiTest`, `CommentApiTest`. |
| **Dependencies** | 5.4 |
| **Verification** | CI green. |

### Task 9.3 — RAG API suite

| | |
|--|--|
| **Objective** | Grounding, citations, no-match, re-ingestion (`spec/test-strategy.md` §6–7). |
| **Files / components** | `AskApiTest`, ingestion integration tests. |
| **Dependencies** | 8.4, 7.3 |
| **Verification** | CI green; use mocks for Ollama where needed (`rules/testing.md`). |

### Task 9.4 — Persistence / restart test

| | |
|--|--|
| **Objective** | Data survives restart (`spec/test-strategy.md` §4). |
| **Dependencies** | 2.3 |
| **Verification** | Integration test as specified. |

---

## Phase 10 — Frontend / UI

### Task 10.1 — Next.js app scaffold

| | |
|--|--|
| **Objective** | Next.js app with API base URL config (`spec/ui-flow.md`). |
| **Files / components** | `frontend/` package.json, env `NEXT_PUBLIC_API_URL`. |
| **Dependencies** | 3.2 (API available) |
| **Verification** | `npm run dev` loads home. |

### Task 10.2 — Ticket list, search, filter

| | |
|--|--|
| **Objective** | List + `q` + `status` (`spec/ui-flow.md` §3, §7–8). |
| **Files / components** | `app/tickets/page.tsx`, API client. |
| **Dependencies** | 10.1, 5.4 |
| **Verification** | Manual: list, search, filter against running backend. |

### Task 10.3 — Create and detail views

| | |
|--|--|
| **Objective** | Create form, detail with comments (`spec/ui-flow.md` §2, §4, §6). |
| **Files / components** | `app/tickets/new`, `app/tickets/[ticketId]`. |
| **Dependencies** | 10.2, 5.1 |
| **Verification** | Manual AC: create from UI, view details, add comment. |

### Task 10.4 — Update fields and transitions

| | |
|--|--|
| **Objective** | PATCH form; transition UI with 409 handling (`spec/ui-flow.md` §5, §9). |
| **Files / components** | Edit form, status actions. |
| **Dependencies** | 10.3, 4.2 |
| **Verification** | Manual: valid/invalid transitions, error messages. |

### Task 10.5 — Error display

| | |
|--|--|
| **Objective** | Meaningful errors from API (`spec/ui-flow.md` §11). |
| **Files / components** | Shared error component. |
| **Dependencies** | 10.2 |
| **Verification** | Manual: trigger 400/409. |

### Task 10.6 — AI Q&A panel

| | |
|--|--|
| **Objective** | Ask UI; citations as links; no-match styling (`spec/ui-flow.md` §10). |
| **Files / components** | `AskPanel` component. |
| **Dependencies** | 10.3, 8.4 |
| **Verification** | Manual + `commands/review-rag-output.md` on samples. |

---

## Phase 11 — RAG evaluation

### Task 11.1 — Fixture dataset

| | |
|--|--|
| **Objective** | Seed tickets for assessment example questions (`spec/evaluation-strategy.md` §3 R1–R5). |
| **Files / components** | Test fixtures or `data/dev-seed.sql` (dev only). |
| **Dependencies** | 7.3 |
| **Verification** | Retrieval recall checks documented. |

### Task 11.2 — Retrieval evaluation tests

| | |
|--|--|
| **Objective** | Probabilistic suite: top-K/threshold tuning (`spec/evaluation-strategy.md`). |
| **Files / components** | `RagRetrievalEvaluationTest` (tagged integration). |
| **Dependencies** | 11.1, 8.1 |
| **Verification** | Expected `ticketId` in top-K for fixtures. |

### Task 11.3 — Grounding review pass

| | |
|--|--|
| **Objective** | Run `commands/review-rag-output.md` on live stack; document results. |
| **Dependencies** | 8.4, 10.6 |
| **Verification** | Checklist pass; no hallucination on fixtures. |

---

## Phase 12 — Documentation and final review

### Task 12.1 — Sync architecture.md in repo

| | |
|--|--|
| **Objective** | Ensure implemented chunking/embedding match `spec/architecture.md` (assessment AC). |
| **Files / components** | `spec/architecture.md` or `docs/architecture-impl-notes.md` if drift — prefer updating spec only if behavior matches. |
| **Dependencies** | 7.1, 6.2 |
| **Verification** | `commands/review-spec.md` against implementation. |

### Task 12.2 — AI mistake documentation

| | |
|--|--|
| **Objective** | ≥1 meaningful AI mistake recorded (`spec/evaluation-strategy.md` §8, `docs/prompt-history.md`). |
| **Dependencies** | Throughout project |
| **Verification** | Entry exists in `docs/prompt-history.md`. |

### Task 12.3 — Prompt history hygiene

| | |
|--|--|
| **Objective** | `.specstory/history/` + `docs/prompt-history.md` maintained (`rules/prompt-history.md`). |
| **Dependencies** | None |
| **Verification** | Files present and non-empty. |

### Task 12.4 — Final review gates

| | |
|--|--|
| **Objective** | Run `commands/review-code.md`, `review-security.md`, `review-spec.md`; fix loop per SDD. |
| **Dependencies** | 9.x, 10.x, 11.x |
| **Verification** | Assessment AC checklist in `spec/requirements.md` §7 manually confirmed. |

---

## Dependency / order summary

```
1.1 → 1.2 → 1.3 → 1.4
         ↓
2.1 → 2.2 → 2.3
         ↓
3.1 → 3.2 → 3.3 → 3.4 ─────────────────────────┐
         ↓                    ↓                  │
4.1 → 4.2                    │                  │
         ↓                    ↓                  │
5.1 → 5.2 → 5.3 → 5.4        │                  │
         ↓                    ↓                  │
6.1 → 6.2 → 6.3 ─────────────┴→ 7.1 → 7.2 → 7.3
                                    ↓
                              8.1 → 8.2/8.3 → 8.4
                                    ↓
                              9.1–9.4 (parallel after features)
                                    ↓
10.1 → 10.2 → 10.3 → 10.4 → 10.5 → 10.6
                                    ↓
                              11.1 → 11.2 → 11.3
                                    ↓
                              12.1 → 12.2 → 12.3 → 12.4
```

**Parallelism notes:**

- Phase 4 can start after 3.2 (does not need search/comments).
- Phase 7 can start 7.1–7.2 while finishing 5.x if ingestion interfaces are stubbed.
- Frontend (10.x) can begin after 3.2 + 4.2; AI panel (10.6) needs 8.4.

**Suggested milestones:**

1. **M1:** Tickets CRUD + state machine + tests (1.x–4.x, 9.1–9.2).  
2. **M2:** Comments + search/filter (5.x).  
3. **M3:** RAG ingest + ask + backend RAG tests (6.x–8.x, 9.3).  
4. **M4:** UI complete (10.x).  
5. **M5:** Evaluation + docs + review (11.x–12.x).

---

## Review commands (use per milestone)

| Milestone | Command |
|-----------|---------|
| After spec-aligned API | `commands/review-code.md` |
| After RAG | `commands/review-rag-output.md` |
| Before merge | `commands/review-security.md`, `commands/generate-tests.md` (gap check) |
