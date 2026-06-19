import { Chip } from '@mui/material';

interface StatusChipProps {
  label: string;
}

export function StatusChip({ label }: StatusChipProps) {
  const upper = label.toUpperCase();
  const color = upper.includes('BUY') || upper === 'INDEXED' || upper === 'LOW'
    ? 'primary'
    : upper.includes('CRITICAL') || upper.includes('REVIEW')
      ? 'error'
      : upper.includes('HIGH') || upper.includes('PROCESS')
        ? 'warning'
        : 'default';

  return <Chip label={label} color={color} variant={color === 'default' ? 'outlined' : 'filled'} />;
}
