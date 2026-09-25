# Command: Review Code Against Specification

Use after implementation, before merge. Source: assessment SDD **Review** phase.

## Prompt template

```
Review the current code changes against the Ticket Management System specifications.

Read:
- docs/assessment.pdf (acceptance criteria)
- spec/requirements.md
- spec/api-contract.md
- spec/state-machine.md
- spec/rag-ingestion.md
- spec/rag-api-contract.md
- rules/java-springboot.md
- rules/api-standards.md
- rules/rag-vector-store.md
- rules/code-review.md

Diff: [branch changes | uncommitted changes]

Verify:
1. Ticket CRUD, comments, search, filter, validation, UI-error contract
2. State machine: allowed transitions only; invalid examples rejected
3. POST /api/ai/ask: grounding, citations, no-match behavior
4. Re-ingestion on ticket update/close
5. Configurable top-K and similarity threshold (not hardcoded)
6. No secrets, no agent side effects from /api/ai/ask
7. Tests align with spec/test-strategy.md

Output:
- Pass/fail per area
- File:line issues
- Spec section violated (if any)
- Suggested fixes (no drive-by refactors)
```

## Notes

- Do not accept AI review as sole approval—human verifies.
- Flag any requirement not traceable to assessment or spec.
