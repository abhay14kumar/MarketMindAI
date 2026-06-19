import { SaveRounded } from '@mui/icons-material';
import { Button, FormControlLabel, Grid, MenuItem, Stack, Switch, TextField, Typography } from '@mui/material';
import { PageHeader } from '../components/PageHeader';
import { SectionCard } from '../components/SectionCard';

export function SettingsPage() {
  return (
    <>
      <PageHeader eyebrow="Workspace controls" title="Settings" description="Configure display preferences, research defaults, and notification behavior." action={<Button variant="contained" startIcon={<SaveRounded />}>Save changes</Button>} />
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, lg: 6 }}>
          <SectionCard eyebrow="General" title="Workspace preferences">
            <Stack spacing={2.5}>
              <TextField label="Display name" defaultValue="Aarush" />
              <TextField select label="Base currency" defaultValue="INR"><MenuItem value="INR">INR — Indian Rupee</MenuItem><MenuItem value="USD">USD — US Dollar</MenuItem></TextField>
              <TextField select label="Default benchmark" defaultValue="NIFTY50"><MenuItem value="NIFTY50">NIFTY 50</MenuItem><MenuItem value="NIFTY500">NIFTY 500</MenuItem></TextField>
              <TextField select label="Research horizon" defaultValue="12M"><MenuItem value="6M">6 months</MenuItem><MenuItem value="12M">12 months</MenuItem><MenuItem value="36M">3 years</MenuItem></TextField>
            </Stack>
          </SectionCard>
        </Grid>
        <Grid size={{ xs: 12, lg: 6 }}>
          <SectionCard eyebrow="Notifications" title="Alert preferences">
            <Stack spacing={2}>
              {[['Material company filings', 'Notify when portfolio or watchlist companies publish material disclosures.', true], ['Valuation buy zones', 'Notify when valuation enters a configured opportunity band.', true], ['Portfolio concentration', 'Notify when issuer or sector exposure exceeds policy.', true], ['Daily research digest', 'Receive a consolidated end-of-day research summary.', false]].map(([label, description, checked]) => (
                <FormControlLabel key={String(label)} control={<Switch defaultChecked={Boolean(checked)} />} label={<><Typography variant="body2" fontWeight={650}>{label}</Typography><Typography variant="caption" color="text.secondary">{description}</Typography></>} />
              ))}
            </Stack>
          </SectionCard>
        </Grid>
      </Grid>
    </>
  );
}
