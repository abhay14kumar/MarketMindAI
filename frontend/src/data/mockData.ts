export const portfolioMetrics = [
  { label: 'Portfolio value', value: '₹48,72,640', change: '+₹32,480', tone: 'positive' as const },
  { label: 'Today gain / loss', value: '+₹18,240', change: '+0.38%', tone: 'positive' as const },
  { label: 'Total return', value: '+₹8,24,710', change: '+20.37%', tone: 'positive' as const },
  { label: 'Research alerts', value: '12', change: '4 critical', tone: 'warning' as const },
];

export const performanceData = [
  { month: 'Jul', portfolio: 100, nifty: 100 },
  { month: 'Aug', portfolio: 103, nifty: 102 },
  { month: 'Sep', portfolio: 101, nifty: 100 },
  { month: 'Oct', portfolio: 108, nifty: 104 },
  { month: 'Nov', portfolio: 112, nifty: 106 },
  { month: 'Dec', portfolio: 116, nifty: 110 },
  { month: 'Jan', portfolio: 114, nifty: 109 },
  { month: 'Feb', portfolio: 121, nifty: 113 },
  { month: 'Mar', portfolio: 124, nifty: 115 },
  { month: 'Apr', portfolio: 127, nifty: 117 },
  { month: 'May', portfolio: 132, nifty: 120 },
  { month: 'Jun', portfolio: 135, nifty: 122 },
];

export const sectorAllocation = [
  { name: 'Financials', value: 27, color: '#54d6c2' },
  { name: 'Technology', value: 22, color: '#78a9ff' },
  { name: 'Industrials', value: 16, color: '#f3bd62' },
  { name: 'Consumer', value: 14, color: '#a989f5' },
  { name: 'Energy', value: 12, color: '#ff8b64' },
  { name: 'Other', value: 9, color: '#536575' },
];

export const holdings = [
  { symbol: 'HDFCBANK', company: 'HDFC Bank', quantity: 320, price: '₹1,986.40', value: '₹6,35,648', allocation: '13.0%', return: '+18.4%', day: '+0.8%' },
  { symbol: 'RELIANCE', company: 'Reliance Industries', quantity: 190, price: '₹3,015.20', value: '₹5,72,888', allocation: '11.8%', return: '+11.2%', day: '-0.3%' },
  { symbol: 'INFY', company: 'Infosys', quantity: 350, price: '₹1,621.80', value: '₹5,67,630', allocation: '11.6%', return: '+26.7%', day: '+1.1%' },
  { symbol: 'LT', company: 'Larsen & Toubro', quantity: 140, price: '₹3,642.15', value: '₹5,09,901', allocation: '10.5%', return: '+33.1%', day: '+0.4%' },
  { symbol: 'TITAN', company: 'Titan Company', quantity: 115, price: '₹3,521.75', value: '₹4,05,001', allocation: '8.3%', return: '+9.8%', day: '-0.7%' },
  { symbol: 'BHARTIARTL', company: 'Bharti Airtel', quantity: 240, price: '₹1,484.60', value: '₹3,56,304', allocation: '7.3%', return: '+41.5%', day: '+1.4%' },
];

export const watchlist = [
  { symbol: 'ASIANPAINT', company: 'Asian Paints', price: '₹2,914.10', day: '+0.6%', valuation: 'Premium', signal: 'WATCH', score: 72 },
  { symbol: 'ICICIBANK', company: 'ICICI Bank', price: '₹1,284.40', day: '+1.2%', valuation: 'Fair', signal: 'BUY ZONE', score: 87 },
  { symbol: 'TATAMOTORS', company: 'Tata Motors', price: '₹991.85', day: '-0.9%', valuation: 'Attractive', signal: 'BUY ZONE', score: 84 },
  { symbol: 'DMART', company: 'Avenue Supermarts', price: '₹4,842.20', day: '-0.2%', valuation: 'Premium', signal: 'WATCH', score: 68 },
  { symbol: 'PIDILITIND', company: 'Pidilite Industries', price: '₹3,127.45', day: '+0.3%', valuation: 'Premium', signal: 'HOLD', score: 74 },
];

export const filings = [
  { company: 'HDFC Bank', type: 'Exchange filing', title: 'Board approves ₹12,500 Cr capital raise', time: '18 min ago', severity: 'material' },
  { company: 'Infosys', type: 'Annual report', title: 'FY26 annual report and ESG disclosures', time: '2 hours ago', severity: 'new' },
  { company: 'Reliance Industries', type: 'Investor presentation', title: 'New energy business operating update', time: '4 hours ago', severity: 'new' },
  { company: 'Larsen & Toubro', type: 'Order update', title: 'Wins major hydrocarbon project in Middle East', time: 'Yesterday', severity: 'material' },
];

export const alerts = [
  { title: 'ICICI Bank entered valuation buy zone', detail: 'Forward P/B is now 1.8σ below its five-year mean.', time: '09:48', priority: 'High' },
  { title: 'Portfolio financials exposure above policy band', detail: 'Allocation reached 27%, versus a preferred ceiling of 25%.', time: '09:31', priority: 'Critical' },
  { title: 'Tata Motors Q1 transcript indexed', detail: 'Management commentary is ready for AI-assisted review.', time: '08:50', priority: 'Medium' },
  { title: 'HDFC Bank filing contains capital action', detail: 'Potential dilution requires thesis and valuation refresh.', time: '08:12', priority: 'High' },
];

export const documents = [
  { company: 'Infosys', document: 'FY26 Annual Report', type: 'Annual report', period: 'FY 2025–26', status: 'Indexed', pages: 312 },
  { company: 'HDFC Bank', document: 'Q1 FY27 Earnings Call', type: 'Transcript', period: 'Q1 FY 2026–27', status: 'Indexed', pages: 42 },
  { company: 'Reliance Industries', document: 'New Energy Update', type: 'Presentation', period: 'June 2026', status: 'Processing', pages: 56 },
  { company: 'Larsen & Toubro', document: 'Q1 Financial Results', type: 'Quarterly result', period: 'Q1 FY 2026–27', status: 'Indexed', pages: 18 },
  { company: 'Titan Company', document: 'Exchange Filing', type: 'Filing', period: 'June 2026', status: 'Needs review', pages: 7 },
];

export const researchMessages = [
  {
    role: 'user',
    body: 'Compare HDFC Bank and ICICI Bank on asset quality, deposit growth, and valuation. Which deserves incremental capital?',
  },
  {
    role: 'assistant',
    body: 'ICICI Bank currently presents the stronger risk-adjusted setup. Its asset-quality trend is marginally cleaner, while the valuation discount to HDFC Bank has narrowed but remains defensible. HDFC Bank retains the stronger deposit franchise, though near-term return ratios are still normalizing after the merger.',
  },
];
