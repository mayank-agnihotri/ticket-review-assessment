# Requirements — Ticket Management System

**Source of truth:** `docs/assessment.pdf`  
**Status:** Specification (pre-implementation)

## 1. Assessment goals (explicit)

- Demonstrate **spec-driven development** with AI assistance (not one-shot “build the complete application”).
- Design an **AI-native** feature (RAG) from the start.
- Validate **AI-generated code** and **AI-generated answers** (grounding, hallucination).
- Test **deterministic** logic (state machine) and **probabilistic** output (retrieval quality).
- Document at least **one meaningful AI mistake** caught during development.
- Maintain prompt history (`.specstory/history/`, `docs/prompt-history.md`).
- **No secrets committed.**

## 2. Technology stack (explicit)

| Component | Assessment |
|-----------|------------|
| Language | Java 21 |
| Framework | Spring Boot |
| AI | Spring AI |
| Database | PostgreSQL and/or H2 |
| Embeddings | Embedding model |
| Vector store | e.g. PGVector or Chroma |
| API | REST |
| Frontend | React/Next.js or equivalent |
| IDE AI | Cursor / GitHub Copilot / Kiro |

## 3. Functional requirements (explicit)

| ID | Requirement |
|----|-------------|
| FR-1 | Create a ticket |
| FR-2 | List tickets |
| FR-3 | View ticket details |
| FR-4 | Update **title, description, priority, assignee** |
| FR-5 | Add **comments** |
| FR-6 | **Search tickets by keyword** |
| FR-7 | **Filter tickets by status** |
| FR-8 | **Persist** data in a database |
| FR-9 | **Validate input** at the backend |
| FR-10 | **Display meaningful errors** in the UI |
| FR-11 | Natural-language **Q&A over ticket history**, grounded strictly in **real ticket data** |
| FR-12 | **Cite** specific ticket(s) used for any assistant answer |
| FR-13 | **Explicitly indicate** when no relevant tickets are found (no fabrication) |

## 4. State machine (explicit)

**Allowed:**

- `OPEN → IN_PROGRESS → RESOLVED → CLOSED`
- `OPEN → CANCELLED`
- `IN_PROGRESS → CANCELLED`

**Invalid transitions must be rejected** (backend).  
**Examples of invalid:** `CLOSED → OPEN`, `RESOLVED → OPEN`, `CANCELLED → OPEN`.

## 5. RAG / assistant (explicit)

- Pipeline: tickets → knowledge documents → chunk → embeddings → vector store → question → similarity search → LLM + context → grounded answer + ticket sources.
- Ingest: **description, comments, resolution notes** with metadata **ticketId, status, priority, assignee, category**.
- **Re-ingest / refresh** when ticket **updated or closed** (knowledge base must not go stale).
- API: **`POST /api/ai/ask`** with body `{ "question": "..." }`.
- **Top-K** and **similarity threshold** configurable, not hardcoded.
- **Grounding:** answer only from retrieved context for support-specific questions; no general-knowledge fallback.
- **No-match:** explicit honest response, not fabricated plausible answers.
- **Single retrieval → generate** — not an autonomous agent (no ticket creation, notifications, tool chaining).

## 6. Documentation (explicit)

- Chunking strategy and embedding model choice **documented and justified** in `architecture.md`.
- Embedding tradeoffs (local Ollama vs cloud) documented.

## 7. Core acceptance criteria (explicit)

- UI: create, list, view, update fields, change assignee, add comments, search, status filter.
- Valid/invalid status transitions (backend rejects invalid).
- Data survives application restart.
- Backend validation; UI meaningful errors.
- **State-machine integration tests pass.**
- Embeddings in vector store.
- `POST /api/ai/ask`: grounded answer, cites ticket ID(s), no-match returns honest “no relevant tickets found”.
- Re-ingestion on ticket **update**.
- Configurable top-K and similarity threshold.
- No secrets committed.
- ≥1 meaningful AI mistake documented.

## 8. Design decisions — not specified by assessment

| ID | Decision | See |
|----|----------|-----|
| DD-1 | PostgreSQL + PGVector for runtime; H2 optional for fast tests | `architecture.md` |
| DD-2 | Ollama for chat + embeddings (local dev) | `architecture.md` |
| DD-3 | REST paths, DTO shapes, error JSON | `api-contract.md` |
| DD-4 | Initial ticket status `OPEN` | `state-machine.md` |
| DD-5 | Ticket fields: `category`, `resolutionNotes`, public `ticketId` format | `data-model.md` |
| DD-6 | Paragraph-based chunking with size cap | `architecture.md`, `rag-ingestion.md` |
| DD-7 | Default top-K / similarity threshold values | `rag-ingestion.md` |
| DD-8 | RAG response JSON fields | `rag-api-contract.md` |
| DD-9 | No authentication (local assessment deployment) | `api-contract.md` |
| DD-10 | Next.js frontend | `ui-flow.md` |
| DD-11 | Keyword search: `title`, `description`, `ticketId` only (not comments) | `api-contract.md` |
| DD-12 | Simple ticket list response (no pagination metadata); optional list max cap | `api-contract.md` |
| DD-13 | Priority enum `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` | `data-model.md` |
| DD-14 | RAG no-match: HTTP 200, `noMatch`, fixed answer text; skip LLM when no chunks | `rag-api-contract.md` |
| DD-15 | Title in knowledge document (beyond assessment ingest list) | `data-model.md`, `rag-ingestion.md` |
| DD-16 | Ollama model tags as configurable local defaults | `architecture.md` |

## 9. Out of scope (explicit / implied)

- Autonomous agent workflows from `/api/ai/ask`.
- Authentication/authorization (not in assessment — optional DD-9).
