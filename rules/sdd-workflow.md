# Spec-Driven Development (SDD) Workflow

Source: Ticket Management System assessment (`docs/assessment.pdf`).

## Required workflow

```
Requirement → Specification → Plan / Tasks → Implementation → Testing → Review → Fix
```

- **Specifications before implementation**—no coding against guessed requirements.
- **Do not** start with a one-shot prompt such as “Build the complete application.”
- The assessment prioritizes **how** the system is built with AI (specs, validation, grounding)—not only the running app.

## Specification artefacts (create before implementation)

Example structure from assessment (exact files may vary):

```
spec/
├── requirements.md
├── architecture.md
├── data-model.md
├── api-contract.md
├── state-machine.md
├── rag-ingestion.md
├── rag-api-contract.md
├── evaluation-strategy.md
├── ui-flow.md
└── test-strategy.md
```

## Steering files

Maintain reusable AI instructions under `rules/`, `skills/`, and `commands/` (this repo).

## AI mistake documentation

- Identify at least **one meaningful AI mistake** (wrong code or ungrounded/hallucinated answer) during development and **document** it (assessment acceptance criterion).
- Location/format: **design decision**—reference in `docs/prompt-history.md` or project docs.

## Token optimisation (advisory)

Assessment suggests Graphify, Caveman, Codebase-memory MCP, and prompt caching for static system instructions—optional, not acceptance criteria.

## Commands

Use `commands/review-spec.md`, `commands/review-code.md`, `commands/generate-tests.md`, and `commands/review-rag-output.md` at each phase gate.
