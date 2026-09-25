"use client";

import Link from "next/link";
import { useState } from "react";
import { ApiError, askAi } from "@/lib/api";
import type { AiAskResponse } from "@/lib/types";
import { ErrorAlert } from "./ErrorAlert";

export function AskPanel() {
  const [question, setQuestion] = useState("");
  const [loading, setLoading] = useState(false);
  const [response, setResponse] = useState<AiAskResponse | null>(null);
  const [error, setError] = useState<unknown>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const trimmed = question.trim();
    if (!trimmed) {
      return;
    }
    setLoading(true);
    setError(null);
    setResponse(null);
    try {
      const result = await askAi(trimmed);
      setResponse(result);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }

  const isServiceUnavailable =
    error instanceof ApiError && error.status === 503;

  return (
    <section className="panel ask-panel">
      <h2>AI Q&amp;A</h2>
      <p className="muted">
        Ask about ticket history. Answers use retrieved ticket excerpts only.
      </p>
      <form onSubmit={handleSubmit} className="stack">
        <label htmlFor="ai-question">Question</label>
        <textarea
          id="ai-question"
          rows={3}
          maxLength={2000}
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="e.g. Have we seen payment failures before?"
        />
        <button type="submit" disabled={loading || !question.trim()}>
          {loading ? "Asking…" : "Ask"}
        </button>
      </form>

      {isServiceUnavailable && (
        <div className="alert alert-warn" role="alert">
          <strong>AI service unavailable</strong>
          <p>
            The assistant could not reach the AI or vector store. Try again
            later.
          </p>
          {error instanceof ApiError && <p className="muted">{error.message}</p>}
        </div>
      )}

      {!isServiceUnavailable && <ErrorAlert error={error} title="Ask failed" />}

      {response && (
        <div className="ask-result">
          {response.noMatch ? (
            <div className="alert alert-info" role="status">
              <p>{response.answer}</p>
            </div>
          ) : (
            <div className="answer-block">
              <p>{response.answer}</p>
              {response.citedTicketIds.length > 0 && (
                <div>
                  <h3>Cited tickets</h3>
                  <ul className="citation-list">
                    {response.citedTicketIds.map((id) => (
                      <li key={id}>
                        <Link href={`/tickets/${id}`}>{id}</Link>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}
          <details className="retrieval-details">
            <summary>Retrieval details</summary>
            <ul>
              <li>Top K: {response.retrieval.topK}</li>
              <li>
                Similarity threshold: {response.retrieval.similarityThreshold}
              </li>
              <li>Chunks used: {response.retrieval.chunksUsed}</li>
              <li>Grounded: {response.grounded ? "yes" : "no"}</li>
            </ul>
          </details>
        </div>
      )}
    </section>
  );
}
