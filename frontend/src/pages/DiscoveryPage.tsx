import { useState } from 'react';
import {
  BlockRounded,
  LinkRounded,
  PlayArrowRounded,
  ScienceRounded,
} from '@mui/icons-material';
import {
  Alert,
  Button,
  Grid,
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
import { discoveryQueries } from '../api/client';
import type { DiscoveryRunRequest } from '../api/types';
import { PageHeader } from '../components/PageHeader';
import { QueryState } from '../components/QueryState';
import { SectionCard } from '../components/SectionCard';
import { StatusChip } from '../components/StatusChip';
import { formatDateTime, formatEnum } from '../utils/format';

export function DiscoveryPage() {
  const queryClient = useQueryClient();
  const [sourceUrl, setSourceUrl] = useState('');
  const [companySymbol, setCompanySymbol] = useState('');
  const jobsQuery = useQuery({
    queryKey: ['discovery-jobs'],
    queryFn: () => discoveryQueries.jobs(0, 100),
  });
  const documentsQuery = useQuery({
    queryKey: ['discovered-documents'],
    queryFn: () => discoveryQueries.documents(0, 100),
  });
  const refreshDiscovery = () => Promise.all([
    queryClient.invalidateQueries({ queryKey: ['discovery-jobs'] }),
    queryClient.invalidateQueries({ queryKey: ['discovered-documents'] }),
  ]);
  const runMutation = useMutation({
    mutationFn: discoveryQueries.run,
    onSuccess: refreshDiscovery,
  });
  const ignoreMutation = useMutation({
    mutationFn: discoveryQueries.ignore,
    onSuccess: refreshDiscovery,
  });

  const run = (payload: DiscoveryRunRequest) => runMutation.mutate(payload);
  const jobs = jobsQuery.data?.content ?? [];
  const documents = documentsQuery.data?.content ?? [];
  const error = runMutation.error ?? ignoreMutation.error;

  return (
    <>
      <PageHeader
        eyebrow="Official-source intelligence"
        title="Document discovery"
        description="Scan trusted web pages for official PDF links and store classified metadata. Discovery never downloads or indexes documents."
      />

      {error instanceof Error && (
        <Alert severity="error" sx={{ mb: 2 }}>{error.message}</Alert>
      )}

      <Grid container spacing={2} mb={2}>
        <Grid size={{ xs: 12, lg: 4 }}>
          <SectionCard eyebrow="Local verification" title="Test discovery" minHeight={220}>
            <Typography color="text.secondary" mb={2}>
              Discover the deterministic test corpus, including Reliance’s annual report
              and the W3C dummy PDF.
            </Typography>
            <Button
              variant="contained"
              startIcon={<ScienceRounded />}
              disabled={runMutation.isPending}
              onClick={() => run({
                sourceType: 'TEST_SOURCE',
                maxDocuments: 20,
              })}
            >
              Run Test Discovery
            </Button>
          </SectionCard>
        </Grid>
        <Grid size={{ xs: 12, lg: 8 }}>
          <SectionCard eyebrow="Generic HTML crawler" title="URL discovery" minHeight={220}>
            <Stack gap={2}>
              <TextField
                label="Trusted source URL"
                placeholder="https://company.example/investors/reports"
                value={sourceUrl}
                onChange={(event) => setSourceUrl(event.target.value)}
              />
              <TextField
                label="Company symbol (optional)"
                placeholder="RELIANCE"
                value={companySymbol}
                onChange={(event) => setCompanySymbol(event.target.value)}
              />
              <Button
                variant="contained"
                startIcon={<PlayArrowRounded />}
                disabled={runMutation.isPending || sourceUrl.trim() === ''}
                onClick={() => run({
                  sourceType: 'COMPANY_IR',
                  sourceUrl: sourceUrl.trim(),
                  companySymbol: companySymbol.trim() || undefined,
                  maxDocuments: 20,
                })}
                sx={{ alignSelf: 'flex-start' }}
              >
                Run URL Discovery
              </Button>
            </Stack>
          </SectionCard>
        </Grid>
      </Grid>

      <Stack gap={2}>
        <SectionCard eyebrow="Discovery operations" title="Jobs">
          <QueryState
            loading={jobsQuery.isPending}
            error={jobsQuery.error instanceof Error ? jobsQuery.error : null}
            empty={jobs.length === 0}
            onRetry={() => void jobsQuery.refetch()}
          >
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Source</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Discovered</TableCell>
                    <TableCell>New</TableCell>
                    <TableCell>Existing</TableCell>
                    <TableCell>Failed Sources</TableCell>
                    <TableCell>Started</TableCell>
                    <TableCell>Error</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {jobs.map((job) => (
                    <TableRow key={job.discoveryJobId} hover>
                      <TableCell>{formatEnum(job.sourceType)}</TableCell>
                      <TableCell><StatusChip label={formatEnum(job.status)} /></TableCell>
                      <TableCell>{job.totalDiscovered}</TableCell>
                      <TableCell>{job.newDocuments}</TableCell>
                      <TableCell>{job.existingDocuments}</TableCell>
                      <TableCell>{job.failedSources}</TableCell>
                      <TableCell>{formatDateTime(job.startedAt)}</TableCell>
                      <TableCell>{job.errorMessage ?? '—'}</TableCell>
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
            onRetry={() => void documentsQuery.refetch()}
          >
            <TableContainer>
              <Table sx={{ minWidth: 1250 }}>
                <TableHead>
                  <TableRow>
                    <TableCell>Document</TableCell>
                    <TableCell>Source</TableCell>
                    <TableCell>Company</TableCell>
                    <TableCell>Type</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>First discovered</TableCell>
                    <TableCell>Last seen</TableCell>
                    <TableCell>Seen</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {documents.map((document) => (
                    <TableRow key={document.id} hover>
                      <TableCell sx={{ maxWidth: 420 }}>
                        <Typography fontWeight={650}>{document.title}</Typography>
                        <Typography
                          component="a"
                          href={document.documentUrl}
                          target="_blank"
                          rel="noreferrer"
                          variant="caption"
                          color="text.secondary"
                          noWrap
                          display="block"
                        >
                          <LinkRounded sx={{ fontSize: 12, mr: 0.5, verticalAlign: 'middle' }} />
                          {document.documentUrl}
                        </Typography>
                      </TableCell>
                      <TableCell>{formatEnum(document.sourceType)}</TableCell>
                      <TableCell>{document.companySymbol ?? '—'}</TableCell>
                      <TableCell>{formatEnum(document.documentType)}</TableCell>
                      <TableCell><StatusChip label={formatEnum(document.status)} /></TableCell>
                      <TableCell>{formatDateTime(document.firstDiscoveredAt)}</TableCell>
                      <TableCell>{formatDateTime(document.lastSeenAt)}</TableCell>
                      <TableCell>{document.seenCount}</TableCell>
                      <TableCell align="right">
                        <Button
                          size="small"
                          variant="text"
                          color="inherit"
                          startIcon={<BlockRounded />}
                          disabled={document.status === 'IGNORED'
                            || (ignoreMutation.isPending
                              && ignoreMutation.variables === document.id)}
                          onClick={() => ignoreMutation.mutate(document.id)}
                        >
                          Ignore
                        </Button>
                      </TableCell>
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
