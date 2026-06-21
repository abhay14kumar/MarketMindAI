import { CloudUploadRounded, FilterListRounded } from '@mui/icons-material';
import { Button, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { documentQueries } from '../api/client';
import { PageHeader } from '../components/PageHeader';
import { QueryState } from '../components/QueryState';
import { SectionCard } from '../components/SectionCard';
import { StatusChip } from '../components/StatusChip';
import { formatEnum } from '../utils/format';

export function DocumentsPage() {
  const query = useQuery({ queryKey: ['documents'], queryFn: documentQueries.list });
  const documents = query.data?.data ?? [];

  return (
    <>
      <PageHeader eyebrow="Document intelligence" title="Research library" description="Filings, annual reports, quarterly results, transcripts, and presentations available to the RAG pipeline." action={<Stack direction="row" gap={1}><Button variant="outlined" startIcon={<FilterListRounded />}>Filters</Button><Button variant="contained" startIcon={<CloudUploadRounded />}>Upload document</Button></Stack>} />
      <SectionCard eyebrow="Indexed corpus" title="Documents">
        <QueryState
          loading={query.isPending}
          error={query.error instanceof Error ? query.error : null}
          empty={documents.length === 0}
          fallbackMessage={query.data?.fallbackReason}
          onRetry={() => void query.refetch()}
        >
          <TableContainer>
            <Table>
              <TableHead><TableRow><TableCell>Document</TableCell><TableCell>Source</TableCell><TableCell>Type</TableCell><TableCell>Period</TableCell><TableCell>Published</TableCell><TableCell>Status</TableCell></TableRow></TableHead>
              <TableBody>
                {documents.map((document) => (
                  <TableRow key={document.id} hover>
                    <TableCell><Typography fontWeight={650}>{document.title}</Typography></TableCell>
                    <TableCell>{document.sourceName ?? document.sourceCode ?? '—'}</TableCell>
                    <TableCell>{formatEnum(document.documentType)}</TableCell>
                    <TableCell>{document.reportingPeriod ?? document.quarter ?? '—'}</TableCell>
                    <TableCell>{document.publicationDate ?? '—'}</TableCell>
                    <TableCell><StatusChip label={formatEnum(document.status)} /></TableCell>
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
