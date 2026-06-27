import { useEffect, useMemo, useRef, useState } from 'react';
import {
  PlayArrowRounded,
  RefreshRounded,
  ReplayRounded,
} from '@mui/icons-material';
import {
  Alert,
  Box,
  Button,
  Divider,
  Grid,
  LinearProgress,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { pipelineQueries } from '../api/client';
import type { AutonomousPipelineJob } from '../api/types';
import { MetricCard } from '../components/MetricCard';
import { PageHeader } from '../components/PageHeader';
import { QueryState } from '../components/QueryState';
import { SectionCard } from '../components/SectionCard';
import { StatusChip } from '../components/StatusChip';
import { formatDateTime, formatEnum, formatNumber } from '../utils/format';
import { useNotifications } from '../notifications/NotificationProvider';

const stageTimeline = [
  'DISCOVERY', 'DOWNLOAD', 'TEXT_EXTRACTION', 'CHUNKING',
  'EMBEDDING', 'QDRANT_INDEXING', 'AI_SUMMARY', 'AI_READY',
];

export function PipelineMonitorPage() {
  const queryClient = useQueryClient();
  const { notify } = useNotifications();
  const previousStatuses = useRef<Record<string, string>>({});
  const [documentId, setDocumentId] = useState('');
  const [discoveredDocumentId, setDiscoveredDocumentId] = useState('');
  const jobsQuery = useQuery({
    queryKey: ['pipeline-jobs'],
    queryFn: () => pipelineQueries.jobs(0, 100),
    refetchInterval: 5_000,
  });
  const metricsQuery = useQuery({
    queryKey: ['pipeline-metrics'],
    queryFn: pipelineQueries.metrics,
    refetchInterval: 5_000,
  });
  const refresh = () => Promise.all([
    queryClient.invalidateQueries({ queryKey: ['pipeline-jobs'] }),
    queryClient.invalidateQueries({ queryKey: ['pipeline-metrics'] }),
  ]);
  const start = useMutation({
    mutationFn: pipelineQueries.start,
    onSuccess: async () => {
      await refresh();
      notify({
        title: 'Pipeline started',
        message: 'The document pipeline is now running.',
        severity: 'info',
        path: '/pipeline',
      });
    },
  });
  const retry = useMutation({
    mutationFn: pipelineQueries.retryJob,
    onSuccess: refresh,
  });
  const jobs = useMemo(() => jobsQuery.data?.content ?? [], [jobsQuery.data]);
  const metrics = metricsQuery.data;
  const error = jobsQuery.error ?? metricsQuery.error ?? start.error ?? retry.error;

  useEffect(() => {
    jobs.forEach((job) => {
      const previous = previousStatuses.current[job.id];
      if (previous && previous !== job.status && ['COMPLETED', 'FAILED'].includes(job.status)) {
        notify({
          title: job.status === 'COMPLETED' ? 'Pipeline completed' : 'Pipeline failed',
          message: job.status === 'COMPLETED'
            ? `Pipeline ${job.id.slice(0, 8)} is AI ready.`
            : job.errorMessage ?? `Pipeline ${job.id.slice(0, 8)} failed.`,
          severity: job.status === 'COMPLETED' ? 'success' : 'error',
          path: '/pipeline',
        });
      }
      previousStatuses.current[job.id] = job.status;
    });
  }, [jobs, notify]);

  return (
    <>
      <PageHeader
        eyebrow="Autonomous document intelligence"
        title="Pipeline orchestrator"
        description="Track discovery, download, extraction, chunking, embedding, Qdrant indexing, summarization, and AI readiness."
        action={(
          <Button
            variant="outlined"
            startIcon={<RefreshRounded />}
            onClick={() => {
              void jobsQuery.refetch();
              void metricsQuery.refetch();
            }}
          >
            Refresh
          </Button>
        )}
      />

      {error instanceof Error && (
        <Alert severity="error" sx={{ mb: 2 }}>{error.message}</Alert>
      )}

      <Grid container spacing={2} mb={2}>
        <Grid size={{ xs: 6, lg: 3 }}>
          <MetricCard label="Total jobs" value={formatNumber(metrics?.totalJobs)} change="All time" />
        </Grid>
        <Grid size={{ xs: 6, lg: 3 }}>
          <MetricCard label="Running" value={formatNumber(metrics?.runningJobs)} change="Queued and active" />
        </Grid>
        <Grid size={{ xs: 6, lg: 3 }}>
          <MetricCard
            label="Success rate"
            value={`${((metrics?.successRate ?? 0) * 100).toFixed(1)}%`}
            change="Completed jobs"
          />
        </Grid>
        <Grid size={{ xs: 6, lg: 3 }}>
          <MetricCard
            label="Average duration"
            value={`${formatNumber(metrics?.averageDurationMs, 0)} ms`}
            change="Completed executions"
          />
        </Grid>
      </Grid>

      <SectionCard eyebrow="Manual control" title="Start pipeline">
        <Stack direction={{ xs: 'column', md: 'row' }} gap={2}>
          <TextField
            fullWidth
            label="Existing document ID"
            value={documentId}
            onChange={(event) => setDocumentId(event.target.value)}
          />
          <TextField
            fullWidth
            label="Discovered document ID"
            value={discoveredDocumentId}
            onChange={(event) => setDiscoveredDocumentId(event.target.value)}
          />
          <Button
            variant="contained"
            startIcon={<PlayArrowRounded />}
            disabled={start.isPending
              || (documentId.trim() === '') === (discoveredDocumentId.trim() === '')}
            onClick={() => start.mutate(documentId.trim()
              ? { documentId: documentId.trim() }
              : { discoveredDocumentId: discoveredDocumentId.trim() })}
            sx={{ minWidth: 150 }}
          >
            Start
          </Button>
        </Stack>
      </SectionCard>

      <Box mt={2}>
        <SectionCard eyebrow="Live execution" title="Pipeline jobs">
          <QueryState
            loading={jobsQuery.isPending}
            error={jobsQuery.error instanceof Error ? jobsQuery.error : null}
            empty={jobs.length === 0}
            onRetry={() => void jobsQuery.refetch()}
          >
            <Stack gap={2}>
              {jobs.map((job) => (
                <PipelineJobCard
                  key={job.id}
                  summary={job}
                  retrying={retry.isPending && retry.variables === job.id}
                  onRetry={() => retry.mutate(job.id)}
                />
              ))}
            </Stack>
          </QueryState>
        </SectionCard>
      </Box>
    </>
  );
}

function PipelineJobCard({
  summary,
  retrying,
  onRetry,
}: {
  summary: AutonomousPipelineJob;
  retrying: boolean;
  onRetry: () => void;
}) {
  const [showEvents, setShowEvents] = useState(false);
  const details = useQuery({
    queryKey: ['pipeline-job', summary.id],
    queryFn: () => pipelineQueries.job(summary.id),
    refetchInterval: ['QUEUED', 'RUNNING'].includes(summary.status) ? 5_000 : false,
  });
  const job = details.data ?? summary;

  return (
    <Box sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 2, p: 2 }}>
      <Stack
        direction={{ xs: 'column', md: 'row' }}
        justifyContent="space-between"
        gap={2}
      >
        <Box>
          <Typography fontWeight={700}>Job {job.id}</Typography>
          <Typography variant="caption" color="text.secondary">
            Correlation {job.correlationId} · Started {formatDateTime(job.startedAt)}
          </Typography>
        </Box>
        <Stack direction="row" gap={1} alignItems="center">
          <StatusChip label={formatEnum(job.status)} />
          <Typography variant="caption" color="text.secondary">
            {formatEnum(job.currentStage)}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {job.startedAt && job.completedAt
              ? `${Math.max(0, new Date(job.completedAt).getTime()
                - new Date(job.startedAt).getTime())} ms`
              : job.startedAt ? 'In progress' : 'Waiting'}
          </Typography>
          <Button size="small" onClick={() => setShowEvents((current) => !current)}>
            {showEvents ? 'Hide events' : 'View events'}
          </Button>
          {['FAILED', 'PARTIAL'].includes(job.status) && (
            <Button
              size="small"
              variant="outlined"
              startIcon={<ReplayRounded />}
              disabled={retrying}
              onClick={onRetry}
            >
              {retrying ? 'Retrying…' : 'Retry'}
            </Button>
          )}
        </Stack>
      </Stack>

      <Stack direction="row" justifyContent="space-between" mt={2} mb={0.75}>
        <Typography variant="caption" color="text.secondary">Progress</Typography>
        <Typography variant="caption">{job.progressPercent}%</Typography>
      </Stack>
      <LinearProgress variant="determinate" value={job.progressPercent} />
      <Stack direction="row" gap={0.75} flexWrap="wrap" mt={2}>
        {stageTimeline.map((stageName) => {
          const stage = job.stages.find((item) => item.stageName === stageName);
          const status = stage?.status
            ?? (job.currentStage === stageName ? 'RUNNING' : 'WAITING');
          return (
            <Stack key={stageName} direction="row" gap={0.75} alignItems="center">
              <StatusChip label={status} />
              <Typography variant="caption">{formatEnum(stageName)}</Typography>
              {stageName !== stageTimeline.at(-1) && <Typography color="text.secondary">→</Typography>}
            </Stack>
          );
        })}
      </Stack>
      {job.errorMessage && <Alert severity="error" sx={{ mt: 2 }}>{job.errorMessage}</Alert>}

      <Divider sx={{ my: 2 }} />
      <Grid container spacing={1}>
        {job.stages.map((stage) => (
          <Grid key={stage.id} size={{ xs: 12, sm: 6, lg: 3 }}>
            <Box sx={{
              p: 1.5,
              border: '1px solid',
              borderColor: 'divider',
              borderRadius: 1.5,
              height: '100%',
            }}>
              <Typography variant="overline">{formatEnum(stage.stageName)}</Typography>
              <Box mt={0.5}><StatusChip label={formatEnum(stage.status)} /></Box>
              <Typography variant="caption" color="text.secondary" display="block" mt={1}>
                {formatNumber(stage.durationMs, 0)} ms · Attempts {stage.attemptCount}/{stage.maxAttempts}
              </Typography>
              {stage.errorMessage && (
                <Typography variant="caption" color="error.main">
                  {stage.errorMessage}
                </Typography>
              )}
            </Box>
          </Grid>
        ))}
      </Grid>

      {showEvents && job.events.length > 0 && (
        <>
          <Divider sx={{ my: 2 }} />
          <Typography variant="overline" color="text.secondary">Events</Typography>
          <Stack gap={0.75} mt={1}>
            {job.events.slice().reverse().map((event) => (
              <Stack key={event.id} direction="row" gap={1} alignItems="baseline">
                <Typography variant="caption" color="text.secondary" minWidth={145}>
                  {formatDateTime(event.createdAt)}
                </Typography>
                <StatusChip label={formatEnum(event.eventType)} />
                <Typography variant="caption">{event.message}</Typography>
              </Stack>
            ))}
          </Stack>
        </>
      )}
    </Box>
  );
}
