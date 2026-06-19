import { CloudUploadRounded, FilterListRounded } from '@mui/icons-material';
import { Button, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography } from '@mui/material';
import { PageHeader } from '../components/PageHeader';
import { SectionCard } from '../components/SectionCard';
import { StatusChip } from '../components/StatusChip';
import { documents } from '../data/mockData';

export function DocumentsPage() {
  return (
    <>
      <PageHeader eyebrow="Document intelligence" title="Research library" description="Filings, annual reports, quarterly results, transcripts, and presentations available to the RAG pipeline." action={<Stack direction="row" gap={1}><Button variant="outlined" startIcon={<FilterListRounded />}>Filters</Button><Button variant="contained" startIcon={<CloudUploadRounded />}>Upload document</Button></Stack>} />
      <SectionCard eyebrow="Indexed corpus" title="Documents">
        <TableContainer>
          <Table>
            <TableHead><TableRow><TableCell>Document</TableCell><TableCell>Company</TableCell><TableCell>Type</TableCell><TableCell>Period</TableCell><TableCell align="right">Pages</TableCell><TableCell>Status</TableCell></TableRow></TableHead>
            <TableBody>
              {documents.map((document) => (
                <TableRow key={document.document} hover>
                  <TableCell><Typography fontWeight={650}>{document.document}</Typography></TableCell>
                  <TableCell>{document.company}</TableCell>
                  <TableCell>{document.type}</TableCell>
                  <TableCell>{document.period}</TableCell>
                  <TableCell align="right">{document.pages}</TableCell>
                  <TableCell><StatusChip label={document.status} /></TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </SectionCard>
    </>
  );
}
