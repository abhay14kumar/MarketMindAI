import { useMemo, useState } from 'react';
import {
  BlockRounded,
  CloseRounded,
  LinkRounded,
  PlayArrowRounded,
  ScienceRounded,
  VisibilityRounded,
} from '@mui/icons-material';
import {
  Alert,
  Box,
  Button,
  Chip,
  Drawer,
  Grid,
  IconButton,
  MenuItem,
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
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { discoveryQueries, pipelineQueries } from '../api/client';
import type { DiscoveryJob, DiscoveryRunRequest } from '../api/types';
import { PageHeader } from '../components/PageHeader';
import { QueryState } from '../components/QueryState';
import { SectionCard } from '../components/SectionCard';
import { StatusChip } from '../components/StatusChip';
import { useNotifications } from '../notifications/NotificationProvider';
import { formatDateTime, formatEnum } from '../utils/format';

const sourceTypes: DiscoveryRunRequest['sourceType'][] = [
  'TEST_SOURCE', 'COMPANY_IR', 'NSE', 'BSE', 'SEBI', 'RBI',
];

function duration(job: DiscoveryJob) {
  if (!job.completedAt) return 'Running';
  return `${Math.max(0, new Date(job.completedAt).getTime()
    - new Date(job.startedAt).getTime())} ms`;
}

export function DiscoveryPage() {
  const queryClient = useQueryClient();
  const { notify } = useNotifications();
  const [sourceType, setSourceType] = useState<DiscoveryRunRequest['sourceType']>('COMPANY_IR');
  const [sourceUrl, setSourceUrl] = useState('');
  const [companySymbol, setCompanySymbol] = useState('');
  const [maxDocuments, setMaxDocuments] = useState(20);
  const [lastResult, setLastResult] = useState<DiscoveryJob | null>(null);
  const [selectedJobId, setSelectedJobId] = useState<string | null>(null);

  const jobsQuery = useQuery({
    queryKey: ['discovery-jobs'],
    queryFn: () => discoveryQueries.jobs(0, 100),
    refetchInterval: 5_000,
  });
  const documentsQuery = useQuery({
    queryKey: ['discovered-documents'],
    queryFn: () => discoveryQueries.documents(0, 100),
    refetchInterval: 5_000,
  });
  const pipelinesQuery = useQuery({
    queryKey: ['pipeline-jobs'],
    queryFn: () => pipelineQueries.jobs(0, 100),
    refetchInterval: 5_000,
  });
  const detailQuery = useQuery({
    queryKey: ['discovery-job', selectedJobId],
    queryFn: () => discoveryQueries.job(selectedJobId!),
    enabled: selectedJobId !== null,
  });

  const refreshDiscovery = () => Promise.all([
    queryClient.invalidateQueries({ queryKey: ['discovery-jobs'] }),
    queryClient.invalidateQueries({ queryKey: ['discovered-documents'] }),
    queryClient.invalidateQueries({ queryKey: ['pipeline-jobs'] }),
  ]);
  const runMutation = useMutation({
    mutationFn: discoveryQueries.run,
    onMutate: () => notify({
      title: 'Discovery started',
      message: 'The crawler is scanning the selected source in the background.',
      severity: 'info',
      path: '/discovery',
    }),
    onSuccess: async (job) => {
      setLastResult(job);
      await refreshDiscovery();
      notify({
        title: job.totalDiscovered === 0
          ? 'Discovery completed with no documents'
          : 'Discovery completed',
        message: job.message ?? `Found ${job.totalDiscovered} documents.`,
        severity: job.totalDiscovered === 0 ? 'warning' : 'success',
        path: '/discovery',
      });
    },
    onError: (error) => notify({
      title: 'Discovery failed',
      message: error instanceof Error ? error.message : 'Discovery failed.',
      severity: 'error',
      path: '/discovery',
    }),
  });
  const ignoreMutation = useMutation({
    mutationFn: discoveryQueries.ignore,
    onSuccess: refreshDiscovery,
  });
  const ingestMutation = useMutation({
    mutationFn: (discoveredDocumentId: string) =>
      pipelineQueries.start({ discoveredDocumentId }),
    onSuccess: async () => {
      await refreshDiscovery();
      notify({
        title: 'Pipeline started',
        message: 'The discovered document is now moving through ingestion.',
        severity: 'info',
        path: '/pipeline',
      });
    },
  });

  const jobs = useMemo(() => jobsQuery.data?.content ?? [], [jobsQuery.data]);
  const documents = useMemo(
    () => documentsQuery.data?.content ?? [],
    [documentsQuery.data],
  );
  const pipelines = useMemo(
    () => pipelinesQuery.data?.content ?? [],
    [pipelinesQuery.data],
  );
  const summary = useMemo(() => ({
    total: jobs.length,
    successful: jobs.filter((job) => job.status === 'COMPLETED' && job.totalDiscovered > 0).length,
    noResults: jobs.filter((job) => job.status === 'COMPLETED' && job.totalDiscovered === 0).length,
    failed: jobs.filter((job) => job.status === 'FAILED').length,
    newDocuments: jobs.reduce((sum, job) => sum + job.newDocuments, 0),
    existing: jobs.reduce((sum, job) => sum + job.existingDocuments, 0),
    pipelines: pipelines.length,
  }), [jobs, pipelines]);
  const detail = detailQuery.data;
  const relatedPipelines = detail
    ? pipelines.filter((pipeline) => documents.some((document) =>
      document.id === pipeline.discoveredDocumentId
      && document.sourceType === detail.job.sourceType))
    : [];

  const run = (payload: DiscoveryRunRequest) => runMutation.mutate(payload);

  return (
    <>
      <PageHeader
        eyebrow="Official-source intelligence"
        title="Document discovery"
        description="See what each crawler reached, scanned, found, and why a successful run may still produce no documents."
      />

      {lastResult && (
        <Alert
          severity={lastResult.status === 'FAILED'
            ? 'error'
            : lastResult.totalDiscovered === 0 ? 'warning' : 'success'}
          sx={{ mb: 2 }}
        >
          <Typography fontWeight={700}>
            {lastResult.totalDiscovered === 0
              ? 'Discovery completed but no documents were found'
              : lastResult.message}
          </Typography>
          {lastResult.totalDiscovered === 0 && (
            <>
              <Typography variant="body2" mt={0.5}>
                The source was reached, but no direct PDF links were discovered. The page may
                be dynamic, protected, or require a source-specific crawler.
              </Typography>
              <Typography variant="caption">
                Next: try Test Source, use a direct company reports page, validate the source,
                or plan the NSE-specific crawler.
              </Typography>
            </>
          )}
        </Alert>
      )}

      <Grid container spacing={2} mb={2}>
        {[
          ['Total Jobs', summary.total],
          ['Successful Jobs', summary.successful],
          ['Jobs With No Results', summary.noResults],
          ['Failed Jobs', summary.failed],
          ['New Documents', summary.newDocuments],
          ['Existing Documents', summary.existing],
          ['Pipeline Jobs Created', summary.pipelines],
        ].map(([label, value]) => (
          <Grid key={label} size={{ xs: 6, md: 3, xl: 12 / 7 }}>
            <SectionCard eyebrow="Background work" title={String(value)} minHeight={120}>
              <Typography color="text.secondary">{label}</Typography>
            </SectionCard>
          </Grid>
        ))}
      </Grid>

      <SectionCard eyebrow="Crawler control" title="Run discovery">
        <Stack direction={{ xs: 'column', lg: 'row' }} gap={1.5} alignItems={{ lg: 'center' }}>
          <TextField
            select
            label="Source type"
            value={sourceType}
            onChange={(event) => setSourceType(event.target.value as DiscoveryRunRequest['sourceType'])}
            sx={{ minWidth: 180 }}
          >
            {sourceTypes.map((type) => <MenuItem key={type} value={type}>{formatEnum(type)}</MenuItem>)}
          </TextField>
          <TextField
            label="Source URL"
            value={sourceUrl}
            onChange={(event) => setSourceUrl(event.target.value)}
            placeholder="https://company.example/investors/reports"
            sx={{ flex: 1, minWidth: 280 }}
          />
          <TextField
            label="Company symbol"
            value={companySymbol}
            onChange={(event) => setCompanySymbol(event.target.value.toUpperCase())}
            sx={{ width: 170 }}
          />
          <TextField
            label="Max documents"
            type="number"
            value={maxDocuments}
            onChange={(event) => setMaxDocuments(Number(event.target.value))}
            slotProps={{ htmlInput: { min: 1, max: 100 } }}
            sx={{ width: 150 }}
          />
          <Button
            variant="outlined"
            startIcon={<ScienceRounded />}
            disabled={runMutation.isPending}
            onClick={() => run({ sourceType: 'TEST_SOURCE', maxDocuments })}
          >
            Run Test Discovery
          </Button>
          <Button
            variant="contained"
            startIcon={<PlayArrowRounded />}
            disabled={runMutation.isPending || sourceUrl.trim() === ''}
            onClick={() => run({
              sourceType,
              sourceUrl: sourceUrl.trim(),
              companySymbol: companySymbol || undefined,
              maxDocuments,
            })}
          >
            Run URL Discovery
          </Button>
        </Stack>
      </SectionCard>

      <Stack gap={2} mt={2}>
        <SectionCard eyebrow="Discovery operations" title="Jobs">
          <QueryState
            loading={jobsQuery.isPending}
            error={jobsQuery.error instanceof Error ? jobsQuery.error : null}
            empty={jobs.length === 0}
            emptyMessage="No discovery jobs yet. Run Test Discovery to validate the full flow with known public PDFs."
            onRetry={() => void jobsQuery.refetch()}
          >
            <TableContainer>
              <Table sx={{ minWidth: 1450 }}>
                <TableHead>
                  <TableRow>
                    <TableCell>Job</TableCell><TableCell>Source</TableCell>
                    <TableCell>URL</TableCell><TableCell>Status</TableCell>
                    <TableCell>Total</TableCell><TableCell>New</TableCell>
                    <TableCell>Existing</TableCell><TableCell>Failed</TableCell>
                    <TableCell>Crawler</TableCell><TableCell>Message</TableCell>
                    <TableCell>Started</TableCell><TableCell>Duration</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {jobs.map((job) => (
                    <TableRow key={job.discoveryJobId} hover>
                      <TableCell>{job.discoveryJobId.slice(0, 8)}</TableCell>
                      <TableCell>{formatEnum(job.sourceType)}</TableCell>
                      <TableCell sx={{ maxWidth: 220 }}><Typography noWrap>{job.sourceUrl ?? 'Test corpus'}</Typography></TableCell>
                      <TableCell><StatusChip label={job.totalDiscovered === 0 && job.status === 'COMPLETED' ? 'NO_RESULTS' : job.status} /></TableCell>
                      <TableCell>{job.totalDiscovered}</TableCell><TableCell>{job.newDocuments}</TableCell>
                      <TableCell>{job.existingDocuments}</TableCell><TableCell>{job.failedSources}</TableCell>
                      <TableCell>{formatEnum(job.crawlerTypeUsed ?? 'UNKNOWN')}</TableCell>
                      <TableCell sx={{ maxWidth: 300 }}><Typography variant="body2">{job.message ?? '—'}</Typography></TableCell>
                      <TableCell>{formatDateTime(job.startedAt)}</TableCell><TableCell>{duration(job)}</TableCell>
                      <TableCell align="right">
                        <Button size="small" startIcon={<VisibilityRounded />} onClick={() => setSelectedJobId(job.discoveryJobId)}>
                          View Details
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </QueryState>
        </SectionCard>

        <SectionCard eyebrow="Metadata catalog" title="Discovered documents">
          <QueryState
            loading={documentsQuery.isPending}
            error={documentsQuery.error instanceof Error ? documentsQuery.error : null}
            empty={documents.length === 0}
            emptyMessage="No document metadata exists yet. A completed job with zero results means the source was scanned but exposed no direct PDF links."
            onRetry={() => void documentsQuery.refetch()}
          >
            <TableContainer>
              <Table sx={{ minWidth: 1300 }}>
                <TableHead><TableRow>
                  <TableCell>Title</TableCell><TableCell>Source</TableCell><TableCell>Company</TableCell>
                  <TableCell>Type</TableCell><TableCell>Status</TableCell><TableCell>First discovered</TableCell>
                  <TableCell>Last seen</TableCell><TableCell>Seen</TableCell><TableCell>URL</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow></TableHead>
                <TableBody>
                  {documents.map((document) => (
                    <TableRow key={document.id} hover>
                      <TableCell><Typography fontWeight={650}>{document.title}</Typography></TableCell>
                      <TableCell>{formatEnum(document.sourceType)}</TableCell>
                      <TableCell>{document.companySymbol ?? '—'}</TableCell>
                      <TableCell>{formatEnum(document.documentType)}</TableCell>
                      <TableCell><StatusChip label={document.status} /></TableCell>
                      <TableCell>{formatDateTime(document.firstDiscoveredAt)}</TableCell>
                      <TableCell>{formatDateTime(document.lastSeenAt)}</TableCell>
                      <TableCell>{document.seenCount}</TableCell>
                      <TableCell>
                        <Button component="a" href={document.documentUrl} target="_blank" startIcon={<LinkRounded />}>View URL</Button>
                      </TableCell>
                      <TableCell align="right">
                        <Stack direction="row" justifyContent="flex-end">
                          <Button
                            size="small"
                            startIcon={<PlayArrowRounded />}
                            disabled={ingestMutation.isPending || document.status === 'IGNORED'}
                            onClick={() => ingestMutation.mutate(document.id)}
                          >
                            Start Pipeline
                          </Button>
                          <Button
                            size="small"
                            color="inherit"
                            startIcon={<BlockRounded />}
                            disabled={document.status === 'IGNORED'}
                            onClick={() => ignoreMutation.mutate(document.id)}
                          >
                            Ignore
                          </Button>
                        </Stack>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </QueryState>
        </SectionCard>
      </Stack>

      <Drawer
        anchor="right"
        open={selectedJobId !== null}
        onClose={() => setSelectedJobId(null)}
        slotProps={{ paper: { sx: { width: { xs: '100%', md: 560 }, p: 3 } } }}
      >
        <Stack direction="row" justifyContent="space-between">
          <Box>
            <Typography variant="overline" color="primary.main">Discovery evidence</Typography>
            <Typography variant="h2">Job details</Typography>
          </Box>
          <IconButton onClick={() => setSelectedJobId(null)}><CloseRounded /></IconButton>
        </Stack>
        {detail && (
          <Stack gap={2} mt={3}>
            <StatusChip label={detail.job.totalDiscovered === 0 ? 'NO_RESULTS' : detail.job.status} />
            <Typography>{detail.job.message}</Typography>
            {detail.job.reasonWhenZeroResults && <Alert severity="warning">{detail.job.reasonWhenZeroResults}</Alert>}
            <Typography color="text.secondary">{detail.job.recommendation}</Typography>
            {detail.sourceRuns.map((run) => (
              <SectionCard key={run.id} eyebrow={run.crawlerType ?? 'Crawler'} title={formatEnum(run.sourceType)}>
                <Stack gap={0.8}>
                  <Typography>HTTP status: {run.httpStatus ?? 'Not applicable'}</Typography>
                  <Typography>Fetched HTML: {run.fetchedHtmlBytes.toLocaleString()} bytes</Typography>
                  <Typography>Links scanned: {run.totalLinksFound}</Typography>
                  <Typography>PDF links found: {run.pdfLinksFound}</Typography>
                  <Typography>Skipped links: {run.skippedLinksCount}</Typography>
                  {run.errorMessage && <Alert severity="error">{run.errorMessage}</Alert>}
                </Stack>
              </SectionCard>
            ))}
            <Typography variant="overline">Related pipeline jobs</Typography>
            {relatedPipelines.length === 0
              ? <Typography color="text.secondary">No pipeline jobs are linked yet. Discovery and ingestion are separate actions.</Typography>
              : relatedPipelines.map((pipeline) => (
                <Chip key={pipeline.id} label={`${pipeline.id.slice(0, 8)} · ${formatEnum(pipeline.status)}`} />
              ))}
          </Stack>
        )}
      </Drawer>
    </>
  );
}
