import { Button, Stack, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';

export function NotFoundPage() {
  const navigate = useNavigate();
  return (
    <Stack minHeight="65vh" alignItems="center" justifyContent="center" textAlign="center" spacing={2}>
      <Typography variant="overline" color="primary.main">404 · ROUTE NOT FOUND</Typography>
      <Typography variant="h1">This workspace does not exist.</Typography>
      <Typography color="text.secondary">Return to the MarketMind command center.</Typography>
      <Button variant="contained" onClick={() => navigate('/')}>Open dashboard</Button>
    </Stack>
  );
}
