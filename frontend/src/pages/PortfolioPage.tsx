import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  HistoryRounded,
  PriceChangeRounded,
  PublicRounded,
  RefreshRounded,
  SyncRounded,
  UploadFileRounded,
} from '@mui/icons-material';
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Alert,
  Button,
  Chip,
  CircularProgress,
  Grid,
  MenuItem,
  Snackbar,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { ExpandMoreRounded } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { portfolioQueries, priceFeedQueries } from '../api/client';
import type { ManualPriceRequest } from '../api/types';
import { MetricCard } from '../components/MetricCard';
import { PageHeader } from '../components/PageHeader';
import { QueryState } from '../components/QueryState';
import { SectionCard } from '../components/SectionCard';
import { AllocationChart } from '../components/charts/AllocationChart';
import { formatCurrency, formatDateTime, formatEnum, formatNumber } from '../utils/format';
import { useNotifications } from '../notifications/NotificationProvider';

const initialManualPrice: ManualPriceRequest = {
  symbol: '',
  exchange: 'NSE',
  lastPrice: 0,
  previousClose: 0,
  source: 'MANUAL',
};
const PRICE_REFRESH_INTERVAL_MS = 30_000;

export function PortfolioPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { notify } = useNotifications();
  const [manualPrice, setManualPrice] = useState(initialManualPrice);
  const [message, setMessage] = useState<{ severity: 'success' | 'error'; text: string } | null>(null);
  const summaryQuery = useQuery({
    queryKey: ['portfolio', 'summary'],
    queryFn: portfolioQueries.summary,
    refetchInterval: PRICE_REFRESH_INTERVAL_MS,
  });
  const holdingsQuery = useQuery({
    queryKey: ['portfolio', 'holdings'],
    queryFn: () => portfolioQueries.holdings(),
    refetchInterval: PRICE_REFRESH_INTERVAL_MS,
  });
  const sectorQuery = useQuery({ queryKey: ['portfolio', 'allocation', 'sector'], queryFn: portfolioQueries.sectorAllocation });
  const instrumentQuery = useQuery({ queryKey: ['portfolio', 'allocation', 'instrument'], queryFn: portfolioQueries.instrumentAllocation });
  const latestPricesQuery = useQuery({
    queryKey: ['market', 'prices', 'latest'],
    queryFn: priceFeedQueries.latest,
    refetchInterval: PRICE_REFRESH_INTERVAL_MS,
  });
  const providerStatusQuery = useQuery({
    queryKey: ['market', 'prices', 'provider-status'],
    queryFn: priceFeedQueries.providerStatus,
    refetchInterval: PRICE_REFRESH_INTERVAL_MS,
  });

  const refreshPortfolio = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['portfolio'] }),
      queryClient.invalidateQueries({ queryKey: ['market', 'prices'] }),
    ]);
  };
  const mockRefresh = useMutation({
    mutationFn: priceFeedQueries.refreshMock,
    onSuccess: async (job) => {
      await refreshPortfolio();
      setMessage({ severity: 'success', text: `Updated ${job.updatedInstruments} mock prices.` });
      notify({
        title: 'Price refresh completed',
        message: `Updated ${job.updatedInstruments} mock prices.`,
        severity: 'success',
        path: '/portfolio',
      });
    },
    onError: (error) => setMessage({
      severity: 'error',
      text: error instanceof Error ? error.message : 'Mock refresh failed.',
    }),
  });
  const realRefresh = useMutation({
    mutationFn: priceFeedQueries.refreshReal,
    onSuccess: async (job) => {
      await refreshPortfolio();
      await providerStatusQuery.refetch();
      setMessage({
        severity: job.status === 'FAILED' ? 'error' : 'success',
        text: `${job.provider ?? 'Public provider'} updated ${job.updatedInstruments} prices; ${job.failedInstruments} failed.`,
      });
      notify({
        title: job.status === 'FAILED' ? 'Price refresh failed' : 'Price refresh completed',
        message: `${job.provider ?? 'Public provider'} updated ${job.updatedInstruments} prices; ${job.failedInstruments} failed.`,
        severity: job.status === 'FAILED' ? 'error' : 'success',
        path: '/portfolio',
      });
    },
    onError: (error) => setMessage({
      severity: 'error',
      text: error instanceof Error ? error.message : 'Real price refresh failed.',
    }),
  });
  const manualUpdate = useMutation({
    mutationFn: priceFeedQueries.updateManual,
    onSuccess: async (snapshot) => {
      await refreshPortfolio();
      setMessage({ severity: 'success', text: `${snapshot.symbol} price updated.` });
      setManualPrice(initialManualPrice);
    },
    onError: (error) => setMessage({
      severity: 'error',
      text: error instanceof Error ? error.message : 'Manual price update failed.',
    }),
  });

  const summary = summaryQuery.data;
  const holdings = holdingsQuery.data?.content ?? [];
  const sectorAllocation = sectorQuery.data ?? [];
  const instrumentAllocation = instrumentQuery.data ?? [];
  const loading = summaryQuery.isPending || holdingsQuery.isPending || sectorQuery.isPending || instrumentQuery.isPending;
  const error = summaryQuery.error ?? holdingsQuery.error ?? sectorQuery.error ?? instrumentQuery.error;
  const totalPnl = Number(summary?.totalPnl ?? 0);
  const dayPnl = Number(summary?.dayPnl ?? 0);
  const priceSources = Array.from(new Set(
    holdings.map((holding) => holding.priceSource).filter(Boolean),
  ));
  const dataSource = priceSources.length === 0
    ? 'Future live'
    : priceSources.length === 1
      ? formatEnum(priceSources[0])
      : 'Mixed sources';
  const hasMockPrices = priceSources.includes('MOCK');
  const isBackgroundRefreshing = !loading && (
    summaryQuery.isFetching
    || holdingsQuery.isFetching
    || latestPricesQuery.isFetching
    || providerStatusQuery.isFetching
  );
  const providerStatus = providerStatusQuery.data;

  const retry = () => {
    void summaryQuery.refetch();
    void holdingsQuery.refetch();
    void sectorQuery.refetch();
    void instrumentQuery.refetch();
    void latestPricesQuery.refetch();
    void providerStatusQuery.refetch();
  };

  return (
    <>
      <PageHeader
        eyebrow="Portfolio intelligence"
        title="Zerodha Holdings"
        description={`Latest market price: ${formatDateTime(summary?.latestPriceAt)} · Last import: ${formatDateTime(summary?.lastImportedAt)}`}
        action={(
          <Stack direction="row" gap={1} flexWrap="wrap" justifyContent="flex-end">
            <Button
              variant="outlined"
              startIcon={realRefresh.isPending ? <CircularProgress size={15} color="inherit" /> : <PublicRounded />}
              disabled={realRefresh.isPending}
              onClick={() => realRefresh.mutate()}
            >
              {realRefresh.isPending ? 'Refreshing…' : 'Refresh Real Prices'}
            </Button>
            <Button variant="outlined" startIcon={<HistoryRounded />} onClick={() => navigate('/portfolio/import-history')}>
              History
            </Button>
            <Button variant="contained" startIcon={<UploadFileRounded />} onClick={() => navigate('/portfolio/import')}>
              Import XLSX
            </Button>
          </Stack>
        )}
      />
      <QueryState loading={loading} error={error instanceof Error ? error : null} empty={false} onRetry={retry}>
        <Grid container spacing={2}>
          <Grid size={12}>
            <Stack
              direction={{ xs: 'column', sm: 'row' }}
              alignItems={{ sm: 'center' }}
              justifyContent="space-between"
              gap={1.5}
              sx={{
                px: 2,
                py: 1.4,
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: 2,
                bgcolor: 'rgba(84,214,194,0.035)',
              }}
            >
              <Stack direction="row" gap={1} alignItems="center" flexWrap="wrap">
                <Chip label={`Data source: ${dataSource}`} variant="outlined" color="primary" />
                {hasMockPrices && <Chip label="Simulated data" color="warning" />}
                <Typography variant="body2" color="text.secondary">
                  Last updated {formatDateTime(summary?.latestPriceAt)}
                </Typography>
              </Stack>
              <Stack direction="row" gap={1} alignItems="center">
                {isBackgroundRefreshing
                  ? <CircularProgress size={15} />
                  : <SyncRounded color="primary" sx={{ fontSize: 18 }} />}
                <Typography variant="caption" color="text.secondary">
                  {isBackgroundRefreshing ? 'Refreshing prices…' : 'Auto-refreshing every 30s'}
                </Typography>
              </Stack>
            </Stack>
          </Grid>
          <Grid size={{ xs: 12, sm: 6, xl: 3 }}>
            <MetricCard label="Invested value" value={formatCurrency(summary?.totalInvestedValue)} change={`${summary?.totalHoldings ?? 0} holdings`} />
          </Grid>

          <Grid size={12}>
            <SectionCard eyebrow="Live price refresh" title="Provider status">
              <Stack direction={{ xs: 'column', md: 'row' }} gap={2} justifyContent="space-between">
                <Stack direction="row" gap={1} flexWrap="wrap" alignItems="center">
                  <Chip
                    label={`Provider: ${formatEnum(providerStatus?.configuredProvider ?? 'PUBLIC')}`}
                    color="primary"
                    variant="outlined"
                  />
                  <Chip
                    label={providerStatus?.lastRefreshStatus
                      ? `Last refresh: ${formatEnum(providerStatus.lastRefreshStatus)}`
                      : 'Not refreshed yet'}
                    color={providerStatus?.lastRefreshStatus === 'FAILED' ? 'error' : 'default'}
                    variant={providerStatus?.lastRefreshStatus === 'FAILED' ? 'filled' : 'outlined'}
                  />
                  <Chip
                    label={providerStatus?.scheduledRefreshEnabled
                      ? `Scheduler: ${providerStatus.refreshIntervalSeconds}s`
                      : 'Scheduler disabled'}
                    variant="outlined"
                  />
                </Stack>
                <Stack alignItems={{ md: 'flex-end' }}>
                  <Typography variant="body2" color="text.secondary">
                    {providerStatus
                      ? `${providerStatus.successfulSymbols} successful · ${providerStatus.failedSymbols} failed`
                      : 'Waiting for provider status'}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Last provider refresh {formatDateTime(providerStatus?.lastRefreshAt)}
                  </Typography>
                </Stack>
              </Stack>
              {providerStatus?.errorSummary && (
                <Alert severity="warning" sx={{ mt: 2 }}>
                  {providerStatus.errorSummary}
                </Alert>
              )}
            </SectionCard>
          </Grid>
          <Grid size={{ xs: 12, sm: 6, xl: 3 }}>
            <MetricCard label="Current value" value={formatCurrency(summary?.totalCurrentValue)} change={formatDateTime(summary?.latestPriceAt)} />
          </Grid>
          <Grid size={{ xs: 12, sm: 6, xl: 3 }}>
            <MetricCard label="Day gain / loss" value={formatCurrency(dayPnl)} change={`${Number(summary?.dayPnlPercentage ?? 0).toFixed(2)}%`} tone={dayPnl >= 0 ? 'positive' : 'negative'} />
          </Grid>
          <Grid size={{ xs: 12, sm: 6, xl: 3 }}>
            <MetricCard label="Total gain / loss" value={formatCurrency(totalPnl)} change={`${Number(summary?.totalPnlPercentage ?? 0).toFixed(2)}%`} tone={totalPnl >= 0 ? 'positive' : 'negative'} />
          </Grid>

          <Grid size={12}>
            <Accordion disableGutters>
              <AccordionSummary expandIcon={<ExpandMoreRounded />}>
                <Stack>
                  <Typography variant="overline" color="text.secondary">Developer tools</Typography>
                  <Typography fontWeight={650}>Price simulation controls</Typography>
                </Stack>
              </AccordionSummary>
              <AccordionDetails>
                <Stack gap={2}>
                  <Stack direction={{ xs: 'column', md: 'row' }} gap={1.5} alignItems={{ md: 'center' }}>
                <TextField
                  label="Symbol"
                  value={manualPrice.symbol}
                  onChange={(event) => setManualPrice((current) => ({ ...current, symbol: event.target.value.toUpperCase() }))}
                />
                <TextField
                  select
                  label="Exchange"
                  value={manualPrice.exchange}
                  onChange={(event) => setManualPrice((current) => ({ ...current, exchange: event.target.value as ManualPriceRequest['exchange'] }))}
                  sx={{ minWidth: 130 }}
                >
                  <MenuItem value="NSE">NSE</MenuItem>
                  <MenuItem value="BSE">BSE</MenuItem>
                  <MenuItem value="UNKNOWN">Unknown</MenuItem>
                </TextField>
                <TextField
                  label="Current price"
                  type="number"
                  value={manualPrice.lastPrice}
                  onChange={(event) => setManualPrice((current) => ({ ...current, lastPrice: Number(event.target.value) }))}
                  slotProps={{ htmlInput: { min: 0, step: '0.01' } }}
                />
                <TextField
                  label="Previous close"
                  type="number"
                  value={manualPrice.previousClose}
                  onChange={(event) => setManualPrice((current) => ({ ...current, previousClose: Number(event.target.value) }))}
                  slotProps={{ htmlInput: { min: 0, step: '0.01' } }}
                />
                    <Button
                      variant="contained"
                      startIcon={<PriceChangeRounded />}
                      disabled={!manualPrice.symbol || manualUpdate.isPending}
                      onClick={() => manualUpdate.mutate(manualPrice)}
                    >
                      {manualUpdate.isPending ? 'Saving…' : 'Update Price'}
                    </Button>
                  </Stack>
                  <Stack direction={{ xs: 'column', sm: 'row' }} gap={1} alignItems={{ sm: 'center' }}>
                    <Button
                      size="small"
                      variant="outlined"
                      startIcon={<RefreshRounded />}
                      disabled={mockRefresh.isPending}
                      onClick={() => mockRefresh.mutate()}
                    >
                      {mockRefresh.isPending ? 'Generating…' : 'Developer: Generate Mock Prices'}
                    </Button>
                    <Typography variant="caption" color="text.secondary">
                      Local simulation only. No external provider, credentials, or trading connection is used.
                    </Typography>
                  </Stack>
                </Stack>
              </AccordionDetails>
            </Accordion>
          </Grid>

          <Grid size={{ xs: 12, lg: 6 }}>
            <SectionCard eyebrow="Diversification" title="Sector allocation">
              <QueryState loading={false} error={null} empty={sectorAllocation.length === 0} onRetry={retry} emptyMessage="Import holdings to calculate sector allocation.">
                <AllocationChart data={sectorAllocation} />
              </QueryState>
            </SectionCard>
          </Grid>
          <Grid size={{ xs: 12, lg: 6 }}>
            <SectionCard eyebrow="Asset mix" title="Instrument allocation">
              <QueryState loading={false} error={null} empty={instrumentAllocation.length === 0} onRetry={retry} emptyMessage="Import holdings to calculate instrument allocation.">
                <AllocationChart data={instrumentAllocation} />
              </QueryState>
            </SectionCard>
          </Grid>

          <Grid size={12}>
            <SectionCard eyebrow="Positions" title="Current holdings">
              <QueryState loading={false} error={null} empty={holdings.length === 0} onRetry={retry} emptyMessage="No holdings yet. Import a Zerodha XLSX export to begin.">
                <TableContainer>
                  <Table sx={{ minWidth: 1450 }}>
                    <TableHead>
                      <TableRow>
                        <TableCell>Security</TableCell>
                        <TableCell>Sector</TableCell>
                        <TableCell align="right">Qty</TableCell>
                        <TableCell align="right">Avg. cost</TableCell>
                        <TableCell align="right">Current price</TableCell>
                        <TableCell align="right">Previous close</TableCell>
                        <TableCell align="right">Current value</TableCell>
                        <TableCell align="right">Day gain / loss</TableCell>
                        <TableCell align="right">Total gain / loss</TableCell>
                        <TableCell>Price timestamp</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {holdings.map((holding) => (
                        <TableRow key={holding.id} hover>
                          <TableCell>
                            <Typography fontWeight={650}>{holding.symbol}</Typography>
                            <Typography variant="caption" color="text.secondary">{formatEnum(holding.priceSource)}</Typography>
                          </TableCell>
                          <TableCell>{holding.sector ?? 'Unclassified'}</TableCell>
                          <TableCell align="right">{formatNumber(holding.quantity, 4)}</TableCell>
                          <TableCell align="right">{formatCurrency(holding.averageCost)}</TableCell>
                          <TableCell align="right">{holding.currentPrice == null ? '—' : formatCurrency(holding.currentPrice)}</TableCell>
                          <TableCell align="right">{holding.previousClose == null ? '—' : formatCurrency(holding.previousClose)}</TableCell>
                          <TableCell align="right">{holding.currentValue == null ? '—' : formatCurrency(holding.currentValue)}</TableCell>
                          <PnlCell value={holding.dayPnl} percentage={holding.dayPnlPercentage} />
                          <PnlCell value={holding.totalPnl} percentage={holding.totalPnlPercentage} />
                          <TableCell>{formatDateTime(holding.priceCapturedAt)}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              </QueryState>
            </SectionCard>
          </Grid>
        </Grid>
      </QueryState>
      <Snackbar open={message !== null} autoHideDuration={6000} onClose={() => setMessage(null)}>
        <Alert severity={message?.severity ?? 'success'} variant="filled" onClose={() => setMessage(null)}>
          {message?.text}
        </Alert>
      </Snackbar>
    </>
  );
}

function PnlCell({ value, percentage }: { value: number | null; percentage: number | null }) {
  if (value == null) {
    return <TableCell align="right">—</TableCell>;
  }
  return (
    <TableCell align="right" sx={{ color: value >= 0 ? 'primary.main' : 'error.main', fontWeight: 650 }}>
      {formatCurrency(value)} ({Number(percentage ?? 0).toFixed(2)}%)
    </TableCell>
  );
}
