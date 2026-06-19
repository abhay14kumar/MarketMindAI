import TrendingDownRounded from '@mui/icons-material/TrendingDownRounded';
import TrendingUpRounded from '@mui/icons-material/TrendingUpRounded';
import WarningAmberRounded from '@mui/icons-material/WarningAmberRounded';
import { Box, Card, CardContent, Stack, Typography } from '@mui/material';

type Tone = 'positive' | 'negative' | 'warning' | 'neutral';

interface MetricCardProps {
  label: string;
  value: string;
  change: string;
  tone?: Tone;
}

const toneColor: Record<Tone, string> = {
  positive: 'primary.main',
  negative: 'error.main',
  warning: 'warning.main',
  neutral: 'text.secondary',
};

export function MetricCard({ label, value, change, tone = 'neutral' }: MetricCardProps) {
  const Icon = tone === 'positive'
    ? TrendingUpRounded
    : tone === 'negative'
      ? TrendingDownRounded
      : WarningAmberRounded;

  return (
    <Card>
      <CardContent sx={{ p: 2.25, '&:last-child': { pb: 2.25 } }}>
        <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
          <Box>
            <Typography variant="overline" color="text.secondary">
              {label}
            </Typography>
            <Typography sx={{ mt: 1, fontSize: { xs: '1.45rem', lg: '1.7rem' }, fontWeight: 700, letterSpacing: '-0.04em' }}>
              {value}
            </Typography>
          </Box>
          <Box sx={{ width: 34, height: 34, borderRadius: 1.5, bgcolor: 'rgba(84,214,194,0.08)', display: 'grid', placeItems: 'center', color: toneColor[tone] }}>
            <Icon fontSize="small" />
          </Box>
        </Stack>
        <Typography variant="body2" sx={{ mt: 1.2, color: toneColor[tone], fontFamily: '"IBM Plex Mono", monospace', fontWeight: 600 }}>
          {change}
        </Typography>
      </CardContent>
    </Card>
  );
}
