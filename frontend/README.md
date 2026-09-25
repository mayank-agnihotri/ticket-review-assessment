# Ticket Management UI

Next.js App Router frontend for the Ticket Management System backend.

## Setup

```bash
npm install
cp .env.example .env.local   # optional; default backend is http://localhost:8080
```

Start the Spring Boot API on port **8080**, then:

```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000). API calls use `/api/*` rewrites to `BACKEND_URL` (no browser CORS required).

## Scripts

- `npm run dev` — development server
- `npm run build` — production build
- `npm run test` — Vitest unit tests
- `npm run lint` — ESLint
