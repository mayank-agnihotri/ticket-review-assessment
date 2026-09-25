# UI Flow — Ticket Management System

**Source:** `docs/assessment.pdf`, `spec/api-contract.md`, `spec/rag-api-contract.md`  
**Stack:** **Design Decision — not specified by assessment:** Next.js (App Router), calls backend REST API.

## 1. Global

- Display API errors using `message` and `fieldErrors` from error JSON (`api-contract.md`).
- Loading and empty states for lists.

## 2. Create ticket

1. Form: title, description, priority, assignee (optional), category (optional).
2. Submit → `POST /api/tickets`.
3. On success → navigate to ticket detail.
4. On 400 → show field errors inline.

## 3. List tickets

1. `GET /api/tickets` (optional `q`, `status` query params).
2. Table/cards: ticketId, title, status, priority, assignee, updatedAt.
3. Link row → detail.

## 4. View ticket

1. `GET /api/tickets/{ticketId}`.
2. Show all fields + comment thread.
3. Actions: edit fields, add comment, change status, open AI panel.

## 5. Update ticket

1. Edit form for title, description, priority, assignee, category, resolutionNotes.
2. `PATCH /api/tickets/{ticketId}`.
3. Show success toast or inline errors.

## 6. Add comment

1. Text area + submit → `POST /api/tickets/{ticketId}/comments`.
2. Append comment to thread on success.

## 7. Keyword search

1. Search input on list page.
2. `GET /api/tickets?q={keyword}` (debounced).
3. Show results or empty state.

## 8. Status filtering

1. Dropdown: All, OPEN, IN_PROGRESS, RESOLVED, CLOSED, CANCELLED.
2. `GET /api/tickets?status={value}`.

## 9. Status transition

1. Show allowed next statuses based on current status (`state-machine.md` — **Design Decision:** compute client-side for UX; server enforces).
2. `POST /api/tickets/{ticketId}/transitions` with selected status.
3. On 409 → show transition error message.

## 10. AI Q&A

1. Panel on list or detail: question input.
2. `POST /api/ai/ask` with `{ "question" }`.
3. If `noMatch` → show `answer` (“No relevant tickets found.”) distinctly (info style, not error).
4. If grounded → show `answer` + list `citedTicketIds` as links to ticket detail.
5. On 503 → service unavailable message.

## 11. Validation / API errors

| HTTP | UI behavior |
|------|-------------|
| 400 | Field-level + summary |
| 404 | “Ticket not found” |
| 409 | Status transition message |
| 500/503 | Generic retry message |

## 12. Assessment alignment

All assessment UI acceptance items map to flows §2–11 (create, list, view, update, assignee, comments, search, filter, errors, AI ask with citations/no-match).
