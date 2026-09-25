# RAG Ingestion — Ticket Management System

**Source:** `docs/assessment.pdf`, `spec/architecture.md`, `spec/data-model.md`

## 1. Pipeline

```
Ticket (description, comments, resolutionNotes)
  → assemble knowledge document
  → paragraph chunking (+ max size)
  → generate embeddings (Ollama)
  → upsert into PGVector (metadata per chunk)
```

**Not in scope:** autonomous agents or tool calls.

## 2. Knowledge document assembly

Per ticket, build one document (see `data-model.md` template) from:

| Source | Assessment |
|--------|------------|
| Description | Yes |
| Comments | Yes |
| Resolution notes | Yes (when present on ticket) |
| Title | **Design Decision — assessment explicitly requires description, comments and resolution notes; title is an additional retrieval field** |

Metadata copied to **every chunk**:

| Key | Assessment |
|-----|------------|
| `ticketId` | Required |
| `status` | Required |
| `priority` | Required |
| `assignee` | Required |
| `category` | Required |

## 3. Chunking

**Strategy:** Paragraph-based (split on `\n\n+`), then split any paragraph exceeding **1500 characters** at sentence boundaries where possible.

**Justification:** See `architecture.md` §7.

**Design Decision:** `app.rag.max-chunk-chars=1500`.

## 4. Embeddings

| Setting | Value |
|---------|--------|
| Provider | Ollama (Spring AI) |
| Model | `nomic-embed-text` |
| Dimensions | 768 |

**Trade-offs:** Documented in `architecture.md` §6 (local cost/latency vs cloud quality).

## 5. Vector store

- **PGVector** in PostgreSQL (`architecture.md`).
- **Design Decision:** Filter key `ticketId` for targeted delete/replace on re-ingestion.

## 6. Re-ingestion triggers

| Event | Assessment | Action |
|-------|------------|--------|
| Ticket fields updated (PATCH) | Updated | Full re-ingest for `ticketId` |
| Comment added | Implied by “updated” | Full re-ingest |
| Status transition (incl. **closed**) | Updated or **closed** | Full re-ingest |

**Procedure:**

1. Delete existing vectors where metadata `ticketId` = X.
2. Rebuild document from current DB state.
3. Chunk, embed, insert.

**Design Decision:** Synchronous in request thread after successful DB commit.

## 7. Retrieval configuration

| Property | Default (Design Decision) | Description |
|----------|---------------------------|-------------|
| `app.rag.top-k` | `5` | Max chunks passed to LLM |
| `app.rag.similarity-threshold` | `0.65` | Min similarity; below → no-match |

Must be overridable via environment variables.

## 8. Ingestion on empty tickets

**Design Decision:** Tickets with empty description and no comments still ingest title/category only if present; otherwise skip vector write (no empty embeddings).

## 9. Consistency with ask API

Retrieved chunks must be the same store written here; citations in `rag-api-contract.md` use metadata `ticketId`.
