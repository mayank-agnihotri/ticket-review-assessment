# SpecStory / Cursor prompt history

## Limitation in this repository

The assessment and `rules/prompt-history.md` expect automatic capture under `.specstory/history/` (SpecStory extension for Cursor/VS Code).

**This Cloud Agent environment does not have access to export the full Cursor/SpecStory session transcript.** No complete per-prompt SpecStory export was available when these files were added.

## What is included instead

The only **documented** development prompt entry in this project is maintained in:

- **`docs/prompt-history.md`** (source of truth)

A matching summary for graders is mirrored here (not expanded or fabricated):

- [`2026-09-24-rag-e2e-grounded-ask.md`](./2026-09-24-rag-e2e-grounded-ask.md)

**Full Cloud Agent transcript** (user prompts + Cursor text responses, 58 turns) exported from [agent bc-8fca06df](https://cursor.com/agents/bc-8fca06df-7547-4d23-9220-1df93afd61cf):

- [`docs/agent-transcript-bc-8fca06df.md`](../docs/agent-transcript-bc-8fca06df.md) (canonical path)
- [`2026-09-25-cloud-agent-bc-8fca06df-transcript.md`](./2026-09-25-cloud-agent-bc-8fca06df-transcript.md) (mirror)

Tool-call payloads and assistant “thinking” blocks are omitted to keep the file readable.

If you need native SpecStory history from Desktop sessions, open the project in **Cursor Desktop** with the SpecStory extension enabled and export or sync `.specstory/history/` from that workspace.
