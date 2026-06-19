import {
  Area,
  AreaChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { performanceData } from '../../data/mockData';
import { chartColors } from '../../theme';

export function PerformanceChart() {
  return (
    <ResponsiveContainer width="100%" height={285}>
      <AreaChart data={performanceData} margin={{ top: 10, right: 8, left: -24, bottom: 0 }}>
        <defs>
          <linearGradient id="portfolioGradient" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={chartColors.primary} stopOpacity={0.25} />
            <stop offset="100%" stopColor={chartColors.primary} stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid stroke={chartColors.grid} strokeDasharray="3 6" vertical={false} />
        <XAxis dataKey="month" stroke={chartColors.muted} tickLine={false} axisLine={false} fontSize={11} />
        <YAxis stroke={chartColors.muted} tickLine={false} axisLine={false} fontSize={11} domain={['dataMin - 5', 'dataMax + 4']} />
        <Tooltip
          contentStyle={{ background: '#0c1721', border: `1px solid ${chartColors.grid}`, borderRadius: 8 }}
          labelStyle={{ color: '#e7eef5' }}
        />
        <Legend iconType="circle" wrapperStyle={{ fontSize: 12, paddingTop: 16 }} />
        <Area type="monotone" dataKey="portfolio" name="Portfolio" stroke={chartColors.primary} fill="url(#portfolioGradient)" strokeWidth={2.5} />
        <Area type="monotone" dataKey="nifty" name="NIFTY 50" stroke={chartColors.secondary} fill="transparent" strokeWidth={1.8} strokeDasharray="5 5" />
      </AreaChart>
    </ResponsiveContainer>
  );
}
