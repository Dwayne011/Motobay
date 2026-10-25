import { createBrowserRouter, Navigate, RouterProvider } from 'react-router-dom';
import GlobalStyles from '@mui/material/GlobalStyles';
import { AppShell } from './AppShell';
import { ExplorePage } from './features/explore/ExplorePage';
import { ReviewPage } from './features/review/ReviewPage';
import { SavedPage } from './features/saved/SavedPage';
import { SettingsPage } from './features/settings/SettingsPage';
import { profileStore } from './data/userStore';
import { M3ThemeProvider, m3Pins } from './theme';

function ModeratorOnly({ children }: { children: React.ReactNode }) {
  const moderator = profileStore.use((p) => p.moderator);
  return moderator ? children : <Navigate to="/settings" replace />;
}

const router = createBrowserRouter([
  {
    element: <AppShell />,
    children: [
      { path: '/', element: <ExplorePage /> },
      { path: '/bay/:type/:num', element: <ExplorePage /> },
      { path: '/saved', element: <SavedPage /> },
      { path: '/review', element: <ModeratorOnly><ReviewPage /></ModeratorOnly> },
      { path: '/settings', element: <SettingsPage /> },
      { path: '*', element: <Navigate to="/" replace /> },
    ],
  },
]);

export default function App() {
  return (
    <M3ThemeProvider>
      <GlobalStyles styles={m3Pins} />
      <RouterProvider router={router} />
    </M3ThemeProvider>
  );
}
