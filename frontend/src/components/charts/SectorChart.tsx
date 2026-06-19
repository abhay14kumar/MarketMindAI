import { Box, Stack, Typography } from '@mui/material';
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import { sectorAllocation } from '../../data/mockData';

export function SectorChart() {
  return (
    <Stack direction={{ xs: 'column', sm: 'row', lg: 'column', xl: 'row' }} alignItems="center" gap={2}>
      <Box sx={{ width: '100%', minWidth: 180, height: 210 }}>
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie data={sectorAllocation} dataKey="value" nameKey="name" innerRadius={57} outerRadius={82} paddingAngle={3} stroke="none">
              {sectorAllocation.map((entry) => <Cell key={entry.name} fill={entry.color} />)}
            </Pie>
            <Tooltip contentStyle={{ background: '#0c1721', border: '1px solid #20303d', borderRadius: 8 }} />
          </PieChart>
        </ResponsiveContainer>
      </Box>
      <Stack spacing={1.15} sx={{ minWidth: 170, width: '100%' }}>
        {sectorAllocation.map((sector) => (
          <Stack key={sector.name} direction="row" justifyContent="space-between" alignItems="center">
            <Stack direction="row" gap={1} alignItems="center">
              <Box sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: sector.color }} />
              <Typography variant="body2" color="text.secondary">{sector.name}</Typography>
            </Stack>
            <Typography variant="body2" fontFamily='"IBM Plex Mono", monospace'>{sector.value}%</Typography>
          </Stack>
        ))}
      </Stack>
    </Stack>
  );
}
