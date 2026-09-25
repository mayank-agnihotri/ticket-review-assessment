# Command: Generate Tests From Specification

Use during **Testing** phase. Generated tests require human review (`rules/ai-output-validation.md`).

## Prompt template

```
Generate tests for the Ticket Management System from specifications only.

Read:
- spec/test-strategy.md
- spec/state-machine.md
- spec/api-contract.md
- spec/rag-api-contract.md
- spec/evaluation-strategy.md
- rules/testing.md
- docs/assessment.pdf (Core Acceptance Criteria)

Produce:
1. State-machine integration tests: all allowed transitions; invalid transitions including CLOSED→OPEN, RESOLVED→OPEN, CANCELLED→OPEN
2. API tests: validation errors, meaningful error bodies, search, status filter
3. POST /api/ai/ask: grounded response with ticket ID citations; no-match returns honest message (mock retrieval/LLM as appropriate)
4. Re-ingestion tests per spec/rag-ingestion.md

Constraints:
- Do not invent requirements beyond spec + assessment
- Use design decisions from spec for DB (PostgreSQL/H2), testcontainers, mocks
- Follow package and naming conventions in rules/testing.md

Output test class list with brief rationale; then implement only after human approval of the plan.
```

## After generation

Run `commands/review-code.md` focused on test correctness and coverage gaps.
