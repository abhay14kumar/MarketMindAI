import { AddRounded, TuneRounded } from '@mui/icons-material';
import {
  Button,
  Grid,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { MetricCard } from '../components/MetricCard';
import { PageHeader } from '../components/PageHeader';
import { SectionCard } from '../components/SectionCard';
import { PerformanceChart } from '../components/charts/PerformanceChart';
import { SectorChart } from '../components/charts/SectorChart';
import { holdings } from '../data/mockData';

export function PortfolioPage() {
  return (
    <>
      <PageHeader
        eyebrow="Portfolio intelligence"
        title="Core Equity Portfolio"
        description="Position-level performance, allocation, and concentration monitoring."
        action={<Stack direction="row" gap={1}><Button variant="outlined" startIcon={<TuneRounded />}>Policies</Button><Button variant="contained" startIcon={<AddRounded />}>Add transaction</Button></Stack>}
      />
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 4 }}><MetricCard label="Invested capital" value="₹40,47,930" change="83.1% deployed" /></Grid>
        <Grid size={{ xs: 12, sm: 4 }}><MetricCard label="Market value" value="₹48,72,640" change="+20.37%" tone="positive" /></Grid>
        <Grid size={{ xs: 12, sm: 4 }}><MetricCard label="Largest position" value="13.0%" change="HDFC Bank" tone="warning" /></Grid>
        <Grid size={{ xs: 12, lg: 8 }}>
          <SectionCard eyebrow="Return profile" title="Performance history"><PerformanceChart /></SectionCard>
        </Grid>
        <Grid size={{ xs: 12, lg: 4 }}>
          <SectionCard eyebrow="Diversification" title="Allocation"><SectorChart /></SectionCard>
        </Grid>
        <Grid size={12}>
          <SectionCard eyebrow="Positions" title="Current holdings">
            <TableContainer>
              <Table>
                <TableHead><TableRow><TableCell>Security</TableCell><TableCell align="right">Qty</TableCell><TableCell align="right">Price</TableCell><TableCell align="right">Market value</TableCell><TableCell align="right">Weight</TableCell><TableCell align="right">Today</TableCell><TableCell align="right">Total return</TableCell></TableRow></TableHead>
                <TableBody>
                  {holdings.map((holding) => (
                    <TableRow key={holding.symbol} hover>
                      <TableCell><Typography fontWeight={650}>{holding.symbol}</Typography><Typography variant="caption" color="text.secondary">{holding.company}</Typography></TableCell>
                      <TableCell align="right">{holding.quantity}</TableCell>
                      <TableCell align="right">{holding.price}</TableCell>
                      <TableCell align="right">{holding.value}</TableCell>
                      <TableCell align="right">{holding.allocation}</TableCell>
                      <TableCell align="right" sx={{ color: holding.day.startsWith('+') ? 'primary.main' : 'error.main' }}>{holding.day}</TableCell>
                      <TableCell align="right" sx={{ color: 'primary.main', fontWeight: 650 }}>{holding.return}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </SectionCard>
        </Grid>
      </Grid>
    </>
  );
}
