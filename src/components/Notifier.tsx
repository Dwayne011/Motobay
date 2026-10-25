import Button from '@mui/material/Button';
import Snackbar from '@mui/material/Snackbar';
import { createStore } from '../data/createStore';
import { useWindowSizeClass } from '../hooks/useWindowSizeClass';

interface Notice {
  id: number;
  message: string;
  action?: { label: string; onClick: () => void };
}

const noticeStore = createStore<Notice | null>(null);
let nextId = 1;

/** Shows an M3 snackbar with an optional single action. */
export function notify(message: string, action?: Notice['action']) {
  noticeStore.set({ id: nextId++, message, action });
}

export function Notifier() {
  const notice = noticeStore.use((n) => n);
  const size = useWindowSizeClass();
  const close = () => noticeStore.set(null);
  return (
    <Snackbar
      key={notice?.id}
      open={notice !== null}
      onClose={(_, reason) => reason !== 'clickaway' && close()}
      autoHideDuration={notice?.action ? 6000 : 4000}
      message={notice?.message}
      // Keep clear of the 80dp navigation bar on compact screens.
      sx={size === 'compact' ? { bottom: 96 } : undefined}
      action={
        notice?.action && (
          <Button
            variant="text"
            onClick={() => {
              notice.action?.onClick();
              close();
            }}
          >
            {notice.action.label}
          </Button>
        )
      }
    />
  );
}
