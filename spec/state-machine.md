# State Machine — Ticket Management System

**Source:** `docs/assessment.pdf`

## 1. States

| State | Assessment |
|-------|------------|
| `OPEN` | Explicit in transitions |
| `IN_PROGRESS` | Explicit |
| `RESOLVED` | Explicit |
| `CLOSED` | Explicit |
| `CANCELLED` | Explicit |

## 2. Initial status

**Design Decision — not specified by assessment:** New tickets start in **`OPEN`**.

## 3. Allowed transitions

| From | To |
|------|-----|
| `OPEN` | `IN_PROGRESS` |
| `IN_PROGRESS` | `RESOLVED` |
| `RESOLVED` | `CLOSED` |
| `OPEN` | `CANCELLED` |
| `IN_PROGRESS` | `CANCELLED` |

## 4. Invalid transitions (assessment examples + closure)

**Must be rejected by backend** (`409 INVALID_STATUS_TRANSITION`):

| From | To | Assessment |
|------|-----|------------|
| `CLOSED` | `OPEN` | Explicit example ❌ |
| `RESOLVED` | `OPEN` | Explicit example ❌ |
| `CANCELLED` | `OPEN` | Explicit example ❌ |

**Design Decision — not specified by assessment:** All other transitions not listed in §3 are **invalid**, including but not limited to:

| From | To |
|------|-----|
| `CLOSED` | `IN_PROGRESS`, `RESOLVED`, `CANCELLED` |
| `RESOLVED` | `IN_PROGRESS`, `CANCELLED` |
| `CANCELLED` | any except none |
| `OPEN` | `RESOLVED`, `CLOSED` *(must go via `IN_PROGRESS`)* |
| `IN_PROGRESS` | `OPEN`, `CLOSED` |

## 5. Backend enforcement

1. Transition only via `POST /api/tickets/{ticketId}/transitions` (not via generic PATCH of status **Design Decision**).
2. Service method `transition(ticketId, targetStatus)`:
   - Load ticket; if not found → 404.
   - If `(current, target)` not in allowed set → 409 with clear message.
   - Persist new status; update `updatedAt`; trigger **re-ingestion**.
3. Terminal states: `CLOSED`, `CANCELLED` — no outbound transitions.

## 6. Testing reference

- Integration tests must cover all §3 transitions and assessment invalid examples (`test-strategy.md`).
