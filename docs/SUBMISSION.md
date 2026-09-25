# Submission notes

## PostgreSQL-gated integration tests

Several backend integration tests require a **running PostgreSQL** instance with the **pgvector** extension (same database the app uses for production-style RAG). They are annotated with JUnit `@EnabledIf` and call `com.atl.ticketmgmt.ai.support.RagIntegrationConditions#isPostgresReady`.

When PostgreSQL is **not** reachable (for example in a minimal CI job without Docker), these tests are **skipped**, not failed:

- `com.atl.ticketmgmt.ai.vector.PgVectorTicketKnowledgeVectorStoreIntegrationTest`
- `com.atl.ticketmgmt.ai.rag.RagRetrievalEvaluationIntegrationTest` (assessment-style **R1–R5** retrieval fixtures)
- `com.atl.ticketmgmt.ai.rag.TicketCreateTransitionIngestionIntegrationTest`

To execute them locally: start `docker compose up -d postgres`, set credentials if needed (`PGVECTOR_IT_*` env vars), then run `./gradlew test`.

Optional **Ollama** full-stack tests are separate: `./gradlew ollamaE2eTest` (see root `README.md`).

Default `./gradlew test` still validates tickets, state machine, and RAG logic via H2 and mocked components without PostgreSQL.
