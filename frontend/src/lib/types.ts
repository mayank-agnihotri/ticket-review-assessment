export type Priority = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export type TicketStatus =
  | "OPEN"
  | "IN_PROGRESS"
  | "RESOLVED"
  | "CLOSED"
  | "CANCELLED";

export interface Ticket {
  ticketId: string;
  title: string;
  description: string;
  priority: Priority;
  assignee: string | null;
  category: string | null;
  status: TicketStatus;
  resolutionNotes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Comment {
  id: string;
  body: string;
  author: string;
  createdAt: string;
}

export interface TicketDetail extends Ticket {
  comments: Comment[];
}

export interface FieldError {
  field: string;
  message: string;
}

export interface ApiErrorBody {
  code: string;
  message: string;
  fieldErrors?: FieldError[];
}

export interface AiAskRetrievalInfo {
  topK: number;
  similarityThreshold: number;
  chunksUsed: number;
}

export interface AiAskResponse {
  answer: string;
  citedTicketIds: string[];
  grounded: boolean;
  noMatch: boolean;
  retrieval: AiAskRetrievalInfo;
}
