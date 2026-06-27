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
  Alert,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { MetricCard } from '../components/MetricCard';
import { PageHeader } from '../components/PageHeader';
import { SectionCard } from '../components/SectionCard';
import { StatusChip } from '../components/StatusChip';
import { PerformanceChart } from '../components/charts/PerformanceChart';
import { SectorChart } from '../components/charts/SectorChart';
import { filings, portfolioMetrics, watchlist } from '../data/mockData';
import { discoveryQueries, documentQueries, pipelineQueries, sourceQueries } from '../api/client';
import { formatDateTime, formatEnum } from '../utils/format';

export function DashboardPage() {
  const discovery = useQuery({
    queryKey: ['discovery-jobs'],
    queryFn: () => discoveryQueries.jobs(0, 5),
    refetchInterval: 5_000,
  });
  const pipelines = useQuery({
    queryKey: ['pipeline-jobs'],
    queryFn: () => pipelineQueries.jobs(0, 5),
    refetchInterval: 5_000,
  });
  const documents = useQuery({
    queryKey: ['documents'],
    queryFn: documentQueries.list,
    refetchInterval: 15_000,
  });
  const health = useQuery({
    queryKey: ['sources', 'health'],
    queryFn: sourceQueries.health,
    refetchInterval: 30_000,
  });
  const discoveryJobs = discovery.data?.content ?? [];
  const pipelineJobs = pipelines.data?.content ?? [];
  const recentDocuments = documents.data?.data.slice(0, 5) ?? [];
  const healthySources = health.data?.data.filter((item) => item.available).length ?? 0;
  const nseWarning = discoveryJobs.find((job) =>
    job.sourceType === 'NSE' && job.status === 'COMPLETED' && job.totalDiscovered === 0);

  return (
    <>
      <PageHeader
        eyebrow="Command center"
        title="Good morning, Aarush."
        description="Your portfolio is outperforming NIFTY 50 by 13.2 percentage points over the trailing twelve months."
        action={<Button variant="contained" startIcon={<AutoAwesomeRounded />}>Run portfolio review</Button>}
      />

      {nseWarning && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          NSE discovery ran but found no documents. NSE-specific crawler required.
        </Alert>
      )}

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

        <Grid size={{ xs: 12, lg: 4 }}>
          <SectionCard eyebrow="Background work" title="Latest discovery jobs" minHeight={300}>
            <Stack divider={<Divider flexItem />}>
              {discoveryJobs.length === 0 && (
                <Typography color="text.secondary">
                  No discovery jobs yet. Run Test Discovery to validate source scanning.
                </Typography>
              )}
              {discoveryJobs.map((job) => (
                <Stack key={job.discoveryJobId} py={1.2} gap={0.5}>
                  <Stack direction="row" justifyContent="space-between">
                    <Typography fontWeight={650}>{formatEnum(job.sourceType)}</Typography>
                    <StatusChip label={job.totalDiscovered === 0 ? 'NO_RESULTS' : job.status} />
                  </Stack>
                  <Typography variant="body2" color="text.secondary">{job.message}</Typography>
                </Stack>
              ))}
            </Stack>
          </SectionCard>
        </Grid>

        <Grid size={{ xs: 12, lg: 4 }}>
          <SectionCard eyebrow="Background work" title="Latest pipeline jobs" minHeight={300}>
            <Stack divider={<Divider flexItem />}>
              {pipelineJobs.length === 0 && (
                <Typography color="text.secondary">
                  No pipeline jobs yet. Start ingestion from a discovered document.
                </Typography>
              )}
              {pipelineJobs.map((job) => (
                <Stack key={job.id} py={1.2} gap={0.5}>
                  <Stack direction="row" justifyContent="space-between">
                    <Typography fontWeight={650}>{job.id.slice(0, 8)}</Typography>
                    <StatusChip label={job.status} />
                  </Stack>
                  <Typography variant="body2" color="text.secondary">
                    {formatEnum(job.currentStage)} · {job.progressPercent}% · {formatDateTime(job.updatedAt)}
                  </Typography>
                </Stack>
              ))}
            </Stack>
          </SectionCard>
        </Grid>

        <Grid size={{ xs: 12, lg: 4 }}>
          <SectionCard eyebrow="Freshness & health" title="System activity" minHeight={300}>
            <Stack gap={1.5}>
              <Stack direction="row" justifyContent="space-between">
                <Typography>Reachable sources</Typography>
                <Typography fontWeight={700}>{healthySources}/{health.data?.data.length ?? 0}</Typography>
              </Stack>
              <Stack direction="row" justifyContent="space-between">
                <Typography>Documents processed</Typography>
                <Typography fontWeight={700}>{recentDocuments.length}</Typography>
              </Stack>
              <Stack direction="row" justifyContent="space-between">
                <Typography>AI-ready documents</Typography>
                <Typography fontWeight={700}>
                  {pipelineJobs.filter((job) => job.status === 'COMPLETED').length}
                </Typography>
              </Stack>
              <Divider />
              {recentDocuments.slice(0, 3).map((document) => (
                <Box key={document.id}>
                  <Typography fontWeight={650} fontSize="0.82rem">{document.title}</Typography>
                  <Typography variant="caption" color="text.secondary">
                    {formatEnum(document.status)} · {formatDateTime(document.createdAt)}
                  </Typography>
                </Box>
              ))}
            </Stack>
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
