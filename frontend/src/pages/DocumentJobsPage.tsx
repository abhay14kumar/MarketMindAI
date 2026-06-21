import { useQuery } from '@tanstack/react-query';
import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography } from '@mui/material';
import { documentQueries } from '../api/client';
import { PageHeader } from '../components/PageHeader';
import { QueryState } from '../components/QueryState';
import { SectionCard } from '../components/SectionCard';
import { StatusChip } from '../components/StatusChip';
import { formatDateTime } from '../utils/format';

export function DocumentJobsPage() {
  const query = useQuery({ queryKey: ['document-jobs'], queryFn: documentQueries.jobs });
  const jobs = query.data?.data ?? [];

  return (
    <>
      <PageHeader
        eyebrow="Acquisition operations"
        title="Download jobs"
        description="Document acquisition attempts, retries, completion state, and safe failure details."
      />
      <SectionCard eyebrow="Pipeline queue" title="Jobs">
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
                  <TableCell>Requested URL</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Attempts</TableCell>
                  <TableCell>Submitted</TableCell>
                  <TableCell>Completed</TableCell>
                  <TableCell>Error</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {jobs.map((job) => (
                  <TableRow key={job.id} hover>
                    <TableCell sx={{ maxWidth: 360 }}>
                      <Typography noWrap title={job.requestedUrl}>{job.requestedUrl}</Typography>
                    </TableCell>
                    <TableCell><StatusChip label={job.status} /></TableCell>
                    <TableCell>{job.attemptCount} / {job.maxAttempts}</TableCell>
                    <TableCell>{formatDateTime(job.submittedAt)}</TableCell>
                    <TableCell>{formatDateTime(job.completedAt)}</TableCell>
                    <TableCell>{job.errorMessage ?? '—'}</TableCell>
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
