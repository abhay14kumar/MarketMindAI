import { useQuery } from '@tanstack/react-query';
import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography } from '@mui/material';
import { schedulerQueries } from '../api/client';
import { PageHeader } from '../components/PageHeader';
import { QueryState } from '../components/QueryState';
import { SectionCard } from '../components/SectionCard';
import { StatusChip } from '../components/StatusChip';
import { formatDateTime, formatEnum } from '../utils/format';

export function SchedulerPage() {
  const query = useQuery({ queryKey: ['scheduler-jobs'], queryFn: schedulerQueries.jobs });
  const jobs = query.data?.data ?? [];

  return (
    <>
      <PageHeader
        eyebrow="Ingestion control plane"
        title="Scheduler"
        description="Configured ingestion schedules and their current operational state."
      />
      <SectionCard eyebrow="Job registry" title="Scheduler jobs">
        <QueryState
          loading={query.isPending}
          error={query.error instanceof Error ? query.error : null}
          empty={jobs.length === 0}
          fallbackMessage={query.data?.fallbackReason}
          onRetry={() => void query.refetch()}
        >
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Job</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Schedule</TableCell>
                  <TableCell>Next run</TableCell>
                  <TableCell>Last run</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {jobs.map((job) => (
                  <TableRow key={job.id} hover>
                    <TableCell>
                      <Typography fontWeight={650}>{job.name}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        {job.description ?? 'No description'}
                      </Typography>
                    </TableCell>
                    <TableCell>{formatEnum(job.schedulerType)}</TableCell>
                    <TableCell><StatusChip label={job.status} /></TableCell>
                    <TableCell>
                      <Typography fontFamily='"IBM Plex Mono", monospace' fontSize="0.75rem">
                        {job.cronExpression}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">{job.timeZone}</Typography>
                    </TableCell>
                    <TableCell>{formatDateTime(job.nextRunAt)}</TableCell>
                    <TableCell>{formatDateTime(job.lastRunAt)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </QueryState>
      </SectionCard>
    </>
  );
}
