# RAG evaluation: deterministic vs Ollama E2E

## Deterministic (default CI)

- **Command:** `./gradlew test` (excludes the `ollama-e2e` tag)
- **Embeddings:** `DeterministicEmbeddingModel` (768 dimensions) in PGVector integration tests — no Ollama
- **Retrieval:** Assessment-style **R1–R5** recall checks in `RagRetrievalEvaluationIntegrationTest`
- **Ask / citations:** `RagServiceTest`, `RagAskCitationWebTest`, and `AskApiTest` with mocked retrieval and/or `ChatModel`
- **Database:** PostgreSQL + PGVector tests use `@EnabledIf` `RagIntegrationConditions#isPostgresReady` and are skipped when Postgres is unreachable

Use this path for stable pass/fail on ingestion, metadata, top-K, threshold wiring, no-match contract, and citation ⊆ retrieval.

## Ollama E2E (optional / manual CI job)

- **Command:** `./gradlew ollamaE2eTest`
- **Requires:** Reachable Postgres, PGVector, Ollama with `nomic-embed-text` and `llama3.2` (see `RagIntegrationConditions`)
- **Scope:** Real embeddings and chat against `RagEndToEndIntegrationTest` — validates the full stack, not exact answer wording or `grounded` flags from the live LLM

Do not use Ollama E2E as the only gate for grounded answers; generation quality belongs in mocked or manual evaluation per `spec/evaluation-strategy.md`.
