# Test Strategy — Ticket Management System

**Source:** `docs/assessment.pdf`, `spec/evaluation-strategy.md`, `rules/testing.md`

## 0. AI mistake documentation (assessment)

The assessment requires at least **one meaningful AI mistake or incorrect suggestion** (code or RAG answer) to be caught and documented during development. That process is defined in **`spec/evaluation-strategy.md` §8**—not an automated test assertion.

## 1. Pyramid

| Layer | Scope |
|-------|--------|
| Unit | State machine rules, validators, document builder, chunker |
| Integration | JPA + PostgreSQL (Testcontainers), state machine + DB |
| API | MockMvc/WebTestClient full HTTP |
| RAG | Retrieval fixtures + mocked LLM; optional manual eval |

## 2. Unit tests

- `TicketStateMachine` / transition table: allowed vs invalid pairs.
- Chunking: paragraph splits, max size enforcement.
- Knowledge document assembly includes description, comments, resolution notes.
- Metadata mapping on chunks.

## 3. Integration tests — state machine (required by assessment)

- **All allowed transitions** (`state-machine.md` §3) against real DB.
- **Invalid transitions** including assessment examples:
  - `CLOSED → OPEN`
  - `RESOLVED → OPEN`
  - `CANCELLED → OPEN`
- Additional invalid samples from `state-machine.md` §4.
- Expect `409` + `INVALID_STATUS_TRANSITION` when via API layer.

## 4. Integration tests — persistence / restart

- Create ticket + comment → restart context or new connection → data still present (**assessment**).
- **Design Decision:** Use `@DirtiesContext` or separate test verifying repository read after simulated restart.

## 5. API tests — tickets

| Area | Tests |
|------|-------|
| CRUD | Create 201, get 200, patch 200, 404 |
| Validation | Blank title 400 + fieldErrors |
| Search | `q` returns matching ticket |
| Filter | `status=OPEN` excludes others |
| Comments | POST 201, appears on detail |
| Transitions | Valid 200, invalid 409 |

## 6. API tests — RAG

| Test | Assertion |
|------|-----------|
| Grounded ask | Mock retrieval + LLM; `grounded=true`, `citedTicketIds` non-empty |
| Citations | Subset of retrieved ticket IDs |
| No-match | `noMatch=true`, answer contains “No relevant tickets found”, `citedTicketIds` empty |
| Insufficient evidence | Threshold excludes all chunks → no-match without fabrication |
| Ask validation | Empty question → 400 |

## 7. Re-ingestion tests

- After PATCH description: vector store for `ticketId` reflects new text (query metadata count or search).
- After comment add: chunk content includes comment.
- After transition to `CLOSED`: metadata `status` updated.

## 8. Configurable top-K / threshold

- `@TestPropertySource` or dynamic properties:
  - Low top-K → at most K chunks in mock prompt builder.
  - High threshold → no-match on fixture question.

## 9. RAG evaluation (probabilistic)

See `evaluation-strategy.md` §3–5; run retrieval recall suite in CI with seeded data.

## 10. Tools

| Tool | Use |
|------|-----|
| JUnit 5 | All tests |
| Spring Boot Test | Integration/API |
| Testcontainers PostgreSQL + pgvector | **Design Decision** |
| Mockito | Ollama/ChatModel mocks |
| AssertJ | Fluent assertions |

## 11. Coverage goals (Design Decision)

- 100% transition table coverage (deterministic).
- All `api-contract.md` endpoints have at least one happy-path and one error-path test.
- RAG: no-match + grounded + citation integrity.

## 12. Out of scope

- Load/performance testing (unless added later).
- E2E browser tests (**Design Decision:** optional Playwright later).

## 13. CI

- Unit + integration + API on every push; probabilistic RAG suite may use fixed seeds.
