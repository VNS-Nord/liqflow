import { useState } from 'react';
import { logout} from '../../auth/api/authApi.ts';
import type { AuthenticatedUser } from '../../auth/api/authApi.ts';

interface DashboardPageProps {
    user: AuthenticatedUser;
    onLogout: () => void;
}

export default function DashboardPage({user, onLogout}: DashboardPageProps) {
    const [error, setError] = useState('');

    async function handleLogout() {
        try {
            setError('');
            await logout();
            onLogout();
        } catch {
            setError('Failed to log out. Please try again.');
        }
    }
    return (
        <main>
            <h1>LiqFlow Dashboard</h1>
            <p>Welcome, {user.username}!</p>

            <button onClick = {handleLogout}>
                Sign out
            </button>

            {error && <p role="alert">{error}</p>}
        </main>
    )
}