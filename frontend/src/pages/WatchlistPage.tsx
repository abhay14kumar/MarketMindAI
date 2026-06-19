import { AddRounded } from '@mui/icons-material';
import { Button, LinearProgress, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography } from '@mui/material';
import { PageHeader } from '../components/PageHeader';
import { SectionCard } from '../components/SectionCard';
import { StatusChip } from '../components/StatusChip';
import { watchlist } from '../data/mockData';

export function WatchlistPage() {
  return (
    <>
      <PageHeader eyebrow="Opportunity monitor" title="Watchlist" description="Track valuation, price action, and AI research scores across prospective investments." action={<Button variant="contained" startIcon={<AddRounded />}>Add security</Button>} />
      <SectionCard eyebrow="Research universe" title="India quality compounders">
        <TableContainer>
          <Table>
            <TableHead><TableRow><TableCell>Security</TableCell><TableCell align="right">Price</TableCell><TableCell align="right">Today</TableCell><TableCell>Valuation</TableCell><TableCell>Decision support</TableCell><TableCell>AI score</TableCell></TableRow></TableHead>
            <TableBody>
              {watchlist.map((item) => (
                <TableRow key={item.symbol} hover>
                  <TableCell><Typography fontWeight={650}>{item.symbol}</Typography><Typography variant="caption" color="text.secondary">{item.company}</Typography></TableCell>
                  <TableCell align="right">{item.price}</TableCell>
                  <TableCell align="right" sx={{ color: item.day.startsWith('+') ? 'primary.main' : 'error.main' }}>{item.day}</TableCell>
                  <TableCell>{item.valuation}</TableCell>
                  <TableCell><StatusChip label={item.signal} /></TableCell>
                  <TableCell sx={{ minWidth: 130 }}><Typography variant="caption">{item.score}/100</Typography><LinearProgress variant="determinate" value={item.score} sx={{ mt: 0.6, height: 4, borderRadius: 3 }} /></TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </SectionCard>
    </>
  );
}
