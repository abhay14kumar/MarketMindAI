import { PlayArrowRounded } from '@mui/icons-material';
import {
  Alert,
  Button,
  Chip,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { schedulerQueries } from '../api/client';
import { PageHeader } from '../components/PageHeader';
import { QueryState } from '../components/QueryState';
import { SectionCard } from '../components/SectionCard';
import { StatusChip } from '../components/StatusChip';
import { useNotifications } from '../notifications/NotificationProvider';
import { formatDateTime, formatEnum } from '../utils/format';

export function SchedulerPage() {
  const queryClient = useQueryClient();
  const { notify } = useNotifications();
  const jobsQuery = useQuery({
    queryKey: ['scheduler-jobs'],
    queryFn: schedulerQueries.jobs,
    refetchInterval: 5_000,
  });
  const runsQuery = useQuery({
    queryKey: ['scheduler-runs'],
    queryFn: schedulerQueries.runs,
    refetchInterval: 5_000,
  });
  const trigger = useMutation({
    mutationFn: schedulerQueries.trigger,
    onSuccess: async (run) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['scheduler-jobs'] }),
        queryClient.invalidateQueries({ queryKey: ['scheduler-runs'] }),
        queryClient.invalidateQueries({ queryKey: ['discovery-jobs'] }),
      ]);
      notify({
        title: run.status === 'FAILED' ? 'Scheduler run failed' : 'Scheduler run completed',
        message: run.resultSummary ?? run.errorMessage ?? 'The run finished.',
        severity: run.status === 'FAILED' ? 'error' : run.discoveredDocumentsCount === 0 ? 'warning' : 'success',
        path: '/scheduler',
      });
    },
  });
  const jobs = jobsQuery.data?.data ?? [];
  const runs = runsQuery.data?.content ?? [];

  return (
    <>
      <PageHeader
        eyebrow="Ingestion control plane"
        title="Scheduler"
        description="Each job identifies whether it is seeded, manually executable, partially wired, or backed by a real schedule."
      />
      {trigger.error instanceof Error && <Alert severity="error" sx={{ mb: 2 }}>{trigger.error.message}</Alert>}
      <Stack gap={2}>
        <SectionCard eyebrow="Job registry" title="Scheduler jobs">
          <QueryState
            loading={jobsQuery.isPending}
            error={jobsQuery.error instanceof Error ? jobsQuery.error : null}
            empty={jobs.length === 0}
            fallbackMessage={jobsQuery.data?.fallbackReason}
            emptyMessage="No scheduler metadata exists. Create a job and explicitly choose how it will execute."
            onRetry={() => void jobsQuery.refetch()}
          >
            <TableContainer>
              <Table sx={{ minWidth: 1450 }}>
                <TableHead><TableRow>
                  <TableCell>Job</TableCell><TableCell>Type</TableCell><TableCell>Status</TableCell>
                  <TableCell>Execution</TableCell><TableCell>Implementation</TableCell>
                  <TableCell>Schedule</TableCell><TableCell>Next run</TableCell><TableCell>Last run</TableCell>
                  <TableCell>Last result</TableCell><TableCell>Documents</TableCell>
                  <TableCell>Pipelines</TableCell><TableCell align="right">Action</TableCell>
                </TableRow></TableHead>
                <TableBody>
                  {jobs.map((job) => (
                    <TableRow key={job.id} hover>
                      <TableCell>
                        <Typography fontWeight={650}>{job.name}</Typography>
                        <Typography variant="caption" color="text.secondary">{job.description ?? 'No description'}</Typography>
                      </TableCell>
                      <TableCell>{formatEnum(job.schedulerType)}</TableCell>
                      <TableCell><StatusChip label={job.status} /></TableCell>
                      <TableCell><Chip size="small" label={formatEnum(job.executionMode ?? 'MOCK')} variant="outlined" /></TableCell>
                      <TableCell><StatusChip label={formatEnum(job.implementationStatus ?? 'NOT_IMPLEMENTED')} /></TableCell>
                      <TableCell>
                        <Typography fontFamily='"IBM Plex Mono", monospace' fontSize="0.75rem">{job.cronExpression}</Typography>
                        <Typography variant="caption" color="text.secondary">{job.timeZone}</Typography>
                      </TableCell>
                      <TableCell>
                        {formatDateTime(job.nextRunAt)}
                        {job.nextRunSeeded && <Chip size="small" label="Seeded / mock" color="warning" sx={{ ml: 1 }} />}
                      </TableCell>
                      <TableCell>
                        {formatDateTime(job.lastRunAt)}
                        {job.lastRunSeeded && <Chip size="small" label="Seeded / mock" color="warning" sx={{ ml: 1 }} />}
                      </TableCell>
                      <TableCell sx={{ maxWidth: 340 }}>
                        {job.lastRunStatus && <StatusChip label={job.lastRunStatus} />}
                        <Typography variant="body2" color="text.secondary" mt={0.5}>{job.lastRunMessage ?? 'Never executed'}</Typography>
                        {job.lastRunDurationMs != null && <Typography variant="caption">{job.lastRunDurationMs} ms</Typography>}
                      </TableCell>
                      <TableCell>{job.createdDocumentsCount ?? 0}</TableCell>
                      <TableCell>{job.pipelineJobsCreatedCount ?? 0}</TableCell>
                      <TableCell align="right">
                        <Button
                          variant="outlined"
                          size="small"
                          startIcon={<PlayArrowRounded />}
                          disabled={trigger.isPending || job.status !== 'ACTIVE'}
                          onClick={() => trigger.mutate(job.id)}
                        >
                          Run Now
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </QueryState>
        </SectionCard>

        <SectionCard eyebrow="Execution evidence" title="Run history">
          <QueryState
            loading={runsQuery.isPending}
            error={runsQuery.error instanceof Error ? runsQuery.error : null}
            empty={runs.length === 0}
            emptyMessage="No runs have been recorded. Run an active job to see duration, results, and downstream counts."
            onRetry={() => void runsQuery.refetch()}
          >
            <TableContainer>
              <Table>
                <TableHead><TableRow>
                  <TableCell>Run</TableCell><TableCell>Job</TableCell><TableCell>Status</TableCell>
                  <TableCell>Started</TableCell><TableCell>Completed</TableCell><TableCell>Duration</TableCell>
                  <TableCell>Result</TableCell><TableCell>Discovered</TableCell><TableCell>Pipelines</TableCell>
                </TableRow></TableHead>
                <TableBody>
                  {runs.map((run) => (
                    <TableRow key={run.id} hover>
                      <TableCell>{run.id.slice(0, 8)}</TableCell>
                      <TableCell>{run.schedulerJobId.slice(0, 8)}</TableCell>
                      <TableCell><StatusChip label={run.status} /></TableCell>
                      <TableCell>{formatDateTime(run.startedAt)}</TableCell>
                      <TableCell>{formatDateTime(run.completedAt)}</TableCell>
                      <TableCell>{run.durationMs} ms</TableCell>
                      <TableCell>{run.resultSummary ?? run.errorMessage ?? '—'}</TableCell>
                      <TableCell>{run.discoveredDocumentsCount}</TableCell>
                      <TableCell>{run.pipelineJobsCreatedCount}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </QueryState>
        </SectionCard>
      </Stack>
    </>
  );
}
