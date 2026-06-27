import { Chip } from '@mui/material';

interface StatusChipProps {
  label: string;
}

export function StatusChip({ label }: StatusChipProps) {
  const upper = label.toUpperCase();
  const color = upper.includes('BUY')
      || upper === 'INDEXED'
      || upper === 'LOW'
      || upper === 'COMPLETED'
      || upper === 'AI READY'
      || upper === 'WIRED'
    ? 'success'
    : upper === 'RUNNING' || upper === 'STARTED' || upper === 'QUEUED'
      ? 'info'
    : upper.includes('CRITICAL')
        || upper.includes('REVIEW')
        || upper === 'FAILED'
      ? 'error'
      : upper.includes('HIGH')
          || upper.includes('PROCESS')
          || upper === 'PARTIAL'
          || upper === 'NO_RESULTS'
          || upper === 'NO RESULTS'
          || upper === 'SEEDED'
        ? 'warning'
        : 'default';

  return <Chip label={label} color={color} variant={color === 'default' ? 'outlined' : 'filled'} />;
}
