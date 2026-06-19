import { AutoAwesomeRounded, DescriptionRounded } from '@mui/icons-material';
import { Box, Button, Chip, Divider, Grid, Stack, Typography } from '@mui/material';
import { MetricCard } from '../components/MetricCard';
import { PageHeader } from '../components/PageHeader';
import { SectionCard } from '../components/SectionCard';
import { StatusChip } from '../components/StatusChip';

export function CompanyIntelligencePage() {
  return (
    <>
      <PageHeader eyebrow="Company intelligence" title="HDFC Bank" description="HDFCBANK · NSE · Financials · Data as of 19 Jun 2026, 12:30 IST" action={<Button variant="contained" startIcon={<AutoAwesomeRounded />}>Generate research memo</Button>} />
      <Grid container spacing={2}>
        <Grid size={{ xs: 6, lg: 3 }}><MetricCard label="Market price" value="₹1,986.40" change="+0.82%" tone="positive" /></Grid>
        <Grid size={{ xs: 6, lg: 3 }}><MetricCard label="Market cap" value="₹15.2T" change="Large cap" /></Grid>
        <Grid size={{ xs: 6, lg: 3 }}><MetricCard label="P / B" value="2.71×" change="-0.4σ vs 5Y" tone="positive" /></Grid>
        <Grid size={{ xs: 6, lg: 3 }}><MetricCard label="ROE" value="14.8%" change="+90 bps YoY" tone="positive" /></Grid>
        <Grid size={{ xs: 12, lg: 7 }}>
          <SectionCard eyebrow="Investment thesis" title="AI evidence summary" minHeight={360}>
            <Stack spacing={2}>
              <Stack direction="row" gap={1}><StatusChip label="Hold review" /><Chip label="Confidence 82%" variant="outlined" /><Chip label="12 citations" variant="outlined" /></Stack>
              <Typography variant="h2">Deposit franchise remains the moat; return ratios are normalizing.</Typography>
              <Typography color="text.secondary">The merger-related funding drag is easing as deposit growth catches up with advances. Asset quality remains resilient, though near-term upside depends on sustained margin recovery and operating leverage.</Typography>
              <Divider />
              {[
                ['Catalyst', 'Deposit growth above system and faster liability repricing.'],
                ['Key risk', 'Prolonged margin pressure or slower post-merger integration.'],
                ['Valuation', 'Fair relative to normalized ROE; not yet a deep-value setup.'],
              ].map(([label, text]) => <Box key={label}><Typography variant="overline" color="primary.main">{label}</Typography><Typography variant="body2">{text}</Typography></Box>)}
            </Stack>
          </SectionCard>
        </Grid>
        <Grid size={{ xs: 12, lg: 5 }}>
          <SectionCard eyebrow="Fundamental monitor" title="Operating indicators" minHeight={360}>
            <Stack spacing={2.2}>
              {[['Deposit growth', '16.5%', '+180 bps YoY'], ['Gross NPA', '1.24%', '-12 bps YoY'], ['Net interest margin', '3.48%', '+6 bps QoQ'], ['Credit cost', '0.43%', 'Stable']].map(([label, value, note]) => (
                <Stack key={label} direction="row" justifyContent="space-between" alignItems="center"><Box><Typography variant="body2" color="text.secondary">{label}</Typography><Typography fontSize="1.35rem" fontWeight={700}>{value}</Typography></Box><Chip label={note} variant="outlined" /></Stack>
              ))}
            </Stack>
          </SectionCard>
        </Grid>
        <Grid size={12}>
          <SectionCard eyebrow="Source room" title="Recent evidence" action={<Button startIcon={<DescriptionRounded />}>View all documents</Button>}>
            <Grid container spacing={2}>
              {['Q1 FY27 earnings call', 'FY26 annual report', 'Capital raise exchange filing'].map((title, index) => (
                <Grid key={title} size={{ xs: 12, md: 4 }}><Box sx={{ p: 2, border: '1px solid', borderColor: 'divider', borderRadius: 2 }}><Typography variant="overline" color="text.secondary">SOURCE 0{index + 1}</Typography><Typography fontWeight={650} mt={0.5}>{title}</Typography><Typography variant="body2" color="text.secondary" mt={1}>Indexed with page-level citations and period metadata.</Typography></Box></Grid>
              ))}
            </Grid>
          </SectionCard>
        </Grid>
      </Grid>
    </>
  );
}
