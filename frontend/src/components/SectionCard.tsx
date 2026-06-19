import type { ReactNode } from 'react';
import { Box, Card, CardContent, Stack, Typography } from '@mui/material';

interface SectionCardProps {
  title: string;
  eyebrow?: string;
  action?: ReactNode;
  children: ReactNode;
  minHeight?: number;
}

export function SectionCard({
  title,
  eyebrow,
  action,
  children,
  minHeight,
}: SectionCardProps) {
  return (
    <Card sx={{ height: '100%', minHeight }}>
      <CardContent sx={{ p: { xs: 2, md: 2.5 }, '&:last-child': { pb: { xs: 2, md: 2.5 } } }}>
        <Stack direction="row" justifyContent="space-between" alignItems="flex-start" mb={2.5}>
          <Box>
            {eyebrow && (
              <Typography variant="overline" color="primary.main">
                {eyebrow}
              </Typography>
            )}
            <Typography variant="h3">{title}</Typography>
          </Box>
          {action}
        </Stack>
        {children}
      </CardContent>
    </Card>
  );
}
