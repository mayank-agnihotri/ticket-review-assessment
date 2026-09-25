# Code Review Expectations

Source: Ticket Management System assessment + project steering rules.

## Checklist

1. **Correctness** — Implements `spec/` and assessment acceptance criteria; state machine matches `spec/state-machine.md`.
2. **Maintainability** — Clear layering (`rules/java-springboot.md`); no god classes; sensible naming.
3. **Validation** — Backend validation on all inputs; invalid transitions rejected.
4. **Error handling** — Consistent API errors; UI can show meaningful messages.
5. **Security** — No secrets; no unsafe defaults (`rules/security.md`).
6. **Tests** — State-machine **integration tests**; API and RAG tests per `rules/testing.md`.
7. **Spec compliance** — No scope creep (e.g. agent tools, notifications from `/api/ai/ask`).
8. **RAG** — Configurable top-K/threshold; re-ingestion on update/close; citations and no-match behavior.
9. **Documentation** — `architecture.md` updated when chunking, embedding, or retrieval changes.

## Process

- Review after implementation phase, before merge; loop **Review → Fix** per SDD workflow.
- Use `commands/review-code.md` for AI-assisted review; human owns the final approve.
