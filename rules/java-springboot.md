# Java 21 + Spring Boot Standards

Source: Ticket Management System assessment (`docs/assessment.pdf`).

## Java 21

- Target **Java 21** for compile and runtime.
- Prefer modern language features where they improve clarity (records for immutable DTOs, `Optional` at boundaries, pattern matching where appropriate)—avoid novelty for its own sake.
- Use **immutable DTOs** for API request/response types; keep JPA entities internal to persistence.
- No wildcard imports; explicit package imports only.

## Layering and packages

Organize by feature or bounded context, with clear layers:

```
.../ticket/          # domain feature
  api/               # REST controllers, request/response DTOs
  service/           # business logic, state machine, orchestration
  domain/            # entities, domain enums (e.g. TicketStatus)
  repository/        # Spring Data JPA
.../ai/              # RAG: ingestion, retrieval, ask endpoint
  api/
  service/
  ...
.../common/          # shared exceptions, validation, config
```

- **Controllers** are thin: validate input, delegate to services, map to HTTP.
- **Services** own transactions, state transitions, and orchestration (including triggering re-ingestion).
- **Repositories** contain persistence only—no business rules.
- Do not expose JPA entities from REST APIs.

## Dependency injection

- Constructor injection only (`@RequiredArgsConstructor` or explicit constructors); no field injection.
- Program to interfaces for services that have multiple implementations or external integrations (e.g. embedding clients).
- Configuration properties via `@ConfigurationProperties` or typed `@Value` on config classes—not scattered literals.

## Exception handling

- Use a **global exception handler** (`@RestControllerAdvice`) for consistent API errors (see `rules/api-standards.md`).
- Map domain failures (e.g. **invalid status transition**) to appropriate HTTP status codes; message must be meaningful for the UI.
- Do not leak stack traces or internal details in production responses.
- Fail fast on validation errors at the API boundary (`@Valid` on request bodies).

## Configuration management

- Externalize environment-specific settings (database URL, Ollama/base URLs, **top-K**, **similarity threshold**, vector store settings).
- **Never hardcode** retrieval tuning (top-K, threshold)—assessment requires these to be configurable.
- Use `application.yml` + profiles (`local`, `test`) and environment variables for secrets.
- Document all configuration keys in spec/architecture phase (`skills/documentation/standards.md`).

## Design decisions (spec phase)

- Exact package names, profile names, and config property prefixes are **design decisions**—define in `spec/` before implementation.
