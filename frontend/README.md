# MarketMind AI Frontend

React and TypeScript frontend foundation for the MarketMind AI investment research workspace.

## Stack

- React 19
- TypeScript
- Vite
- Material UI
- React Router
- TanStack React Query
- Recharts

## Current Scope

The frontend contains responsive workspaces for:

- Dashboard
- Portfolio dashboard, price snapshots, holdings, allocation, XLSX import, and import history
- Watchlist
- Company Intelligence
- AI Research Assistant
- Documents
- Sources and source health
- Document download jobs
- Scheduler jobs
- Alerts
- Settings

Portfolio intelligence, sources, documents, download jobs, scheduler jobs,
and the AI Research Assistant are connected to the MarketMind backend through
TanStack React Query. Other investment-research screens continue to use
interface-development mock data.

## Prerequisites

- Node.js 20.19+ or 22.12+
- npm

The project has been verified with the repository's current Node.js toolchain.

## Install

From `frontend/`:

```bash
npm install
```

## Run

Optionally configure the backend URL:

```bash
export VITE_BACKEND_URL='http://localhost:8080'
```

```bash
npm run dev
```

Open:

```text
http://127.0.0.1:5173
```

## Quality Checks

```bash
npm run lint
npm run build
```

Preview the production build:

```bash
npm run preview
```

## Structure

```text
src/
├── api/              # Typed backend client, contracts, and offline fallbacks
├── components/       # Reusable layout, cards, status, and charts
├── data/             # Typed local mock data
├── pages/            # Route-level workspaces
├── App.tsx           # Route definitions
├── main.tsx          # Application providers and bootstrap
├── theme.ts          # Material UI design system
└── styles.css        # Global styles
```

## Development Notes

- Keep API/server state inside React Query.
- Portfolio XLSX uploads use `multipart/form-data`; broker files are parsed by
  the backend and must never be added to the repository.
- Manual and mock prices are local test snapshots. They are not live quotes,
  broker connections, or executable trading signals.
- Public quote refresh is best-effort and credential-free. The UI displays
  provider status and partial failures; quote availability is not guaranteed.
- The AI Research Assistant can index extracted documents, scope a question
  to one document or all indexed documents, and render the returned citations.
  Ollama and Qdrant must be available through the backend configuration.
- AI answers are document-grounded decision support, not financial advice.
  Do not present an answer without its citations, status, and disclaimer.
- Network failures and HTTP 502/503/504 responses use clearly labelled fallback
  demonstration data. Other backend errors remain visible to the user.
- Do not calculate authoritative portfolio or financial metrics in the browser.
- Do not add API keys or other secrets to Vite environment variables; browser bundles are public.
- Preserve source timestamps, citations, uncertainty, and the decision-support disclaimer when real research data is introduced.
- The current values and recommendations are fictional mock data for interface development only.
