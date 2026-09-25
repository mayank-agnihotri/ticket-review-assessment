# REST API Standards

Source: Ticket Management System assessment (`docs/assessment.pdf`).

## Resource-oriented design

- Model **tickets** as the primary resource; **comments** nested or sub-resource under tickets (exact paths: **design decision** in `spec/api-contract.md`).
- Use nouns and plural collections (`/api/tickets`, `/api/tickets/{id}/comments`) unless assessment defines otherwise.
- AI Q&A is a separate action endpoint: **`POST /api/ai/ask`** (assessment-defined).

## HTTP methods and status codes

| Situation | Status |
|-----------|--------|
| Success create | `201 Created` + `Location` where applicable |
| Success read/update | `200 OK` |
| Success delete (if spec defines delete) | `204 No Content` |
| Validation failure | `400 Bad Request` |
| Invalid state transition | `409 Conflict` or `422 Unprocessable Entity` (**design decision** in spec) |
| Not found | `404 Not Found` |
| Server error | `500` (logged; no stack trace in body) |

## DTOs

- Request/response types are **DTOs only**—never entities.
- Use Jakarta Validation (`@NotBlank`, `@NotNull`, etc.) on request DTOs; assessment requires **backend validation**.
- Response DTOs include fields needed by UI (title, description, priority, assignee, status, comments, etc.—per `spec/data-model.md`).

## `POST /api/ai/ask`

Assessment request body:

```json
{ "question": "..." }
```

- Response contract (answer text, **cited ticket ID(s)**, no-match indicator) is a **design decision** in `spec/rag-api-contract.md`.
- No-match / out-of-scope: return an honest **“no relevant tickets found”** (assessment)—exact JSON shape in spec.

## Validation

- Validate at controller (`@Valid`) and enforce business rules in services (e.g. state machine).
- Return **consistent error bodies** the UI can display (assessment: **meaningful errors in UI**).

## Error response shape

Use a single project-wide structure (example—finalize in spec):

```json
{
  "code": "INVALID_STATUS_TRANSITION",
  "message": "Human-readable message",
  "fieldErrors": [ { "field": "status", "message": "..." } ]
}
```

## API documentation

- Document all endpoints in `spec/api-contract.md` before implementation.
- Keep OpenAPI/Springdoc generation aligned with spec (**design decision**: tooling choice in spec).

## Out of scope (assessment)

- No autonomous agent APIs (create ticket, notifications, tool chaining from `/api/ai/ask`).
