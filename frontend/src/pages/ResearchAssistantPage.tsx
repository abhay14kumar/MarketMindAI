import { AutoAwesomeRounded, SendRounded } from '@mui/icons-material';
import { Avatar, Box, Button, Chip, Divider, Grid, Stack, TextField, Typography } from '@mui/material';
import { PageHeader } from '../components/PageHeader';
import { SectionCard } from '../components/SectionCard';
import { researchMessages } from '../data/mockData';

export function ResearchAssistantPage() {
  return (
    <>
      <PageHeader eyebrow="AI research assistant" title="Evidence-grounded workspace" description="Ask questions across filings, transcripts, financials, valuation, and portfolio context." />
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, lg: 8 }}>
          <SectionCard eyebrow="Research session" title="HDFC Bank vs ICICI Bank" minHeight={610}>
            <Stack spacing={2.5}>
              {researchMessages.map((message, index) => (
                <Stack key={`${message.role}-${index}`} direction="row" gap={1.5} alignItems="flex-start">
                  <Avatar sx={{ width: 32, height: 32, bgcolor: message.role === 'assistant' ? 'primary.main' : 'secondary.main', color: '#071018', fontSize: '0.7rem', fontWeight: 700 }}>
                    {message.role === 'assistant' ? 'AI' : 'AR'}
                  </Avatar>
                  <Box sx={{ flex: 1, p: 2, borderRadius: 2, bgcolor: message.role === 'assistant' ? 'rgba(84,214,194,0.055)' : 'rgba(120,169,255,0.05)', border: '1px solid', borderColor: 'divider' }}>
                    <Typography variant="overline" color={message.role === 'assistant' ? 'primary.main' : 'secondary.main'}>{message.role}</Typography>
                    <Typography variant="body2" sx={{ mt: 0.6, lineHeight: 1.75 }}>{message.body}</Typography>
                    {message.role === 'assistant' && <Stack direction="row" gap={1} mt={1.5} flexWrap="wrap"><Chip label="8 citations" variant="outlined" /><Chip label="Confidence 81%" variant="outlined" /><Chip label="Data through Q1 FY27" variant="outlined" /></Stack>}
                  </Box>
                </Stack>
              ))}
              <Divider />
              <TextField multiline minRows={3} placeholder="Ask a follow-up grounded in the selected evidence…" />
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Typography variant="caption" color="text.secondary">AI output is decision support, not investment advice.</Typography>
                <Button variant="contained" endIcon={<SendRounded />}>Run analysis</Button>
              </Stack>
            </Stack>
          </SectionCard>
        </Grid>
        <Grid size={{ xs: 12, lg: 4 }}>
          <Stack spacing={2}>
            <SectionCard eyebrow="Active agents" title="Research team">
              <Stack spacing={1.3}>
                {['Filing Analyst', 'Financial Analyst', 'Valuation Analyst', 'Risk Analyst', 'Portfolio Agent', 'CIO Agent'].map((agent, index) => (
                  <Stack key={agent} direction="row" justifyContent="space-between"><Stack direction="row" gap={1} alignItems="center"><AutoAwesomeRounded color={index === 5 ? 'warning' : 'primary'} fontSize="small" /><Typography variant="body2">{agent}</Typography></Stack><Typography variant="caption" color={index < 4 ? 'primary.main' : 'text.secondary'}>{index < 4 ? 'Complete' : index === 4 ? 'Skipped' : 'Synthesizing'}</Typography></Stack>
                ))}
              </Stack>
            </SectionCard>
            <SectionCard eyebrow="Evidence scope" title="Selected sources">
              <Stack spacing={1.25}>
                {['6 quarterly results', '4 earnings transcripts', '2 annual reports', 'Live valuation snapshot'].map((source) => <Box key={source} sx={{ p: 1.25, border: '1px solid', borderColor: 'divider', borderRadius: 1.5 }}><Typography variant="body2">{source}</Typography></Box>)}
              </Stack>
            </SectionCard>
          </Stack>
        </Grid>
      </Grid>
    </>
  );
}
