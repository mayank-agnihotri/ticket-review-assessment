import type { Metadata } from "next";
import Link from "next/link";
import "./globals.css";

export const metadata: Metadata = {
  title: "Ticket Management",
  description: "Support ticket management with AI Q&A",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>
        <header className="site-header">
          <strong>Ticket Management</strong>
          <nav>
            <Link href="/tickets">Tickets</Link>
            <Link href="/tickets/new">New ticket</Link>
          </nav>
        </header>
        <main>{children}</main>
      </body>
    </html>
  );
}
