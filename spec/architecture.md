# Architecture — Ticket Management System

**Source:** `docs/assessment.pdf`, `spec/requirements.md`

## 1. High-level view

```
┌─────────────┐     REST      ┌──────────────────────────────────────┐
│  Next.js UI │ ◄──────────► │  Spring Boot API                     │
└─────────────┘               │  ├─ ticket (CRUD, search, transitions) │
                              │  ├─ comment                          │
                              │  └─ ai (POST /api/ai/ask)             │
                              └───────┬──────────────────┬─────────────┘
                                      │ JDBC            │ Spring AI
                                      ▼                 ▼
                              ┌──────────────┐   ┌─────────────┐
                              │ PostgreSQL   │   │ Ollama      │
                              │ + pgvector   │   │ chat+embed  │
                              └──────────────┘   └─────────────┘
```

## 2. Component responsibilities

| Component | Responsibility |
|-----------|----------------|
| **Ticket API** | CRUD, search, filter, status transitions, validation |
| **Comment API** | Add comments; trigger re-ingestion |
| **Ticket service** | Business rules, state machine, orchestration |
| **Ingestion service** | Build knowledge docs, chunk, embed, upsert/delete vectors |
| **RAG service** | Similarity search → prompt assembly → single LLM call → citations |
| **PostgreSQL** | Relational ticket/comment data (durable) |
| **PGVector** | Embedding storage + similarity search (same Postgres instance) |
| **Ollama** | Embedding model + chat model for grounded answers |

## 3. Data flow — ticket operations

1. UI calls REST → controller → service → JPA repository → PostgreSQL.
2. On ticket/comment change or close: service publishes ingestion event (sync call in same transaction boundary **Design Decision — not specified by assessment**).
3. Ingestion rebuilds knowledge text for that `ticketId`, re-chunks, re-embeds, replaces vectors for that ticket.

## 4. Data flow — RAG ask

1. `POST /api/ai/ask` → RAG service embeds question.
2. Vector similarity search with configured **top-K** and **similarity threshold**.
3. If no chunks pass threshold → return no-match response (no LLM call **Design Decision** to avoid fabrication).
4. Else assemble context from retrieved chunks → one LLM generation → response with answer + cited `ticketId`s.

## 5. Database and vector store

| Choice | Rationale |
|--------|-----------|
| **PostgreSQL** (runtime) | Assessment allows PostgreSQL; durable persistence; survives restart |
| **PGVector** extension | Assessment example; single stack for relational + vectors; simpler ops than separate Chroma |

**Design Decision — not specified by assessment:** Use Docker Compose for PostgreSQL+pgvector in local dev (not part of this spec’s deliverables).

**Design Decision:** H2 without pgvector for unit tests that do not need vectors; integration tests use PostgreSQL+pgvector (Testcontainers).

## 6. Embedding and LLM approach

**Design Decision — not specified by assessment:** Local development **defaults** (configurable externally; do not assume models are pre-installed):

| Role | Default model tag | Config (example) |
|------|-------------------|------------------|
| **Embeddings** | `nomic-embed-text` | `spring.ai.ollama.embedding.options.model` |
| **Chat** | `llama3.2` | `spring.ai.ollama.chat.options.model` |

| Topic | Guidance |
|-------|----------|
| **Dimensions** | Embedding vector size (e.g. **768** for `nomic-embed-text`) **must match** PGVector / Spring AI vector store configuration |
| **Trade-offs** | **Local Ollama:** no API cost, privacy, CPU latency; **vs cloud:** scale and quality at cost and network dependency |
| **Availability** | Model tags are configuration only—operators pull/run models in their environment; application must not hard-code model presence |

Spring AI abstracts providers; switching to cloud embedding/chat is a configuration change if dimensions/API align.

## 7. Chunking strategy

**Strategy:** **Paragraph-based splitting** on blank lines, with a **maximum character cap per chunk** (merge/split long paragraphs).

**Justification (ticket data):**

- Tickets are written as short prose (description, comments, resolution notes); paragraph boundaries preserve semantic units better than arbitrary fixed windows.
- Cap prevents oversized chunks from diluting retrieval and exceeding context limits.

**Design Decision — not specified by assessment:**

- `max-chunk-chars`: **1500** (tunable via config).
- One logical **knowledge document per ticket** assembled from description + comments + resolution notes (+ **title** as additional retrieval field per `data-model.md`), then chunked; each chunk carries the same ticket-level metadata.

## 8. Configurable retrieval

| Property | Purpose | Default (Design Decision) |
|----------|---------|---------------------------|
| `app.rag.top-k` | Max chunks retrieved | `5` |
| `app.rag.similarity-threshold` | Minimum cosine similarity (0–1) | `0.65` |

Assessment requires **not hardcoded** — load from `application.yml` / env overrides.

## 9. Assumptions

- Single-tenant local/demo deployment (no auth) unless DD-9 is revised.
- Synchronous re-ingestion acceptable for assessment scale; async queue is **not required**.

## 10. Non-goals

- Agent tool-calling, automatic ticket creation from chat, notifications.
- Multi-region HA.
