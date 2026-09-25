"use client";

import { ApiError } from "@/lib/api";

type Props = {
  error: unknown;
  title?: string;
};

export function ErrorAlert({ error, title }: Props) {
  if (!error) {
    return null;
  }

  if (error instanceof ApiError) {
    return (
      <div className="alert alert-error" role="alert">
        {title && <strong>{title}</strong>}
        <p>{error.message}</p>
        {error.fieldErrors.length > 0 && (
          <ul>
            {error.fieldErrors.map((fe) => (
              <li key={`${fe.field}-${fe.message}`}>
                <span className="field-name">{fe.field}</span>: {fe.message}
              </li>
            ))}
          </ul>
        )}
      </div>
    );
  }

  if (error instanceof Error) {
    return (
      <div className="alert alert-error" role="alert">
        {title && <strong>{title}</strong>}
        <p>{error.message}</p>
      </div>
    );
  }

  return (
    <div className="alert alert-error" role="alert">
      <p>Something went wrong.</p>
    </div>
  );
}
