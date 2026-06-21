import { Box, Stack, Typography } from '@mui/material';
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import type { PortfolioAllocation } from '../../api/types';
import { formatCurrency, formatEnum } from '../../utils/format';

const colors = ['#54d6c2', '#62a8ff', '#f5b85c', '#c98cff', '#ff7b88', '#7fd37a', '#9aa9b8'];

interface AllocationChartProps {
  data: PortfolioAllocation[];
}

export function AllocationChart({ data }: AllocationChartProps) {
  const chartData = data.map((item, index) => ({
    ...item,
    name: formatEnum(item.category),
    color: colors[index % colors.length],
  }));

  return (
    <Stack direction={{ xs: 'column', sm: 'row', lg: 'column', xl: 'row' }} alignItems="center" gap={2}>
      <Box sx={{ width: '100%', minWidth: 180, height: 220 }}>
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie data={chartData} dataKey="percentage" nameKey="name" innerRadius={57} outerRadius={84} paddingAngle={3} stroke="none">
              {chartData.map((entry) => <Cell key={entry.category} fill={entry.color} />)}
            </Pie>
            <Tooltip
              formatter={(value, _name, item) => [
                `${Number(value).toFixed(2)}% · ${formatCurrency(item.payload.presentValue)}`,
                item.payload.name,
              ]}
              contentStyle={{ background: '#0c1721', border: '1px solid #20303d', borderRadius: 8 }}
            />
          </PieChart>
        </ResponsiveContainer>
      </Box>
      <Stack spacing={1.15} sx={{ minWidth: 180, width: '100%' }}>
        {chartData.map((item) => (
          <Stack key={item.category} direction="row" justifyContent="space-between" alignItems="center">
            <Stack direction="row" gap={1} alignItems="center" minWidth={0}>
              <Box sx={{ width: 8, height: 8, flexShrink: 0, borderRadius: '50%', bgcolor: item.color }} />
              <Typography variant="body2" color="text.secondary" noWrap>{item.name}</Typography>
            </Stack>
            <Typography variant="body2" fontFamily='"IBM Plex Mono", monospace'>
              {Number(item.percentage).toFixed(1)}%
            </Typography>
          </Stack>
        ))}
      </Stack>
    </Stack>
  );
}
