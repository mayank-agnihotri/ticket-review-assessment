"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { AskPanel } from "@/components/AskPanel";
import { ErrorAlert } from "@/components/ErrorAlert";
import { listTickets } from "@/lib/api";
import { formatDateTime } from "@/lib/format";
import { ALL_STATUSES } from "@/lib/stateMachine";
import type { Ticket, TicketStatus } from "@/lib/types";

export default function TicketsPage() {
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [q, setQ] = useState("");
  const [debouncedQ, setDebouncedQ] = useState("");
  const [status, setStatus] = useState<TicketStatus | "">("");

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedQ(q.trim()), 300);
    return () => window.clearTimeout(timer);
  }, [q]);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await listTickets({
        q: debouncedQ || undefined,
        status: status || undefined,
      });
      setTickets(data);
    } catch (err) {
      setError(err);
      setTickets([]);
    } finally {
      setLoading(false);
    }
  }, [debouncedQ, status]);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <div>
      <div className="row" style={{ justifyContent: "space-between" }}>
        <h1>Tickets</h1>
        <Link className="button" href="/tickets/new">Create ticket</Link>
      </div>

      <section className="panel">
        <div className="row">
          <div style={{ flex: "1 1 200px" }}>
            <label htmlFor="search">Keyword search</label>
            <input
              id="search"
              type="search"
              placeholder="Title, description, or ticket ID"
              value={q}
              onChange={(e) => setQ(e.target.value)}
            />
          </div>
          <div style={{ flex: "0 1 180px" }}>
            <label htmlFor="status-filter">Status</label>
            <select
              id="status-filter"
              value={status}
              onChange={(e) =>
                setStatus(e.target.value as TicketStatus | "")
              }
            >
              <option value="">All</option>
              {ALL_STATUSES.map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
          </div>
        </div>
      </section>

      <ErrorAlert error={error} title="Could not load tickets" />

      <section className="panel">
        {loading && <p className="muted">Loading tickets…</p>}
        {!loading && tickets.length === 0 && (
          <p className="muted">No tickets found.</p>
        )}
        {!loading && tickets.length > 0 && (
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Title</th>
                <th>Status</th>
                <th>Priority</th>
                <th>Assignee</th>
                <th>Updated</th>
              </tr>
            </thead>
            <tbody>
              {tickets.map((t) => (
                <tr key={t.ticketId}>
                  <td>
                    <Link href={`/tickets/${t.ticketId}`}>{t.ticketId}</Link>
                  </td>
                  <td>{t.title}</td>
                  <td><span className="badge">{t.status}</span></td>
                  <td>{t.priority}</td>
                  <td>{t.assignee ?? "—"}</td>
                  <td>{formatDateTime(t.updatedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <AskPanel />
    </div>
  );
}
