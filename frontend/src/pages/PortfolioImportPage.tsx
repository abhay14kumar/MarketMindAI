import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { CheckCircleRounded, CloudUploadRounded } from '@mui/icons-material';
import {
  Alert,
  Box,
  Button,
  LinearProgress,
  Stack,
  Typography,
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { portfolioQueries } from '../api/client';
import { PageHeader } from '../components/PageHeader';
import { SectionCard } from '../components/SectionCard';
import { useNotifications } from '../notifications/NotificationProvider';

export function PortfolioImportPage() {
  const [file, setFile] = useState<File | null>(null);
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { notify } = useNotifications();
  const mutation = useMutation({
    mutationFn: portfolioQueries.importXlsx,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['portfolio'] });
      notify({
        title: 'Portfolio import completed',
        message: 'The workbook was imported and portfolio views were refreshed.',
        severity: 'success',
        path: '/portfolio',
      });
    },
  });

  return (
    <>
      <PageHeader
        eyebrow="Portfolio ingestion"
        title="Import Zerodha Holdings"
        description="Upload the XLSX export from Zerodha Console or Kite. The file is parsed in memory and is not retained."
      />
      <SectionCard eyebrow="Secure local import" title="Select holdings workbook">
        <Stack gap={2.5}>
          <Box
            component="label"
            sx={{
              minHeight: 230,
              border: '1px dashed',
              borderColor: file ? 'primary.main' : 'divider',
              borderRadius: 2.5,
              bgcolor: file ? 'rgba(84,214,194,0.04)' : 'rgba(255,255,255,0.015)',
              display: 'grid',
              placeItems: 'center',
              cursor: mutation.isPending ? 'default' : 'pointer',
              textAlign: 'center',
              p: 3,
            }}
          >
            <input
              hidden
              type="file"
              accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
              disabled={mutation.isPending}
              onChange={(event) => {
                setFile(event.target.files?.[0] ?? null);
                mutation.reset();
              }}
            />
            <Stack alignItems="center" gap={1}>
              {file ? <CheckCircleRounded color="primary" sx={{ fontSize: 42 }} /> : <CloudUploadRounded color="primary" sx={{ fontSize: 42 }} />}
              <Typography variant="h6">{file?.name ?? 'Choose an XLSX file'}</Typography>
              <Typography color="text.secondary">
                {file ? `${(file.size / 1024).toFixed(1)} KB selected` : 'Required columns are detected by header name; optional fields are handled safely.'}
              </Typography>
            </Stack>
          </Box>

          {mutation.isPending && <LinearProgress />}
          {mutation.error instanceof Error && <Alert severity="error">{mutation.error.message}</Alert>}
          {mutation.data && (
            <Alert severity={mutation.data.importJob.rejectedRows > 0 ? 'warning' : 'success'}>
              Imported {mutation.data.importJob.importedRows} of {mutation.data.importJob.totalRows} rows.
              {mutation.data.importJob.rejectedRows > 0 && ` ${mutation.data.importJob.rejectedRows} rows were rejected and recorded in import history.`}
            </Alert>
          )}

          <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="flex-end" gap={1}>
            <Button variant="outlined" onClick={() => navigate('/portfolio')}>Back to portfolio</Button>
            {mutation.data && (
              <Button variant="outlined" onClick={() => navigate('/portfolio/import-history')}>View import history</Button>
            )}
            <Button
              variant="contained"
              startIcon={<CloudUploadRounded />}
              disabled={!file || mutation.isPending}
              onClick={() => file && mutation.mutate(file)}
            >
              {mutation.isPending ? 'Importing…' : 'Import holdings'}
            </Button>
          </Stack>
        </Stack>
      </SectionCard>
    </>
  );
}
