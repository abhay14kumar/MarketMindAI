import { alpha, createTheme } from '@mui/material/styles';

const colors = {
  background: '#071018',
  paper: '#0d1822',
  elevated: '#111f2b',
  border: '#20303d',
  primary: '#54d6c2',
  secondary: '#78a9ff',
  warning: '#f3bd62',
  error: '#ff6f7d',
  text: '#e7eef5',
  muted: '#81909e',
};

export const theme = createTheme({
  palette: {
    mode: 'dark',
    primary: { main: colors.primary },
    secondary: { main: colors.secondary },
    warning: { main: colors.warning },
    error: { main: colors.error },
    background: {
      default: colors.background,
      paper: colors.paper,
    },
    text: {
      primary: colors.text,
      secondary: colors.muted,
    },
    divider: colors.border,
  },
  typography: {
    fontFamily: 'Inter, system-ui, sans-serif',
    h1: { fontSize: '2rem', fontWeight: 700, letterSpacing: '-0.04em' },
    h2: { fontSize: '1.4rem', fontWeight: 700, letterSpacing: '-0.025em' },
    h3: { fontSize: '1.05rem', fontWeight: 650 },
    body2: { lineHeight: 1.55 },
    overline: {
      fontFamily: '"IBM Plex Mono", monospace',
      fontSize: '0.69rem',
      fontWeight: 600,
      letterSpacing: '0.12em',
    },
    button: {
      fontWeight: 650,
      textTransform: 'none',
    },
  },
  shape: { borderRadius: 10 },
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: { backgroundColor: colors.background },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: 'none',
          border: `1px solid ${colors.border}`,
          boxShadow: '0 14px 36px rgba(0, 0, 0, 0.18)',
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          backgroundColor: alpha(colors.paper, 0.92),
          border: `1px solid ${colors.border}`,
          boxShadow: 'none',
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          minHeight: 38,
          borderRadius: 8,
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          height: 25,
          borderRadius: 5,
          fontFamily: '"IBM Plex Mono", monospace',
          fontSize: '0.68rem',
          fontWeight: 600,
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: {
          borderColor: colors.border,
        },
        head: {
          color: colors.muted,
          fontFamily: '"IBM Plex Mono", monospace',
          fontSize: '0.7rem',
          fontWeight: 600,
          letterSpacing: '0.08em',
          textTransform: 'uppercase',
        },
      },
    },
    MuiTextField: {
      defaultProps: {
        size: 'small',
      },
    },
  },
});

export const chartColors = {
  primary: colors.primary,
  secondary: colors.secondary,
  warning: colors.warning,
  error: colors.error,
  muted: colors.muted,
  grid: colors.border,
};
