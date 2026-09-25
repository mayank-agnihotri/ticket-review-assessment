# Data Model — Ticket Management System

**Source:** `docs/assessment.pdf`, `spec/requirements.md`

## 1. Entities overview

```
Ticket 1──* Comment
Ticket 1──* KnowledgeChunk (vector store; logical, may be table `ticket_embedding` or Spring AI store)
```

## 2. Ticket

| Field | Type | Assessment / notes |
|-------|------|------------------|
| `id` | UUID (internal PK) | **Design Decision — not specified by assessment** |
| `ticketId` | String (business ID, e.g. `TKT-1001`) | Example in assessment questions; **Design Decision** format `TKT-{sequence}` |
| `title` | String | Explicit (update) |
| `description` | String | Explicit (update); RAG source |
| `priority` | Enum: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` | Assessment requires **priority**; enum values are **Design Decision — assessment requires priority but does not define the allowed values** |
| `assignee` | String (nullable) | Explicit (update); RAG metadata |
| `status` | Enum: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED` | State machine |
| `category` | String | RAG metadata only in assessment — **Design Decision** to expose on ticket |
| `resolutionNotes` | Text (nullable) | Assessment RAG source when present; optional ticket data—**no** transition validation (**Design Decision** field on ticket) |
| `createdAt` | Instant | **Design Decision** (audit/UI) |
| `updatedAt` | Instant | **Design Decision** (re-ingestion, UI) |

### Resolution notes

- Optional; may be set or updated via ticket PATCH at any time.
- Ingested into the knowledge base when present; empty/null is omitted from RAG text.
- **No** business rule ties `resolutionNotes` to `RESOLVED` or `CLOSED` transitions.

## 3. Comment

| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID | **Design Decision** |
| `ticket` | FK → Ticket | |
| `body` | String | Assessment: add comments; RAG source |
| `author` | String | **Design Decision — not specified by assessment** |
| `createdAt` | Instant | **Design Decision** |

Assessment does not define edit/delete comments — **out of scope** unless added later.

## 4. Knowledge document (logical)

Not necessarily a relational table; may be materialized only at ingestion time.

**Composed text per ticket:**

**Design Decision — assessment explicitly requires description, comments and resolution notes; title is an additional retrieval field.**

```
Title: {title}
Category: {category}
Description: {description}
Comments:
- [{createdAt}] {body}
Resolution: {resolutionNotes}
```

## 5. Vector / chunk record

Stored in PGVector via Spring AI vector store.

| Field | Notes |
|-------|-------|
| Embedding vector | From Ollama embedding model |
| `content` | Chunk text |
| Metadata `ticketId` | **Assessment (required)** |
| Metadata `status` | **Assessment** |
| Metadata `priority` | **Assessment** |
| Metadata `assignee` | **Assessment** |
| Metadata `category` | **Assessment** |
| Metadata `chunkIndex` | **Design Decision** |
| Metadata `sourceType` | `DESCRIPTION` \| `COMMENT` \| `RESOLUTION` — **Design Decision** |

**Design Decision:** Delete all vectors for a `ticketId` before re-insert on re-ingestion (idempotent refresh).

## 6. Relationships and lifecycle

1. **Create ticket** → status `OPEN` (**Design Decision** initial status).
2. **Updates** to title, description, priority, assignee, category, resolutionNotes → persist → **re-ingest**.
3. **Add comment** → persist → **re-ingest**.
4. **Status transition** → persist → **re-ingest**; **closed** explicitly required by assessment for refresh.
5. **Restart** → data loaded from PostgreSQL; vectors from PGVector (both durable).

## 7. Design decisions summary

| Item | Label |
|------|--------|
| UUID internal IDs, `TKT-{n}` public ID | Design Decision |
| `category`, `resolutionNotes`, timestamps, comment `author` | Design Decision |
| Priority enum values (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`) | Design Decision — assessment requires priority but does not define allowed values |
| Vector metadata `chunkIndex`, `sourceType` | Design Decision |
