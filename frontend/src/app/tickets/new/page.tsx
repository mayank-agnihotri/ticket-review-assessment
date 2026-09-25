"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { ErrorAlert } from "@/components/ErrorAlert";
import { FieldError } from "@/components/FieldError";
import { ApiError, createTicket } from "@/lib/api";
import type { Priority } from "@/lib/types";

const PRIORITIES: Priority[] = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];

export default function NewTicketPage() {
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<Priority>("MEDIUM");
  const [assignee, setAssignee] = useState("");
  const [category, setCategory] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  function mapFieldErrors(err: ApiError): Record<string, string> {
    const map: Record<string, string> = {};
    for (const fe of err.fieldErrors) {
      map[fe.field] = fe.message;
    }
    return map;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    setFieldErrors({});
    try {
      const ticket = await createTicket({
        title: title.trim(),
        description: description.trim(),
        priority,
        assignee: assignee.trim() || undefined,
        category: category.trim() || undefined,
      });
      router.push(`/tickets/${ticket.ticketId}`);
    } catch (err) {
      setError(err);
      if (err instanceof ApiError) {
        setFieldErrors(mapFieldErrors(err));
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div>
      <p><Link href="/tickets">← Back to list</Link></p>
      <h1>Create ticket</h1>

      <form className="panel stack" onSubmit={handleSubmit}>
        <ErrorAlert error={error} title="Could not create ticket" />

        <div>
          <label htmlFor="title">Title *</label>
          <input
            id="title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
            maxLength={200}
          />
          <FieldError message={fieldErrors.title} />
        </div>

        <div>
          <label htmlFor="description">Description *</label>
          <textarea
            id="description"
            rows={5}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            required
            maxLength={5000}
          />
          <FieldError message={fieldErrors.description} />
        </div>

        <div>
          <label htmlFor="priority">Priority *</label>
          <select
            id="priority"
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
          <label htmlFor="assignee">Assignee</label>
          <input
            id="assignee"
            value={assignee}
            onChange={(e) => setAssignee(e.target.value)}
            maxLength={200}
          />
          <FieldError message={fieldErrors.assignee} />
        </div>

        <div>
          <label htmlFor="category">Category</label>
          <input
            id="category"
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            maxLength={100}
          />
          <FieldError message={fieldErrors.category} />
        </div>

        <button type="submit" disabled={submitting}>
          {submitting ? "Creating…" : "Create ticket"}
        </button>
      </form>
    </div>
  );
}
