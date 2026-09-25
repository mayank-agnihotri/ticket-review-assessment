# Testing Guidelines

Source: Ticket Management System assessment (`docs/assessment.pdf`).

## Principles

- Test **deterministic** behavior (CRUD, validation, **state machine**) separately from **probabilistic** behavior (RAG retrieval quality).
- Specifications (`spec/test-strategy.md`, `spec/state-machine.md`) drive what to test—do not invent requirements in tests.
- **State-machine integration tests must pass** (assessment acceptance criterion).

## Unit tests

- Focus on pure logic: transition guards, validators, chunk/metadata builders, mapping helpers.
- Mock external systems (LLM, vector store, Ollama) at service boundaries.
- Fast; no Spring context unless necessary.

## Integration tests

- Use Spring Boot test slice or full context with **real database** (PostgreSQL/H2 per spec decision).
- Cover persistence: data **survives application restart** (assessment)—verify via DB, not mocks.
- **State machine**: every **allowed** transition from the assessment (`OPEN → IN_PROGRESS → RESOLVED → CLOSED`, `OPEN → CANCELLED`, `IN_PROGRESS → CANCELLED`) and explicit **invalid** examples (`CLOSED → OPEN`, `RESOLVED → OPEN`, `CANCELLED → OPEN`) plus any transitions defined in `spec/state-machine.md`.
- Re-ingestion: when a ticket is **updated** (and per RAG section **closed**), assert embeddings/knowledge documents are refreshed—exact assertion strategy is a **design decision** in `spec/rag-ingestion.md`.

## API tests

- Exercise REST endpoints (ticket CRUD, comments, search, filter, status, `POST /api/ai/ask`) with `MockMvc` or `WebTestClient`.
- Assert HTTP status, response body shape per `spec/api-contract.md`, and validation error payloads.
- `POST /api/ai/ask`: test in-scope grounded response shape, **ticket ID citation** presence, and **no relevant tickets found** when retrieval is empty/below threshold.

## RAG / retrieval evaluation

- Define evaluation cases in `spec/evaluation-strategy.md` (example questions from assessment: payment failures, resolution for a ticket ID, similar resolved tickets, etc.).
- Prefer **fixture tickets** with known content; assert retrieval includes expected `ticketId` metadata before LLM is invoked (or mock LLM and assert prompt context contains retrieved chunks).
- Separate tests for: configurable **top-K**, **similarity threshold**, and explicit **no-match** behavior.
- Do not assert exact LLM prose—assert grounding constraints (citations, no-match message, no answer when context empty).

## Naming and organization

- Mirror production packages: `...ticket.service.TicketStateMachineTest`, `...ai.AskEndpointIntegrationTest`.
- Name tests: `methodName_condition_expectedOutcome` or `should_expected_when_condition`.
- Tag slow/integration tests (`@Tag("integration")`) for selective CI runs.

## Design decisions (spec phase)

- Testcontainers vs embedded DB, Ollama test doubles, and RAG golden datasets are **design decisions**—document in `spec/test-strategy.md`.
