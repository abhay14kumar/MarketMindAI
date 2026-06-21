import { useMemo, useState } from 'react';
import {
  AutoAwesomeRounded,
  BusinessRounded,
  DashboardRounded,
  DescriptionRounded,
  DnsRounded,
  InsightsRounded,
  MenuRounded,
  NotificationsRounded,
  PieChartRounded,
  SearchRounded,
  SettingsRounded,
  ScheduleRounded,
  WorkHistoryRounded,
  VisibilityRounded,
} from '@mui/icons-material';
import {
  AppBar,
  Avatar,
  Badge,
  Box,
  Divider,
  Drawer,
  IconButton,
  InputAdornment,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Stack,
  TextField,
  Toolbar,
  Tooltip,
  Typography,
  useMediaQuery,
} from '@mui/material';
import { alpha, useTheme } from '@mui/material/styles';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';

const drawerWidth = 248;

const navItems = [
  { label: 'Dashboard', path: '/', icon: DashboardRounded },
  { label: 'Portfolio', path: '/portfolio', icon: PieChartRounded },
  { label: 'Watchlist', path: '/watchlist', icon: VisibilityRounded },
  { label: 'Company Intelligence', path: '/company-intelligence', icon: BusinessRounded },
  { label: 'AI Research Assistant', path: '/research', icon: AutoAwesomeRounded },
  { label: 'Sources', path: '/sources', icon: DnsRounded },
  { label: 'Documents', path: '/documents', icon: DescriptionRounded },
  { label: 'Download Jobs', path: '/document-jobs', icon: WorkHistoryRounded },
  { label: 'Scheduler', path: '/scheduler', icon: ScheduleRounded },
  { label: 'Alerts', path: '/alerts', icon: NotificationsRounded },
  { label: 'Settings', path: '/settings', icon: SettingsRounded },
];

export function AppShell() {
  const theme = useTheme();
  const isDesktop = useMediaQuery(theme.breakpoints.up('lg'));
  const [mobileOpen, setMobileOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  const currentLabel = useMemo(
    () => navItems.find((item) => item.path === location.pathname)?.label ?? 'MarketMind AI',
    [location.pathname],
  );

  const drawer = (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column', bgcolor: '#09131c' }}>
      <Stack direction="row" alignItems="center" gap={1.25} sx={{ px: 2.4, height: 72 }}>
        <Box sx={{ width: 34, height: 34, display: 'grid', placeItems: 'center', borderRadius: 1.5, bgcolor: 'primary.main', color: '#061410' }}>
          <InsightsRounded fontSize="small" />
        </Box>
        <Box>
          <Typography fontWeight={750} letterSpacing="-0.025em">MarketMind</Typography>
          <Typography variant="overline" color="text.secondary">Intelligence OS</Typography>
        </Box>
      </Stack>
      <Divider />
      <List sx={{ px: 1.25, py: 2 }}>
        {navItems.map(({ label, path, icon: Icon }) => {
          const selected = path === '/'
            ? location.pathname === '/'
            : location.pathname.startsWith(path);
          return (
            <ListItemButton
              key={path}
              selected={selected}
              onClick={() => {
                navigate(path);
                setMobileOpen(false);
              }}
              sx={{
                mb: 0.5,
                minHeight: 44,
                borderRadius: 1.5,
                color: selected ? 'text.primary' : 'text.secondary',
                '&.Mui-selected': {
                  bgcolor: alpha(theme.palette.primary.main, 0.1),
                  border: `1px solid ${alpha(theme.palette.primary.main, 0.22)}`,
                },
                '&.Mui-selected:hover': {
                  bgcolor: alpha(theme.palette.primary.main, 0.13),
                },
              }}
            >
              <ListItemIcon sx={{ minWidth: 38, color: selected ? 'primary.main' : 'text.secondary' }}>
                <Icon fontSize="small" />
              </ListItemIcon>
              <ListItemText
                primary={label}
                primaryTypographyProps={{ fontSize: '0.82rem', fontWeight: selected ? 650 : 500 }}
              />
            </ListItemButton>
          );
        })}
      </List>
      <Box sx={{ mt: 'auto', p: 2 }}>
        <Box sx={{ p: 1.7, border: '1px solid', borderColor: 'divider', borderRadius: 2, bgcolor: 'rgba(84,214,194,0.04)' }}>
          <Stack direction="row" justifyContent="space-between" alignItems="center">
            <Typography variant="overline" color="primary.main">Data status</Typography>
            <Box sx={{ width: 7, height: 7, borderRadius: '50%', bgcolor: 'primary.main', boxShadow: '0 0 12px #54d6c2' }} />
          </Stack>
          <Typography variant="body2" sx={{ mt: 0.5 }}>Backend API enabled</Typography>
          <Typography variant="caption" color="text.secondary">Fallback data available offline</Typography>
        </Box>
      </Box>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppBar
        position="fixed"
        elevation={0}
        sx={{
          width: { lg: `calc(100% - ${drawerWidth}px)` },
          ml: { lg: `${drawerWidth}px` },
          bgcolor: alpha('#071018', 0.84),
          backdropFilter: 'blur(18px)',
          borderBottom: '1px solid',
          borderColor: 'divider',
        }}
      >
        <Toolbar sx={{ minHeight: '72px !important', px: { xs: 2, md: 3 } }}>
          {!isDesktop && (
            <IconButton onClick={() => setMobileOpen(true)} sx={{ mr: 1 }}>
              <MenuRounded />
            </IconButton>
          )}
          <Typography variant="overline" color="text.secondary" sx={{ display: { xs: 'none', sm: 'block' } }}>
            {currentLabel}
          </Typography>
          <Box sx={{ flexGrow: 1 }} />
          <TextField
            placeholder="Search companies, filings, metrics…"
            sx={{
              width: { xs: 160, sm: 300, md: 380 },
              '& .MuiOutlinedInput-root': { bgcolor: alpha('#ffffff', 0.025) },
            }}
            slotProps={{
              input: {
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchRounded fontSize="small" />
                  </InputAdornment>
                ),
              },
            }}
          />
          <Tooltip title="Notifications">
            <IconButton sx={{ ml: 1.2 }}>
              <Badge variant="dot" color="error">
                <NotificationsRounded fontSize="small" />
              </Badge>
            </IconButton>
          </Tooltip>
          <Avatar sx={{ width: 34, height: 34, ml: 1, bgcolor: 'secondary.main', color: '#071018', fontSize: '0.75rem', fontWeight: 700 }}>
            AR
          </Avatar>
        </Toolbar>
      </AppBar>

      <Box component="nav" sx={{ width: { lg: drawerWidth }, flexShrink: { lg: 0 } }}>
        <Drawer
          variant={isDesktop ? 'permanent' : 'temporary'}
          open={isDesktop || mobileOpen}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{
            '& .MuiDrawer-paper': {
              width: drawerWidth,
              borderRight: '1px solid',
              borderColor: 'divider',
            },
          }}
        >
          {drawer}
        </Drawer>
      </Box>

      <Box component="main" sx={{ flexGrow: 1, minWidth: 0, pt: '72px' }}>
        <Box sx={{ p: { xs: 2, md: 3, xl: 4 }, maxWidth: 1680, mx: 'auto' }}>
          <Outlet />
        </Box>
      </Box>
    </Box>
  );
}
