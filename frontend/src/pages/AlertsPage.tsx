import { DoneAllRounded, TuneRounded } from '@mui/icons-material';
import { Box, Button, Divider, Stack, Typography } from '@mui/material';
import { PageHeader } from '../components/PageHeader';
import { SectionCard } from '../components/SectionCard';
import { StatusChip } from '../components/StatusChip';
import { alerts } from '../data/mockData';

export function AlertsPage() {
  return (
    <>
      <PageHeader eyebrow="Monitoring center" title="Research alerts" description="Material disclosures, portfolio policy breaches, valuation changes, and document-processing events." action={<Stack direction="row" gap={1}><Button variant="outlined" startIcon={<TuneRounded />}>Rules</Button><Button variant="contained" startIcon={<DoneAllRounded />}>Mark all read</Button></Stack>} />
      <SectionCard eyebrow="Today" title="Priority queue">
        <Stack divider={<Divider flexItem />} spacing={0}>
          {alerts.map((alert) => (
            <Stack key={alert.title} direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={2} py={2}>
              <Box>
                <Stack direction="row" gap={1} alignItems="center" mb={0.6}><StatusChip label={alert.priority} /><Typography variant="caption" color="text.secondary">{alert.time}</Typography></Stack>
                <Typography fontWeight={650}>{alert.title}</Typography>
                <Typography variant="body2" color="text.secondary" mt={0.45}>{alert.detail}</Typography>
              </Box>
              <Button sx={{ alignSelf: { xs: 'flex-start', sm: 'center' } }}>Review</Button>
            </Stack>
          ))}
        </Stack>
      </SectionCard>
    </>
  );
}
