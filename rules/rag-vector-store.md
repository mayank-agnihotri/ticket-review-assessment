# RAG & Vector Store Guidelines

Source: Ticket Management System assessment (`docs/assessment.pdf`).

## Pipeline (assessment flow)

Support Tickets → knowledge documents → **chunk** → **embeddings** → **vector store** → user question → **similarity search** → relevant tickets → **LLM + context** → **grounded answer** + **ticket sources**.

**Single retrieval → generate** only—not an autonomous agent (no ticket creation, notifications, or tool chaining from the assistant).

## Knowledge documents

Convert ticket **description**, **comments**, and **resolution notes** into searchable documents.

**Required metadata** (assessment):

- `ticketId`
- `status`
- `priority`
- `assignee`
- `category`

Field sources and `category` on the ticket model are **design decisions** in `spec/data-model.md` and `spec/rag-ingestion.md`.

## Chunking

- Chunking strategy (paragraph-based vs fixed-size vs semantic for ticket text) must be **explicitly documented** and justified in **`architecture.md`** (assessment).
- Do not change chunking silently—update architecture doc and re-evaluate retrieval when strategy changes.

## Embeddings

- Use an **embedding model** via Spring AI (assessment stack).
- Document model choice (e.g. local Ollama vs cloud) and **cost/latency/quality** tradeoffs in **`architecture.md`** (assessment).
- Embedding dimensions must match vector store configuration (**design decision** in spec).

## Vector store

- Assessment allows **PGVector or Chroma**—choice is a **design decision** in `spec/architecture.md`.
- Ticket data must be converted to embeddings and stored (acceptance criterion).

## Retrieval configuration

- **Top-K** and **similarity threshold** must be **configurable, not hardcoded** (assessment).
- Property names and defaults: **design decision** in `spec/rag-ingestion.md` / `application` config docs.

## Re-ingestion

- **Refresh embeddings when a ticket is updated or closed**—do not let the knowledge base go stale (assessment).
- Acceptance criterion also requires re-ingestion on **update**—implement hooks after ticket/comment changes per `spec/rag-ingestion.md`.

## Grounding and guardrails

- Answer **only from retrieved ticket context** for support-specific questions—**no general LLM knowledge fallback** (assessment).
- If no relevant tickets: say so **explicitly**—do not fabricate plausible answers.
- **Cite specific ticket ID(s)** used to generate the answer (assessment).

## Retrieval evaluation

- Define cases in `spec/evaluation-strategy.md` using assessment example questions (payment failures, resolution for a ticket, similar resolved tickets, high-priority payment-related, etc.).
- Measure retrieval hit rate on fixtures before tuning top-K/threshold.

## API

- Implement **`POST /api/ai/ask`** with body `{ "question": "..." }` (assessment).
