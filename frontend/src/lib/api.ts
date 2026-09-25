import type { AiAskResponse, ApiErrorBody, Comment, Ticket, TicketDetail } from "./types";

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: { field: string; message: string }[];

  constructor(status: number, body: ApiErrorBody) {
    super(body.message || "Request failed");
    this.status = status;
    this.code = body.code;
    this.fieldErrors = body.fieldErrors ?? [];
  }
}

async function parseJson<T>(response: Response): Promise<T> {
  const text = await response.text();
  if (!text) {
    return {} as T;
  }
  return JSON.parse(text) as T;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
  });

  if (!response.ok) {
    const body = await parseJson<ApiErrorBody>(response);
    throw new ApiError(response.status, body);
  }

  return parseJson<T>(response);
}

export async function listTickets(params?: {
  q?: string;
  status?: string;
}): Promise<Ticket[]> {
  const search = new URLSearchParams();
  if (params?.q) {
    search.set("q", params.q);
  }
  if (params?.status) {
    search.set("status", params.status);
  }
  const query = search.toString();
  return request<Ticket[]>(`/api/tickets${query ? `?${query}` : ""}`);
}

export async function getTicket(ticketId: string): Promise<TicketDetail> {
  return request<TicketDetail>(`/api/tickets/${encodeURIComponent(ticketId)}`);
}

export async function createTicket(body: {
  title: string;
  description: string;
  priority: string;
  assignee?: string;
  category?: string;
}): Promise<Ticket> {
  return request<Ticket>("/api/tickets", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export async function updateTicket(
  ticketId: string,
  body: Record<string, string | undefined>,
): Promise<TicketDetail> {
  return request<TicketDetail>(`/api/tickets/${encodeURIComponent(ticketId)}`, {
    method: "PATCH",
    body: JSON.stringify(body),
  });
}

export async function addComment(
  ticketId: string,
  body: string,
): Promise<Comment> {
  return request<Comment>(
    `/api/tickets/${encodeURIComponent(ticketId)}/comments`,
    {
      method: "POST",
      body: JSON.stringify({ body }),
    },
  );
}

export async function transitionTicket(
  ticketId: string,
  status: string,
): Promise<TicketDetail> {
  return request<TicketDetail>(
    `/api/tickets/${encodeURIComponent(ticketId)}/transitions`,
    {
      method: "POST",
      body: JSON.stringify({ status }),
    },
  );
}

export async function askAi(question: string): Promise<AiAskResponse> {
  return request<AiAskResponse>("/api/ai/ask", {
    method: "POST",
    body: JSON.stringify({ question }),
  });
}
