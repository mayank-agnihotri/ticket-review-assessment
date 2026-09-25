# Evaluation Strategy — Ticket Management System

**Source:** `docs/assessment.pdf`, related specs

## 1. Test categories

| Category | Nature | Purpose |
|----------|--------|---------|
| **Deterministic** | Pass/fail exact | State machine, validation, CRUD, persistence |
| **Probabilistic** | Threshold / fixture-based | Retrieval quality, grounding (LLM mocked or constrained) |

## 2. State machine validation (deterministic)

- Integration tests for every allowed transition in `state-machine.md` §3.
- Reject assessment examples: `CLOSED→OPEN`, `RESOLVED→OPEN`, `CANCELLED→OPEN`.
- Reject representative additional invalid pairs from `state-machine.md` §4.
- **Acceptance:** state-machine integration tests pass.

## 3. RAG / retrieval quality (probabilistic)

**Fixture dataset:** Seed tickets with known content (payment failures, shipment tracking, resolutions).

| Case ID | Question (from assessment examples) | Expected retrieval |
|---------|-------------------------------------|--------------------|
| R1 | “Have we seen payment failures before?” | Chunks with `category=payments` or payment keywords |
| R2 | “What was the resolution for ticket TKT-1001?” | Chunks from `TKT-1001` |
| R3 | “What are the common causes of shipment tracking issues?” | Shipment-related tickets |
| R4 | “Show me similar resolved tickets.” | `status=RESOLVED` in metadata |
| R5 | “Which high-priority tickets are related to payment?” | `priority=HIGH` + payment content |

**Metrics (Design Decision):**

- **Retrieval recall@K:** expected `ticketId` appears in top-K metadata for fixture questions.
- Tune `app.rag.top-k` and `app.rag.similarity-threshold` using this suite.

**LLM:** Mock `ChatModel` in automated CI; evaluate retrieval separately from generation quality.

## 4. Grounding and citation validation

| Check | Method |
|-------|--------|
| Citations ⊆ retrieved ticket IDs | Assert on `citedTicketIds` vs retrieval mock capture |
| No citation when `noMatch=true` | API test |
| Answer does not assert facts absent from chunks | Manual + `commands/review-rag-output.md` on samples |

## 5. No-match testing (deterministic)

| Scenario | Expected |
|----------|----------|
| Question unrelated to any ticket | `noMatch: true`, message contains “No relevant tickets found” |
| All similarities below threshold | Same (no LLM call per `rag-api-contract.md`) |
| Empty vector store | Same |

## 6. Re-ingestion validation

| Action | Expected |
|--------|----------|
| PATCH ticket description | Old vectors for `ticketId` replaced; new content searchable |
| Add comment | Updated chunks include comment text |
| Transition to `CLOSED` | Metadata `status` updated in vectors |

## 7. Configurable parameters

- Tests with `app.rag.top-k=1` vs `5` change chunk count passed to LLM (mock verification).
- Tests with threshold `0.99` force no-match on fixture set.

## 8. Meaningful AI mistake documentation (assessment)

**Process:**

1. During development, log mistakes in `docs/prompt-history.md` or linked doc entry.
2. Include: prompt, incorrect AI output, correction, root cause (hallucination / wrong API / bad test).
3. At least **one** entry required for acceptance.

**Examples of qualifying mistakes:**

- Ungrounded RAG answer accepted without citation check.
- Wrong state transition suggested by AI.
- Generated test that does not match assessment.

## 9. Distinction summary

| Deterministic | Probabilistic |
|---------------|---------------|
| State machine, validation, HTTP codes, no-match flag, citation ⊆ retrieval set | Similarity scores, answer phrasing, LLM wording |

Probabilistic cases use **fixed fixtures** and **thresholds** to keep CI stable.
