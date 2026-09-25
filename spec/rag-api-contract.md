# RAG API Contract — Ticket Management System

**Source:** `docs/assessment.pdf`

## 1. Endpoint (assessment)

`POST /api/ai/ask`

## 2. Request (assessment)

```json
{
  "question": "What caused previous payment failures?"
}
```

| Field | Validation |
|-------|------------|
| `question` | Required, non-blank, max 2000 chars (**Design Decision** max length) |

## 3. Response — grounded match

**Design Decision — not specified by assessment:** HTTP `200` and response JSON structure (fields below). Assessment requires grounded answer and ticket ID citation(s).

```json
{
  "answer": "Based on ticket history, payment failures were often caused by ...",
  "citedTicketIds": ["TKT-1001", "TKT-1004"],
  "grounded": true,
  "noMatch": false,
  "retrieval": {
    "topK": 5,
    "similarityThreshold": 0.65,
    "chunksUsed": 3
  }
}
```

| Field | Purpose |
|-------|---------|
| `answer` | LLM text constrained to retrieved context |
| `citedTicketIds` | **Assessment:** cite specific ticket ID(s) used |
| `grounded` | `true` when answer produced from retrieval |
| `noMatch` | `false` when matches found |
| `retrieval` | Transparency for evaluation (**Design Decision**) |

## 4. Response — no relevant tickets

**Assessment:** honest **“no relevant tickets found”**, not fabricated answer (anti-fabrication).

**Design Decision — not specified by assessment:** HTTP status, JSON shape, and flags below.

```json
{
  "answer": "No relevant tickets found.",
  "citedTicketIds": [],
  "grounded": false,
  "noMatch": true,
  "retrieval": {
    "topK": 5,
    "similarityThreshold": 0.65,
    "chunksUsed": 0
  }
}
```

- **HTTP `200`** with `noMatch: true` (not 404) so UI can distinguish empty retrieval from bad route.
- **`answer`** must be exactly: `No relevant tickets found.` (assessment-aligned wording).
- **`citedTicketIds`** empty array.
- **Do not call the LLM** when retrieval produces no chunks ≥ similarity threshold (**Design Decision**, supports assessment anti-fabrication).

## 5. Behavior rules

1. **Single retrieval → generate:** one similarity search + at most one LLM call per request.
2. **Support-specific questions** with insufficient evidence (no chunks ≥ threshold): return §4; **do not** call LLM with empty context (**Design Decision**).
3. **No general-knowledge fallback** for support-specific questions when retrieval is empty or weak.
4. **Citations:** `citedTicketIds` must be subset of `ticketId` metadata from chunks actually passed to the LLM; deduplicated.
5. **Out of scope:** create tickets, send notifications, multi-step agent plans.

## 6. Prompt guardrails (implementation hint)

System instruction (summary): answer only using provided ticket excerpts; if excerpts do not support an answer, respond with the no-match phrase.

**Design Decision:** Exact prompt text lives in implementation, not this contract.

## 7. Errors

| Condition | HTTP |
|-----------|------|
| Invalid request body | 400 |
| Ollama/vector store unavailable | 503 with `code`: `AI_SERVICE_UNAVAILABLE` |

## 8. Assessment traceability

| Requirement | Section |
|-------------|---------|
| POST /api/ai/ask | §1 |
| Request shape | §2 |
| Grounded answer + citations | §3 |
| No-match | §4 |
| No agent | §5 |
