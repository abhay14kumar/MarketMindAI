import type { ReactNode } from 'react';
import { Alert, Box, Button, CircularProgress, Stack, Typography } from '@mui/material';

interface QueryStateProps {
  loading: boolean;
  error: Error | null;
  empty: boolean;
  fallbackMessage?: string;
  onRetry: () => void;
  children: ReactNode;
  emptyMessage?: string;
}

export function QueryState({
  loading,
  error,
  empty,
  fallbackMessage,
  onRetry,
  children,
  emptyMessage = 'No records are available yet.',
}: QueryStateProps) {
  if (loading) {
    return (
      <Box minHeight={220} display="grid" sx={{ placeItems: 'center' }}>
        <CircularProgress size={30} />
      </Box>
    );
  }

  if (error) {
    return (
      <Stack minHeight={220} alignItems="center" justifyContent="center" gap={1.5}>
        <Alert severity="error" sx={{ width: '100%', maxWidth: 680 }}>
          {error.message}
        </Alert>
        <Button variant="outlined" onClick={onRetry}>Retry</Button>
      </Stack>
    );
  }

  if (empty) {
    return (
      <Box minHeight={180} display="grid" sx={{ placeItems: 'center' }}>
        <Typography color="text.secondary">{emptyMessage}</Typography>
      </Box>
    );
  }

  return (
    <Stack gap={2}>
      {fallbackMessage && <Alert severity="warning">{fallbackMessage}</Alert>}
      {children}
    </Stack>
  );
}
