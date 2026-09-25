# Security Guidelines

Source: Ticket Management System assessment (`docs/assessment.pdf`).

## Secrets

- **No secrets committed** (assessment acceptance criterion).
- Use environment variables, Spring profiles, or external secret stores—never credentials in source or specs checked into git.
- `.env` and local overrides belong in `.gitignore` (already project policy).

## Configuration

- Database URLs, API keys (if any cloud embedding/LLM), and mail credentials via configuration only.
- Review **AI-generated code** for hardcoded passwords, tokens, or permissive CORS—use `commands/review-security.md`.

## Generated code review

- Treat Copilot/Cursor output as untrusted until reviewed.
- Authentication/authorization for ticket APIs is **NOT SPECIFIED** in the assessment—if added, document threat model in `spec/architecture.md` (**design decision**).

## Dependencies

- Prefer well-maintained libraries; pin versions in Gradle lockfiles when introduced.

## Data

- Do not log full ticket PII or embedding vectors at INFO in production without need.
