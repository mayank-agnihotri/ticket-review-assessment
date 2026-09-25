# Command: Review RAG Answers for Hallucination and Grounding

Use when changing ingestion, retrieval, prompts, or `/api/ai/ask`.

## Prompt template

```
Review RAG behavior for the Ticket Management System against docs/assessment.pdf and spec/rag-api-contract.md.

Evaluate sample questions (include assessment examples):
- Payment failures history
- Resolution for a specific ticket ID
- Similar resolved tickets
- High-priority payment-related tickets

For each test case provide:
1. Retrieved chunks (ticketId metadata)
2. top-K and similarity threshold used
3. Model answer
4. Cited ticket IDs

Verify:
- Answer content is supported by retrieved text only (no general-knowledge filler for support-specific questions)
- Every cited ticketId was in retrieval results
- Empty/low-similarity retrieval yields explicit "no relevant tickets found" (or spec-defined equivalent)—not a fabricated answer
- Single retrieval→generate only (no tool calls, ticket creation, notifications)

Flag:
- Hallucinated facts not in chunks
- Missing citations when answer uses ticket data
- Wrong ticket IDs in citations
- Hardcoded top-K or threshold

Output: pass/fail table + remediation steps.
```

## Link

Document any caught hallucination toward the assessment “meaningful AI mistake” requirement.
