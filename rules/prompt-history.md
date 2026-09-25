# Prompt History

Source: Ticket Management System assessment (`docs/assessment.pdf`).

## Locations

| Location | Purpose |
|----------|---------|
| `.specstory/history/` | Automatic capture (SpecStory extension for Cursor/VS Code) |
| `docs/prompt-history.md` | Human-readable log or index of significant prompts and outcomes |

## Requirement

**Every Cursor prompt** used during development must be captured (assessment).

## `docs/prompt-history.md` format (suggested)

```markdown
## YYYY-MM-DD — Short title
**Phase:** Specification | Implementation | Testing | Review
**Prompt summary:** ...
**Outcome:** ...
**AI mistakes caught:** (if any)
```

## Notes

- Do not paste secrets into prompt history.
- Link prompts to spec changes or PRs when relevant.
- AI mistakes documented here satisfy the assessment “meaningful mistake” criterion when described clearly.
