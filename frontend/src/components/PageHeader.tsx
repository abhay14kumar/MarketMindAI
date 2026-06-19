import type { ReactNode } from 'react';
import { Box, Stack, Typography } from '@mui/material';

interface PageHeaderProps {
  eyebrow: string;
  title: string;
  description: string;
  action?: ReactNode;
}

export function PageHeader({ eyebrow, title, description, action }: PageHeaderProps) {
  return (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      justifyContent="space-between"
      alignItems={{ xs: 'flex-start', sm: 'center' }}
      gap={2}
      mb={3}
    >
      <Box>
        <Typography variant="overline" color="primary.main">
          {eyebrow}
        </Typography>
        <Typography variant="h1" sx={{ mt: 0.25 }}>
          {title}
        </Typography>
        <Typography color="text.secondary" sx={{ mt: 0.75, maxWidth: 720 }}>
          {description}
        </Typography>
      </Box>
      {action}
    </Stack>
  );
}
