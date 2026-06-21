import { useQuery } from '@tanstack/react-query';
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { ExpandMoreRounded } from '@mui/icons-material';
import { portfolioQueries } from '../api/client';
import { PageHeader } from '../components/PageHeader';
import { QueryState } from '../components/QueryState';
import { SectionCard } from '../components/SectionCard';
import { StatusChip } from '../components/StatusChip';
import { formatDateTime } from '../utils/format';

export function PortfolioImportHistoryPage() {
  const query = useQuery({
    queryKey: ['portfolio', 'import-jobs'],
    queryFn: () => portfolioQueries.importJobs(),
  });
  const jobs = query.data?.content ?? [];

  return (
    <>
      <PageHeader
        eyebrow="Portfolio operations"
        title="Import History"
        description="Auditable Zerodha XLSX imports, row counts, and row-level validation results."
      />
      <SectionCard eyebrow="Ingestion audit" title="Import jobs">
        <QueryState
          loading={query.isPending}
          error={query.error instanceof Error ? query.error : null}
          empty={jobs.length === 0}
          onRetry={() => void query.refetch()}
          emptyMessage="No portfolio imports have been submitted."
        >
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>File</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="right">Rows</TableCell>
                  <TableCell align="right">Imported</TableCell>
                  <TableCell align="right">Rejected</TableCell>
                  <TableCell>Started</TableCell>
                  <TableCell>Completed</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {jobs.map((job) => (
                  <TableRow key={job.id} hover>
                    <TableCell><Typography fontWeight={650}>{job.originalFileName}</Typography></TableCell>
                    <TableCell><StatusChip label={job.status} /></TableCell>
                    <TableCell align="right">{job.totalRows}</TableCell>
                    <TableCell align="right">{job.importedRows}</TableCell>
                    <TableCell align="right">{job.rejectedRows}</TableCell>
                    <TableCell>{formatDateTime(job.startedAt)}</TableCell>
                    <TableCell>{formatDateTime(job.completedAt)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          {jobs.some((job) => job.rowErrors.length > 0 || job.errorMessage) && (
            <Stack gap={1}>
              <Typography variant="overline" color="text.secondary">Failure details</Typography>
              {jobs.filter((job) => job.rowErrors.length > 0 || job.errorMessage).map((job) => (
                <Accordion key={job.id} disableGutters>
                  <AccordionSummary expandIcon={<ExpandMoreRounded />}>
                    <Typography>{job.originalFileName} · {job.rejectedRows || 1} issue(s)</Typography>
                  </AccordionSummary>
                  <AccordionDetails>
                    <Stack gap={0.75}>
                      {job.errorMessage && <Typography color="error.main">{job.errorMessage}</Typography>}
                      {job.rowErrors.map((error) => (
                        <Typography key={`${job.id}-${error.rowNumber}`} variant="body2" color="text.secondary">
                          Row {error.rowNumber}: {error.message}
                        </Typography>
                      ))}
                    </Stack>
                  </AccordionDetails>
                </Accordion>
              ))}
            </Stack>
          )}
        </QueryState>
      </SectionCard>
    </>
  );
}
