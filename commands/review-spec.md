# Command: Review Specifications for Consistency

Use after drafting or updating `spec/`, before implementation.

## Prompt template

```
Review Ticket Management System specifications for internal consistency and alignment with docs/assessment.pdf.

Read all files under spec/ and compare to assessment acceptance criteria.

Check:
1. No contradictions between data-model, api-contract, state-machine, rag-ingestion, rag-api-contract, ui-flow, test-strategy
2. State machine matches assessment: OPEN→IN_PROGRESS→RESOLVED→CLOSED; OPEN→CANCELLED; IN_PROGRESS→CANCELLED; invalid examples documented
3. RAG: metadata fields (ticketId, status, priority, assignee, category); re-ingest on update/close; configurable top-K/threshold; single retrieval→generate
4. POST /api/ai/ask request documented; response includes citations and no-match behavior
5. Items NOT SPECIFIED in assessment are labeled design decisions, not presented as assessment facts
6. Chunking and embedding choices marked for architecture.md justification

Output:
- Inconsistencies (file + section)
- Missing acceptance criteria coverage
- Ambiguities to resolve before coding
```

## Gate

Do not start implementation until critical inconsistencies are resolved.
