import { useEffect, useState } from 'react';
import LoginPage from './features/auth/pages/LoginPage';
import DashboardPage from './features/dashboard/pages/DashboardPage';
import { getCurrentUser } from './features/auth/api/authApi';
import type { AuthenticatedUser } from './features/auth/types.ts';

export default function App() {
    const [user, setUser] = useState<AuthenticatedUser | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
      getCurrentUser()
          .then(setUser)
          .catch(() => setUser(null))
          .finally(() => setLoading(false))
    }, []);

    if (loading) {
      return <p>Loading...</p>;
    }

    if (user) {
      return <DashboardPage user = {user} onLogout={() => setUser(null)}/>
    }

    return <LoginPage onLogin = {setUser}/>
}
