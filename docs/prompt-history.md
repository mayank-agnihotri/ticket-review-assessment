# Prompt history

Significant AI-assisted development notes for the Ticket Management System.

## 2026-09-24 — RAG E2E grounded ask treated as CI failure

**Phase:** Testing / RAG review

**Prompt summary:** Add full-stack RAG E2E tests with real Ollama and assert `grounded=true` on `/api/ai/ask` when retrieval succeeded.

**Outcome:** Retrieval passed but the chat model sometimes returned the exact no-match phrase (`No relevant tickets found.`), causing flaky failures. The test used `Assumptions.assumeFalse` to skip, which hid real regressions and did not match the evaluation strategy (mock LLM for generation; test retrieval separately).

**AI mistakes caught:** Treating probabilistic LLM wording as a deterministic CI gate; conflating retrieval success with generation success in one assertion.

**Correction:** Default CI uses deterministic embeddings and mocked `ChatModel` for ask/citation tests; Ollama E2E lives under the `ollama-e2e` JUnit tag (`./gradlew ollamaE2eTest`). E2E ask tests assert retrieval and HTTP shape only, not `grounded=true` from the live model.
