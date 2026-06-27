import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type PropsWithChildren,
} from 'react';
import { Alert, Snackbar } from '@mui/material';

export type NotificationSeverity = 'success' | 'warning' | 'error' | 'info';

export interface AppNotification {
  id: string;
  title: string;
  message: string;
  severity: NotificationSeverity;
  timestamp: string;
  path?: string;
  read: boolean;
}

interface NotificationContextValue {
  notifications: AppNotification[];
  unreadCount: number;
  notify: (notification: Omit<AppNotification, 'id' | 'timestamp' | 'read'>) => void;
  markAllRead: () => void;
  clearNotifications: () => void;
}

const STORAGE_KEY = 'marketmind.notifications';
const NotificationContext = createContext<NotificationContextValue | null>(null);

function initialNotifications(): AppNotification[] {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored ? (JSON.parse(stored) as AppNotification[]) : [];
  } catch {
    return [];
  }
}

export function NotificationProvider({ children }: PropsWithChildren) {
  const [notifications, setNotifications] = useState<AppNotification[]>(initialNotifications);
  const [toast, setToast] = useState<AppNotification | null>(null);

  const persist = useCallback((next: AppNotification[]) => {
    const recent = next.slice(0, 50);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(recent));
    return recent;
  }, []);

  const notify = useCallback((
    input: Omit<AppNotification, 'id' | 'timestamp' | 'read'>,
  ) => {
    const notification: AppNotification = {
      ...input,
      id: crypto.randomUUID(),
      timestamp: new Date().toISOString(),
      read: false,
    };
    setNotifications((current) => persist([notification, ...current]));
    setToast(notification);
  }, [persist]);

  const value = useMemo<NotificationContextValue>(() => ({
    notifications,
    unreadCount: notifications.filter((item) => !item.read).length,
    notify,
    markAllRead: () => setNotifications((current) => persist(
      current.map((item) => ({ ...item, read: true })),
    )),
    clearNotifications: () => {
      setNotifications([]);
      localStorage.removeItem(STORAGE_KEY);
    },
  }), [notifications, notify, persist]);

  return (
    <NotificationContext.Provider value={value}>
      {children}
      <Snackbar
        open={toast !== null}
        autoHideDuration={6000}
        onClose={() => setToast(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert
          severity={toast?.severity ?? 'info'}
          variant="filled"
          onClose={() => setToast(null)}
          sx={{ minWidth: 320 }}
        >
          {toast?.message}
        </Alert>
      </Snackbar>
    </NotificationContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useNotifications() {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotifications must be used inside NotificationProvider.');
  }
  return context;
}
