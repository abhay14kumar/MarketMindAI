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

The frontend currently contains responsive, mock-data-only UI pages for:

- Dashboard
- Portfolio
- Watchlist
- Company Intelligence
- AI Research Assistant
- Documents
- Alerts
- Settings

No backend endpoints, credentials, or external market-data services are connected.

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
├── components/       # Reusable layout, cards, status, and charts
├── data/             # Typed local mock data
├── pages/            # Route-level workspaces
├── App.tsx           # Route definitions
├── main.tsx          # Application providers and bootstrap
├── theme.ts          # Material UI design system
└── styles.css        # Global styles
```

## Development Notes

- Keep API/server state inside React Query when backend integration begins.
- Do not calculate authoritative portfolio or financial metrics in the browser.
- Do not add API keys or other secrets to Vite environment variables; browser bundles are public.
- Preserve source timestamps, citations, uncertainty, and the decision-support disclaimer when real research data is introduced.
- The current values and recommendations are fictional mock data for interface development only.
