# Documentation Standards

Source: Ticket Management System assessment (`docs/assessment.pdf`).

## Goals

- Concise, accurate technical docs for an experienced Java/Spring team.
- Documentation stays **synchronized with implementation**—update specs and architecture when behavior changes.

## Required documentation (assessment)

| Document | Content |
|----------|---------|
| `spec/architecture.md` | System structure, **chunking strategy** (justified), **embedding model choice** and tradeoffs (Ollama vs cloud), vector store choice, retrieval defaults |
| `spec/*.md` | Contracts and behavior before code (see `rules/sdd-workflow.md`) |
| `docs/prompt-history.md` | Prompt log (see `rules/prompt-history.md`) |
| AI mistake record | At least one caught AI error during development |

## Style

- Prefer short sections, tables, and diagrams over prose walls.
- State **assumptions** and **trade-offs** explicitly (e.g. why PGVector vs Chroma, why a chunk size).
- Mark undecided items as **design decision (spec phase)**—do not invent values in steering docs.

## Configuration documentation

- List every environment variable and `application.yml` property affecting DB, Ollama/Spring AI, **top-K**, **similarity threshold**, and vector store.
- Document local run prerequisites (Docker Postgres/pgvector, Ollama)—without committing secrets.

## When to update

- API change → `spec/api-contract.md` + OpenAPI if used.
- State machine change → `spec/state-machine.md` + integration tests.
- RAG change → `spec/rag-ingestion.md`, `architecture.md`, `evaluation-strategy.md`.

## Out of scope

- User-facing marketing docs unless required by `spec/ui-flow.md`.
