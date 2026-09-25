# AI Output Validation

Source: Ticket Management System assessment (`docs/assessment.pdf`).

## Principle

Use AI as an **engineering assistant**, not an authority. Wrong code and **ungrounded or hallucinated** assistant answers must be caught and corrected.

## What to review

| Output | Review for |
|--------|------------|
| Generated code | Correctness, requirement/spec match, validation, state machine, security |
| Specifications | Internal consistency with `docs/assessment.pdf` and prior specs |
| Generated tests | Coverage of acceptance criteria; false positives; mocked vs real boundaries |
| RAG answers | Grounding in retrieved tickets only; **citations** match retrieved IDs; **no-match** when context empty |
| Citations | Every cited `ticketId` appeared in retrieval results for that request |

## Repeatable commands

- `commands/review-code.md` — code vs spec
- `commands/review-spec.md` — spec consistency
- `commands/generate-tests.md` — tests from spec (then human review)
- `commands/review-rag-output.md` — hallucination and grounding
- `commands/review-security.md` — secrets and unsafe patterns

## Documentation requirement

- Record at least **one meaningful AI mistake** (code or RAG) caught during development (assessment acceptance criterion).

## RAG-specific

- Reject answers that use general world knowledge for support-specific questions when retrieval is empty or below threshold.
- Verify **single retrieval → generate**—no hidden agent loops or side effects.
