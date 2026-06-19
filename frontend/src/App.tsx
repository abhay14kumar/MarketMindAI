import { lazy, Suspense } from 'react';
import { Box, CircularProgress } from '@mui/material';
import { Route, Routes } from 'react-router-dom';
import { AppShell } from './components/AppShell';

const DashboardPage = lazy(() =>
  import('./pages/DashboardPage').then((module) => ({ default: module.DashboardPage })),
);
const PortfolioPage = lazy(() =>
  import('./pages/PortfolioPage').then((module) => ({ default: module.PortfolioPage })),
);
const WatchlistPage = lazy(() =>
  import('./pages/WatchlistPage').then((module) => ({ default: module.WatchlistPage })),
);
const CompanyIntelligencePage = lazy(() =>
  import('./pages/CompanyIntelligencePage').then((module) => ({
    default: module.CompanyIntelligencePage,
  })),
);
const ResearchAssistantPage = lazy(() =>
  import('./pages/ResearchAssistantPage').then((module) => ({
    default: module.ResearchAssistantPage,
  })),
);
const DocumentsPage = lazy(() =>
  import('./pages/DocumentsPage').then((module) => ({ default: module.DocumentsPage })),
);
const AlertsPage = lazy(() =>
  import('./pages/AlertsPage').then((module) => ({ default: module.AlertsPage })),
);
const SettingsPage = lazy(() =>
  import('./pages/SettingsPage').then((module) => ({ default: module.SettingsPage })),
);
const NotFoundPage = lazy(() =>
  import('./pages/NotFoundPage').then((module) => ({ default: module.NotFoundPage })),
);

function PageLoader() {
  return (
    <Box display="grid" minHeight="50vh" sx={{ placeItems: 'center' }}>
      <CircularProgress size={32} />
    </Box>
  );
}

export default function App() {
  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route element={<AppShell />}>
          <Route index element={<DashboardPage />} />
          <Route path="portfolio" element={<PortfolioPage />} />
          <Route path="watchlist" element={<WatchlistPage />} />
          <Route path="company-intelligence" element={<CompanyIntelligencePage />} />
          <Route path="research" element={<ResearchAssistantPage />} />
          <Route path="documents" element={<DocumentsPage />} />
          <Route path="alerts" element={<AlertsPage />} />
          <Route path="settings" element={<SettingsPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </Suspense>
  );
}
