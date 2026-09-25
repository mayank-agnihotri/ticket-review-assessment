# API Contract — Ticket Management System

**Source:** `docs/assessment.pdf`, `spec/data-model.md`, `spec/state-machine.md`  
**Base URL:** `/api`  
**Auth:** **Design Decision — not specified by assessment:** none for local assessment deployment (optional hardening later).

## 1. Error convention (all endpoints)

**Design Decision — not specified by assessment.**

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable message",
  "fieldErrors": [
    { "field": "title", "message": "must not be blank" }
  ]
}
```

| HTTP | When |
|------|------|
| 400 | Validation failure (`fieldErrors` populated) |
| 404 | Ticket/comment not found |
| 409 | Invalid status transition (`code`: `INVALID_STATUS_TRANSITION`) |
| 500 | Unexpected server error (no stack trace in body) |

## 2. Tickets

### 2.1 Create ticket

**Design Decision:** `POST /api/tickets`

**Request:**

```json
{
  "title": "Payment failed at checkout",
  "description": "Customer card declined",
  "priority": "HIGH",
  "assignee": "agent@example.com",
  "category": "payments"
}
```

| Field | Validation |
|-------|------------|
| `title` | Required, max 200 |
| `description` | Required, max 5000 |
| `priority` | Required, enum `LOW` \| `MEDIUM` \| `HIGH` \| `CRITICAL` (**Design Decision — assessment requires priority but does not define the allowed values**) |
| `assignee` | Optional, max 200 |
| `category` | Optional, max 100 (**Design Decision**) |

**Response:** `201 Created` + `TicketResponse` (status = `OPEN` per state machine spec).

### 2.2 List tickets

**Design Decision:** `GET /api/tickets`

| Query | Purpose |
|-------|---------|
| `q` | Keyword search (assessment) |
| `status` | Filter by status (assessment) |

**Response:** `200` + JSON array of `TicketResponse` (simple list; assessment does not require pagination).

**Design Decision — not specified by assessment:** Keyword search matches `title`, `description`, and `ticketId` (case-insensitive contains). **Comment bodies are not included** in list search.

**Design Decision — not specified by assessment:** Server may cap results (e.g. `app.tickets.list-max=500`) for safety; v1 returns one array without page metadata.

### 2.3 Get ticket detail

**Design Decision:** `GET /api/tickets/{ticketId}`

`ticketId` = business id (`TKT-1001`), not UUID.

**Response:** `200` + `TicketDetailResponse` (includes comments).

### 2.4 Update ticket fields

**Design Decision:** `PATCH /api/tickets/{ticketId}`

Assessment: update title, description, priority, assignee.

```json
{
  "title": "...",
  "description": "...",
  "priority": "MEDIUM",
  "assignee": "agent@example.com",
  "category": "payments",
  "resolutionNotes": "Refunded order."
}
```

All fields optional in PATCH; at least one required.

**Response:** `200` + `TicketDetailResponse`  
**Side effect:** re-ingestion (see `rag-ingestion.md`).

### 2.5 Status transition

**Design Decision:** `POST /api/tickets/{ticketId}/transitions`

```json
{ "status": "IN_PROGRESS" }
```

- Validates against `spec/state-machine.md`.
- **Response:** `200` + updated ticket, or `409` on illegal transition.

**Alternative considered:** PATCH with status only — rejected to separate field updates from lifecycle.

## 3. Comments

**Design Decision:** `POST /api/tickets/{ticketId}/comments`

```json
{ "body": "Customer confirmed retry worked." }
```

| Field | Validation |
|-------|------------|
| `body` | Required, max 5000 |

**Response:** `201` + `CommentResponse`  
**Side effect:** re-ingestion.

**Design Decision:** Comments listed in `TicketDetailResponse` only (no separate list endpoint).

## 4. DTOs

### TicketResponse

```json
{
  "ticketId": "TKT-1001",
  "title": "...",
  "description": "...",
  "priority": "HIGH",
  "assignee": "agent@example.com",
  "category": "payments",
  "status": "OPEN",
  "resolutionNotes": null,
  "createdAt": "2026-09-24T10:00:00Z",
  "updatedAt": "2026-09-24T10:00:00Z"
}
```

### TicketDetailResponse

`TicketResponse` + `"comments": [ CommentResponse ]`

### CommentResponse

```json
{
  "id": "uuid",
  "body": "...",
  "author": "system",
  "createdAt": "2026-09-24T10:05:00Z"
}
```

## 5. AI endpoint

See `spec/rag-api-contract.md` — `POST /api/ai/ask`.

## 6. Assessment traceability

| Assessment feature | API |
|--------------------|-----|
| Create / list / view / update | §2 |
| Comments | §3 |
| Keyword search | `GET /api/tickets?q=` |
| Status filter | `GET /api/tickets?status=` |
| Backend validation | §1, field validations |
| Status machine | §2.5 |
| AI ask | `rag-api-contract.md` |
