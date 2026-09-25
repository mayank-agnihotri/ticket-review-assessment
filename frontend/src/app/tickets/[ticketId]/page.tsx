"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { AskPanel } from "@/components/AskPanel";
import { ErrorAlert } from "@/components/ErrorAlert";
import { FieldError } from "@/components/FieldError";
import {
  ApiError,
  addComment,
  getTicket,
  transitionTicket,
  updateTicket,
} from "@/lib/api";
import { formatDateTime } from "@/lib/format";
import { allowedTransitions } from "@/lib/stateMachine";
import type { Priority, TicketDetail, TicketStatus } from "@/lib/types";

const PRIORITIES: Priority[] = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];

export default function TicketDetailPage() {
  const params = useParams();
  const ticketId = decodeURIComponent(params.ticketId as string);

  const [ticket, setTicket] = useState<TicketDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<unknown>(null);

  const [editing, setEditing] = useState(false);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<Priority>("MEDIUM");
  const [assignee, setAssignee] = useState("");
  const [saveError, setSaveError] = useState<unknown>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [saveSuccess, setSaveSuccess] = useState(false);

  const [commentBody, setCommentBody] = useState("");
  const [commentError, setCommentError] = useState<unknown>(null);
  const [commentSubmitting, setCommentSubmitting] = useState(false);

  const [transitionError, setTransitionError] = useState<unknown>(null);
  const [transitioning, setTransitioning] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const data = await getTicket(ticketId);
      setTicket(data);
      setTitle(data.title);
      setDescription(data.description);
      setPriority(data.priority);
      setAssignee(data.assignee ?? "");
    } catch (err) {
      setLoadError(err);
      setTicket(null);
    } finally {
      setLoading(false);
    }
  }, [ticketId]);

  useEffect(() => {
    load();
  }, [load]);

  function mapFieldErrors(err: ApiError): Record<string, string> {
    const map: Record<string, string> = {};
    for (const fe of err.fieldErrors) {
      map[fe.field] = fe.message;
    }
    return map;
  }

  async function handleSave(e: React.FormEvent) {
    e.preventDefault();
    if (!ticket) {
      return;
    }
    setSaveError(null);
    setFieldErrors({});
    setSaveSuccess(false);
    const patch: Record<string, string | undefined> = {};
    if (title !== ticket.title) {
      patch.title = title;
    }
    if (description !== ticket.description) {
      patch.description = description;
    }
    if (priority !== ticket.priority) {
      patch.priority = priority;
    }
    const assigneeValue = assignee.trim();
    const currentAssignee = ticket.assignee ?? "";
    if (assigneeValue !== currentAssignee) {
      patch.assignee = assigneeValue;
    }
    if (Object.keys(patch).length === 0) {
      setSaveError(new Error("Change at least one field before saving."));
      return;
    }
    try {
      const updated = await updateTicket(ticketId, patch);
      setTicket(updated);
      setEditing(false);
      setSaveSuccess(true);
    } catch (err) {
      setSaveError(err);
      if (err instanceof ApiError) {
        setFieldErrors(mapFieldErrors(err));
      }
    }
  }

  async function handleComment(e: React.FormEvent) {
    e.preventDefault();
    const body = commentBody.trim();
    if (!body) {
      return;
    }
    setCommentSubmitting(true);
    setCommentError(null);
    try {
      await addComment(ticketId, body);
      setCommentBody("");
      await load();
    } catch (err) {
      setCommentError(err);
    } finally {
      setCommentSubmitting(false);
    }
  }

  async function handleTransition(target: TicketStatus) {
    setTransitioning(true);
    setTransitionError(null);
    try {
      const updated = await transitionTicket(ticketId, target);
      setTicket(updated);
    } catch (err) {
      setTransitionError(err);
    } finally {
      setTransitioning(false);
    }
  }

  if (loading) {
    return <p className="muted">Loading ticket…</p>;
  }

  if (loadError instanceof ApiError && loadError.status === 404) {
    return (
      <div>
        <p><Link href="/tickets">← Back to list</Link></p>
        <div className="alert alert-error" role="alert">
          <p>Ticket not found.</p>
        </div>
      </div>
    );
  }

  if (!ticket) {
    return <ErrorAlert error={loadError} title="Could not load ticket" />;
  }

  const nextStatuses = allowedTransitions(ticket.status);

  return (
    <div>
      <p><Link href="/tickets">← Back to list</Link></p>
      <div className="row" style={{ justifyContent: "space-between" }}>
        <h1>{ticket.ticketId}</h1>
        <span className="badge">{ticket.status}</span>
      </div>

      {saveSuccess && (
        <div className="alert alert-success" role="status">
          Ticket updated.
        </div>
      )}

      <section className="panel">
        {!editing ? (
          <>
            <h2>{ticket.title}</h2>
            <p className="muted">
              Priority: {ticket.priority}
              {ticket.assignee ? ` · Assignee: ${ticket.assignee}` : ""}
              {ticket.category ? ` · Category: ${ticket.category}` : ""}
            </p>
            <p>{ticket.description}</p>
            {ticket.resolutionNotes && (
              <p><strong>Resolution notes:</strong> {ticket.resolutionNotes}</p>
            )}
            <p className="muted">
              Created {formatDateTime(ticket.createdAt)} · Updated{" "}
              {formatDateTime(ticket.updatedAt)}
            </p>
            <button type="button" className="secondary" onClick={() => setEditing(true)}>
              Edit fields
            </button>
          </>
        ) : (
          <form className="stack" onSubmit={handleSave}>
            <h2>Edit ticket</h2>
            <ErrorAlert error={saveError} title="Update failed" />
            <div>
              <label htmlFor="edit-title">Title</label>
              <input
                id="edit-title"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                maxLength={200}
              />
              <FieldError message={fieldErrors.title} />
            </div>
            <div>
              <label htmlFor="edit-description">Description</label>
              <textarea
                id="edit-description"
                rows={5}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                maxLength={5000}
              />
              <FieldError message={fieldErrors.description} />
            </div>
            <div>
              <label htmlFor="edit-priority">Priority</label>
              <select
                id="edit-priority"
                value={priority}
                onChange={(e) => setPriority(e.target.value as Priority)}
              >
                {PRIORITIES.map((p) => (
                  <option key={p} value={p}>{p}</option>
                ))}
              </select>
              <FieldError message={fieldErrors.priority} />
            </div>
            <div>
              <label htmlFor="edit-assignee">Assignee</label>
              <input
                id="edit-assignee"
                value={assignee}
                onChange={(e) => setAssignee(e.target.value)}
                maxLength={200}
              />
              <FieldError message={fieldErrors.assignee} />
            </div>
            {ticket.category && (
              <p className="muted">Category (set at create): {ticket.category}</p>
            )}
            <div className="actions-inline">
              <button type="submit">Save changes</button>
              <button
                type="button"
                className="secondary"
                onClick={() => {
                  setEditing(false);
                  setTitle(ticket.title);
                  setDescription(ticket.description);
                  setPriority(ticket.priority);
                  setAssignee(ticket.assignee ?? "");
                  setSaveError(null);
                  setFieldErrors({});
                }}
              >
                Cancel
              </button>
            </div>
          </form>
        )}
      </section>

      <section className="panel">
        <h2>Status</h2>
        <ErrorAlert error={transitionError} title="Transition failed" />
        {nextStatuses.length === 0 ? (
          <p className="muted">No further transitions from {ticket.status}.</p>
        ) : (
          <div className="actions-inline">
            {nextStatuses.map((status) => (
              <button
                key={status}
                type="button"
                disabled={transitioning}
                onClick={() => handleTransition(status)}
              >
                Move to {status}
              </button>
            ))}
          </div>
        )}
      </section>

      <section className="panel">
        <h2>Comments</h2>
        {ticket.comments.length === 0 && (
          <p className="muted">No comments yet.</p>
        )}
        {ticket.comments.map((c) => (
          <div key={c.id} className="comment">
            <p>{c.body}</p>
            <p className="muted">
              {c.author} · {formatDateTime(c.createdAt)}
            </p>
          </div>
        ))}
        <form className="stack" onSubmit={handleComment} style={{ marginTop: "1rem" }}>
          <ErrorAlert error={commentError} title="Could not add comment" />
          <label htmlFor="comment-body">Add comment</label>
          <textarea
            id="comment-body"
            rows={3}
            value={commentBody}
            onChange={(e) => setCommentBody(e.target.value)}
            maxLength={5000}
          />
          <button type="submit" disabled={commentSubmitting || !commentBody.trim()}>
            {commentSubmitting ? "Posting…" : "Post comment"}
          </button>
        </form>
      </section>

      <AskPanel />

      <p className="muted">
        <button type="button" className="secondary" onClick={() => load()}>
          Refresh
        </button>
      </p>
    </div>
  );
}
