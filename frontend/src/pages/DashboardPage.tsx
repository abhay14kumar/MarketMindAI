import {
  ArrowForwardRounded,
  AutoAwesomeRounded,
  BoltRounded,
  DescriptionRounded,
} from '@mui/icons-material';
import {
  Box,
  Button,
  Chip,
  Divider,
  Grid,
  LinearProgress,
  Stack,
  Typography,
} from '@mui/material';
import { MetricCard } from '../components/MetricCard';
import { PageHeader } from '../components/PageHeader';
import { SectionCard } from '../components/SectionCard';
import { StatusChip } from '../components/StatusChip';
import { PerformanceChart } from '../components/charts/PerformanceChart';
import { SectorChart } from '../components/charts/SectorChart';
import { filings, portfolioMetrics, watchlist } from '../data/mockData';

export function DashboardPage() {
  return (
    <>
      <PageHeader
        eyebrow="Command center"
        title="Good morning, Aarush."
        description="Your portfolio is outperforming NIFTY 50 by 13.2 percentage points over the trailing twelve months."
        action={<Button variant="contained" startIcon={<AutoAwesomeRounded />}>Run portfolio review</Button>}
      />

      <Grid container spacing={2}>
        {portfolioMetrics.map((metric) => (
          <Grid key={metric.label} size={{ xs: 12, sm: 6, xl: 3 }}>
            <MetricCard {...metric} />
          </Grid>
        ))}

        <Grid size={{ xs: 12, lg: 8 }}>
          <SectionCard
            eyebrow="Performance"
            title="Portfolio vs benchmark"
            action={<Chip label="1Y · Indexed" variant="outlined" />}
          >
            <PerformanceChart />
          </SectionCard>
        </Grid>

        <Grid size={{ xs: 12, lg: 4 }}>
          <SectionCard eyebrow="Exposure" title="Sector allocation">
            <SectorChart />
          </SectionCard>
        </Grid>

        <Grid size={{ xs: 12, lg: 5 }}>
          <SectionCard
            eyebrow="Decision support"
            title="AI recommendation"
            action={<BoltRounded color="warning" fontSize="small" />}
            minHeight={325}
          >
            <Stack spacing={2}>
              <Stack direction="row" gap={1} flexWrap="wrap">
                <StatusChip label="Rotate review" />
                <Chip label="Confidence 78%" variant="outlined" />
                <Chip label="Horizon 6–12M" variant="outlined" />
              </Stack>
              <Typography variant="h2">Trim financials concentration; build ICICI Bank selectively.</Typography>
              <Typography color="text.secondary">
                HDFC Bank remains a core compounder, but your combined financials exposure has crossed the preferred policy band. ICICI Bank offers the stronger incremental risk/reward at current valuation.
              </Typography>
              <Box>
                <Stack direction="row" justifyContent="space-between" mb={0.75}>
                  <Typography variant="caption" color="text.secondary">Evidence coverage</Typography>
                  <Typography variant="caption" fontFamily='"IBM Plex Mono", monospace'>14 / 16 sources</Typography>
                </Stack>
                <LinearProgress variant="determinate" value={87} sx={{ height: 5, borderRadius: 4 }} />
              </Box>
              <Button endIcon={<ArrowForwardRounded />} sx={{ alignSelf: 'flex-start', px: 0 }}>
                Open CIO memo
              </Button>
            </Stack>
          </SectionCard>
        </Grid>

        <Grid size={{ xs: 12, lg: 3 }}>
          <SectionCard eyebrow="Opportunity monitor" title="Stocks in buy zone" minHeight={325}>
            <Stack divider={<Divider flexItem />} spacing={0}>
              {watchlist.filter((item) => item.signal === 'BUY ZONE').map((item) => (
                <Stack key={item.symbol} direction="row" justifyContent="space-between" py={1.5}>
                  <Box>
                    <Typography fontWeight={650}>{item.symbol}</Typography>
                    <Typography variant="caption" color="text.secondary">{item.valuation} valuation</Typography>
                  </Box>
                  <Box textAlign="right">
                    <Typography fontFamily='"IBM Plex Mono", monospace' fontWeight={600}>{item.score}</Typography>
                    <Typography variant="caption" color="primary.main">AI score</Typography>
                  </Box>
                </Stack>
              ))}
            </Stack>
          </SectionCard>
        </Grid>

        <Grid size={{ xs: 12, lg: 4 }}>
          <SectionCard
            eyebrow="Disclosure monitor"
            title="Latest company filings"
            action={<DescriptionRounded color="primary" fontSize="small" />}
            minHeight={325}
          >
            <Stack divider={<Divider flexItem />} spacing={0}>
              {filings.slice(0, 3).map((filing) => (
                <Box key={filing.title} py={1.25}>
                  <Stack direction="row" justifyContent="space-between" gap={2}>
                    <Typography fontWeight={650} fontSize="0.85rem">{filing.company}</Typography>
                    <Typography variant="caption" color="text.secondary" whiteSpace="nowrap">{filing.time}</Typography>
                  </Stack>
                  <Typography variant="body2" color="text.secondary" mt={0.45}>{filing.title}</Typography>
                </Box>
              ))}
            </Stack>
          </SectionCard>
        </Grid>
      </Grid>
    </>
  );
}
