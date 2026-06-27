import { useMemo, useState } from 'react';
import {
  CloseRounded,
  HubRounded,
  LinkRounded,
  RefreshRounded,
  VerifiedRounded,
  VisibilityRounded,
} from '@mui/icons-material';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Divider,
  Drawer,
  Grid,
  IconButton,
  LinearProgress,
  Stack,
  Tab,
  Tabs,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { sourceIntelligenceQueries } from '../api/client';
import type { SourceIntelligenceCatalogItem } from '../api/types';
import { MetricCard } from '../components/MetricCard';
import { PageHeader } from '../components/PageHeader';
import { QueryState } from '../components/QueryState';
import { SectionCard } from '../components/SectionCard';
import { StatusChip } from '../components/StatusChip';
import { useNotifications } from '../notifications/NotificationProvider';
import { formatDateTime, formatEnum, formatNumber } from '../utils/format';

export function SourcesPage() {
  const queryClient = useQueryClient();
  const { notify } = useNotifications();
  const [tab, setTab] = useState(0);
  const [selected, setSelected] = useState<SourceIntelligenceCatalogItem | null>(null);

  const catalogQuery = useQuery({
    queryKey: ['source-intelligence', 'catalog'],
    queryFn: sourceIntelligenceQueries.catalog,
    refetchInterval: 10_000,
  });
  const metricsQuery = useQuery({
    queryKey: ['source-intelligence', 'metrics'],
    queryFn: sourceIntelligenceQueries.metrics,
    refetchInterval: 10_000,
  });
  const coverageQuery = useQuery({
    queryKey: ['source-intelligence', 'coverage'],
    queryFn: sourceIntelligenceQueries.coverage,
    refetchInterval: 10_000,
  });
  const activityQuery = useQuery({
    queryKey: ['source-intelligence', 'activity'],
    queryFn: () => sourceIntelligenceQueries.activity(100),
    refetchInterval: 5_000,
  });
  const connectorsQuery = useQuery({
    queryKey: ['source-intelligence', 'connectors'],
    queryFn: sourceIntelligenceQueries.connectors,
  });

  const refreshAll = () => Promise.all([
    queryClient.invalidateQueries({ queryKey: ['source-intelligence'] }),
    queryClient.invalidateQueries({ queryKey: ['discovery-jobs'] }),
    queryClient.invalidateQueries({ queryKey: ['pipeline-jobs'] }),
  ]);
  const validate = useMutation({
    mutationFn: sourceIntelligenceQueries.validate,
    onSuccess: async (source) => {
      await refreshAll();
      setSelected(source);
      notify({
        title: 'Source validation completed',
        message: `${source.name} intelligence metadata was refreshed.`,
        severity: source.healthy === false ? 'warning' : 'success',
        path: '/sources',
      });
    },
    onError: (error) => notify({
      title: 'Source validation failed',
      message: error instanceof Error ? error.message : 'Validation failed.',
      severity: 'error',
      path: '/sources',
    }),
  });
  const refresh = useMutation({
    mutationFn: sourceIntelligenceQueries.refresh,
    onSuccess: async (result) => {
      await refreshAll();
      notify({
        title: result.status === 'FAILED' ? 'Source refresh failed' : 'Source refresh completed',
        message: result.message,
        severity: result.status === 'FAILED'
          ? 'error' : result.documentsDiscovered === 0 ? 'warning' : 'success',
        path: '/sources',
      });
    },
    onError: (error) => notify({
      title: 'Source refresh failed',
      message: error instanceof Error ? error.message : 'Refresh failed.',
      severity: 'error',
      path: '/sources',
    }),
  });

  const catalog = useMemo(() => catalogQuery.data ?? [], [catalogQuery.data]);
  const coverage = coverageQuery.data ?? [];
  const activity = activityQuery.data ?? [];
  const connectors = connectorsQuery.data ?? [];
  const metrics = metricsQuery.data;
  const error = catalogQuery.error ?? metricsQuery.error;

  return (
    <>
      <PageHeader
        eyebrow="Enterprise data governance"
        title="Sources Intelligence Center"
        description="Connector selection, source trust, health, freshness, coverage, scheduling, and live downstream activity in one operating view."
        action={(
          <Button
            variant="outlined"
            startIcon={<RefreshRounded />}
            onClick={() => void refreshAll()}
          >
            Refresh Intelligence
          </Button>
        )}
      />

      {error instanceof Error && <Alert severity="error" sx={{ mb: 2 }}>{error.message}</Alert>}

      <Grid container spacing={2} mb={2}>
        <Grid size={{ xs: 6, lg: 2 }}><MetricCard label="Catalog sources" value={formatNumber(metrics?.totalSources)} change={`${metrics?.officialSources ?? 0} official`} /></Grid>
        <Grid size={{ xs: 6, lg: 2 }}><MetricCard label="Healthy sources" value={formatNumber(metrics?.healthySources)} change={`${metrics?.degradedSources ?? 0} degraded`} /></Grid>
        <Grid size={{ xs: 6, lg: 2 }}><MetricCard label="Connectors" value={formatNumber(metrics?.enabledConnectors)} change="Selection enabled" /></Grid>
        <Grid size={{ xs: 6, lg: 2 }}><MetricCard label="Documents found" value={formatNumber(metrics?.documentsDiscovered)} change={`${metrics?.discoveryJobs ?? 0} discovery jobs`} /></Grid>
        <Grid size={{ xs: 6, lg: 2 }}><MetricCard label="Average trust" value={`${(metrics?.averageTrustScore ?? 0).toFixed(0)}%`} change="Official-first ranking" /></Grid>
        <Grid size={{ xs: 6, lg: 2 }}><MetricCard label="Coverage" value={`${(metrics?.coveragePercent ?? 0).toFixed(0)}%`} change={`${metrics?.pipelineJobs ?? 0} pipeline jobs`} /></Grid>
      </Grid>

      <SectionCard eyebrow="Operating views" title="Source intelligence">
        <Tabs value={tab} onChange={(_, value) => setTab(value)} sx={{ mb: 2 }}>
          <Tab label="Catalog" />
          <Tab label="Coverage Matrix" />
          <Tab label="Live Activity" />
          <Tab label="Connectors" />
        </Tabs>

        {tab === 0 && (
          <QueryState
            loading={catalogQuery.isPending}
            error={catalogQuery.error instanceof Error ? catalogQuery.error : null}
            empty={catalog.length === 0}
            emptyMessage="No governed sources exist yet. Register an official or approved provider to begin capability detection."
            onRetry={() => void catalogQuery.refetch()}
          >
            <TableContainer>
              <Table sx={{ minWidth: 2100 }}>
                <TableHead><TableRow>
                  <TableCell>Source</TableCell><TableCell>Trust</TableCell>
                  <TableCell>Reliability</TableCell><TableCell>Freshness</TableCell>
                  <TableCell>Connector</TableCell><TableCell>Health</TableCell>
                  <TableCell>Latency</TableCell><TableCell>Formats</TableCell>
                  <TableCell>Capabilities</TableCell><TableCell>Document types</TableCell>
                  <TableCell>Last crawl</TableCell><TableCell>Next crawl</TableCell>
                  <TableCell>Scheduler</TableCell><TableCell>Coverage</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow></TableHead>
                <TableBody>
                  {catalog.map((source) => (
                    <TableRow key={source.id} hover>
                      <TableCell>
                        <Typography fontWeight={700}>{source.name}</Typography>
                        <Stack direction="row" gap={0.75} alignItems="center">
                          {source.official && <Chip size="small" label="Official" color="primary" />}
                          <Typography
                            component="a"
                            href={source.baseUrl}
                            target="_blank"
                            rel="noreferrer"
                            variant="caption"
                            color="text.secondary"
                          >
                            <LinkRounded sx={{ fontSize: 12, verticalAlign: 'middle' }} /> {source.code}
                          </Typography>
                        </Stack>
                      </TableCell>
                      <TableCell>
                        <Typography fontWeight={700}>{source.trustScore}%</Typography>
                        <Typography variant="caption">{formatEnum(source.trustTier)}</Typography>
                      </TableCell>
                      <TableCell>{Math.round(source.reliabilityScore * 100)}%</TableCell>
                      <TableCell sx={{ minWidth: 150 }}>
                        <Stack direction="row" justifyContent="space-between">
                          <Typography variant="caption">{source.freshnessScore}%</Typography>
                        </Stack>
                        <LinearProgress variant="determinate" value={source.freshnessScore} />
                      </TableCell>
                      <TableCell><Chip label={formatEnum(source.connectorType)} variant="outlined" /></TableCell>
                      <TableCell><StatusChip label={source.healthy == null ? 'UNKNOWN' : source.healthy ? 'HEALTHY' : 'DEGRADED'} /></TableCell>
                      <TableCell>{source.latencyMs == null ? '—' : `${source.latencyMs} ms`}</TableCell>
                      <TableCell><TagList values={source.supportedFormats} /></TableCell>
                      <TableCell><TagList values={source.capabilities} limit={3} /></TableCell>
                      <TableCell><TagList values={source.supportedDocumentTypes} limit={3} /></TableCell>
                      <TableCell>{formatDateTime(source.lastCrawlAt)}</TableCell>
                      <TableCell>{formatDateTime(source.nextCrawlAt)}</TableCell>
                      <TableCell><StatusChip label={source.schedulerState} /></TableCell>
                      <TableCell>
                        <Typography>{source.documentsDiscovered} documents</Typography>
                        <Typography variant="caption" color="text.secondary">
                          {source.successfulCrawls}/{source.totalCrawls} successful
                        </Typography>
                      </TableCell>
                      <TableCell align="right">
                        <Stack direction="row" justifyContent="flex-end">
                          <Button size="small" startIcon={<VisibilityRounded />} onClick={() => setSelected(source)}>Details</Button>
                          <Button
                            size="small"
                            startIcon={validate.isPending && validate.variables === source.id
                              ? <CircularProgress size={14} /> : <VerifiedRounded />}
                            disabled={validate.isPending}
                            onClick={() => validate.mutate(source.id)}
                          >
                            Validate
                          </Button>
                          <Button
                            size="small"
                            variant="outlined"
                            startIcon={<RefreshRounded />}
                            disabled={refresh.isPending}
                            onClick={() => refresh.mutate(source.id)}
                          >
                            Refresh
                          </Button>
                        </Stack>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </QueryState>
        )}

        {tab === 1 && (
          <QueryState
            loading={coverageQuery.isPending}
            error={coverageQuery.error instanceof Error ? coverageQuery.error : null}
            empty={coverage.length === 0}
            emptyMessage="Coverage is empty because no company/document combinations have been discovered. Run discovery, then ingest trusted documents."
            onRetry={() => void coverageQuery.refetch()}
          >
            <TableContainer>
              <Table>
                <TableHead><TableRow>
                  <TableCell>Company</TableCell><TableCell>Document type</TableCell>
                  <TableCell>Discovered</TableCell><TableCell>New</TableCell>
                  <TableCell>Ingested</TableCell><TableCell>AI Ready</TableCell>
                  <TableCell>Coverage</TableCell>
                </TableRow></TableHead>
                <TableBody>
                  {coverage.map((row) => (
                    <TableRow key={`${row.companySymbol}-${row.documentType}`} hover>
                      <TableCell>{row.companySymbol}</TableCell>
                      <TableCell>{formatEnum(row.documentType)}</TableCell>
                      <TableCell>{row.discoveredCount}</TableCell>
                      <TableCell>{row.newCount}</TableCell>
                      <TableCell>{row.ingestedCount}</TableCell>
                      <TableCell>{row.aiReadyCount}</TableCell>
                      <TableCell><StatusChip label={row.coverageStatus} /></TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </QueryState>
        )}

        {tab === 2 && (
          <QueryState
            loading={activityQuery.isPending}
            error={activityQuery.error instanceof Error ? activityQuery.error : null}
            empty={activity.length === 0}
            emptyMessage="No source activity yet. Validation, refresh, discovery, and pipeline events will appear here automatically."
            onRetry={() => void activityQuery.refetch()}
          >
            <Stack divider={<Divider flexItem />}>
              {activity.map((event) => (
                <Stack key={`${event.activityType}-${event.id}`} direction="row" gap={2} py={1.5}>
                  <Box sx={{ width: 10, height: 10, borderRadius: '50%', bgcolor: severityColor(event.severity), mt: 0.8, flexShrink: 0 }} />
                  <Box flex={1}>
                    <Stack direction="row" justifyContent="space-between" gap={2}>
                      <Typography fontWeight={700}>{event.title}</Typography>
                      <Typography variant="caption" color="text.secondary">{formatDateTime(event.occurredAt)}</Typography>
                    </Stack>
                    <Typography color="text.secondary">{event.message}</Typography>
                    <Chip size="small" label={formatEnum(event.activityType)} variant="outlined" sx={{ mt: 0.75 }} />
                  </Box>
                </Stack>
              ))}
            </Stack>
          </QueryState>
        )}

        {tab === 3 && (
          <Grid container spacing={2}>
            {connectors.map((connector) => (
              <Grid key={connector.connectorType} size={{ xs: 12, md: 6, xl: 4 }}>
                <SectionCard
                  eyebrow={formatEnum(connector.trustTier)}
                  title={formatEnum(connector.connectorType)}
                  action={<HubRounded color="primary" />}
                  minHeight={230}
                >
                  <Typography variant="overline">Formats</Typography>
                  <TagList values={connector.supportedFormats} />
                  <Typography variant="overline" display="block" mt={2}>Document types</Typography>
                  <TagList values={connector.supportedDocumentTypes} limit={5} />
                </SectionCard>
              </Grid>
            ))}
          </Grid>
        )}
      </SectionCard>

      <Drawer
        anchor="right"
        open={selected !== null}
        onClose={() => setSelected(null)}
        slotProps={{ paper: { sx: { width: { xs: '100%', md: 560 }, p: 3 } } }}
      >
        {selected && (
          <Stack gap={2}>
            <Stack direction="row" justifyContent="space-between">
              <Box>
                <Typography variant="overline" color="primary.main">Source intelligence profile</Typography>
                <Typography variant="h2">{selected.name}</Typography>
              </Box>
              <IconButton onClick={() => setSelected(null)}><CloseRounded /></IconButton>
            </Stack>
            <Alert severity={selected.healthy === false ? 'warning' : 'info'}>
              Connector {formatEnum(selected.connectorType)} · Trust {selected.trustScore}% ·
              Freshness {selected.freshnessScore}%
            </Alert>
            <Detail label="Organization" value={selected.organization} />
            <Detail label="Source type" value={formatEnum(selected.sourceType)} />
            <Detail label="Trust tier" value={formatEnum(selected.trustTier)} />
            <Detail label="HTTP status" value={selected.httpStatus?.toString() ?? 'Not validated'} />
            <Detail label="Latency" value={selected.latencyMs == null ? 'Unknown' : `${selected.latencyMs} ms`} />
            <Detail label="Last validation" value={formatDateTime(selected.lastValidatedAt)} />
            <Detail label="Last crawl" value={formatDateTime(selected.lastCrawlAt)} />
            <Detail label="Next crawl" value={formatDateTime(selected.nextCrawlAt)} />
            <Divider />
            <Typography variant="overline">Supported formats</Typography>
            <TagList values={selected.supportedFormats} />
            <Typography variant="overline">Capabilities</Typography>
            <TagList values={selected.capabilities} />
            <Typography variant="overline">Supported document types</Typography>
            <TagList values={selected.supportedDocumentTypes} />
          </Stack>
        )}
      </Drawer>
    </>
  );
}

function TagList({ values, limit = 4 }: { values: string[]; limit?: number }) {
  return (
    <Stack direction="row" gap={0.5} flexWrap="wrap">
      {values.slice(0, limit).map((value) => (
        <Chip key={value} size="small" label={formatEnum(value)} variant="outlined" />
      ))}
      {values.length > limit && <Chip size="small" label={`+${values.length - limit}`} />}
    </Stack>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <Stack direction="row" justifyContent="space-between" gap={2}>
      <Typography color="text.secondary">{label}</Typography>
      <Typography textAlign="right">{value}</Typography>
    </Stack>
  );
}

function severityColor(severity: string) {
  switch (severity.toUpperCase()) {
    case 'ERROR':
    case 'FAILED':
      return 'error.main';
    case 'WARNING':
      return 'warning.main';
    case 'SUCCESS':
      return 'success.main';
    default:
      return 'info.main';
  }
}
