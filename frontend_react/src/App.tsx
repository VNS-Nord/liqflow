import { useEffect, useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';

import LoginPage from './features/auth/pages/LoginPage';
import DashboardPage from './features/dashboard/pages/DashboardPage';
import { getCurrentUser } from './features/auth/api/authApi';
import type { AuthenticatedUser } from './features/auth/types';
import AppLayout from './components/layout/AppLayout';
import InventoryPage from './features/inventory/pages/InventoryPage';

export default function App() {
    const [user, setUser] = useState<AuthenticatedUser | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        getCurrentUser()
            .then(setUser)
            .catch(() => setUser(null))
            .finally(() => setLoading(false));
    }, []);

    if (loading) {
        return <p>Loading...</p>;
    }

    return (
        <BrowserRouter>
            <Routes>
                <Route
                    path="/login"
                    element={
                        user
                            ? <Navigate to="/dashboard" replace />
                            : <LoginPage onLogin={setUser} />
                    }
                />

                <Route
                    element={
                        user
                            ? <AppLayout />
                            : <Navigate to="/login" replace />
                    }
                >
                    <Route
                        path="/dashboard"
                        element={
                            <DashboardPage
                                user={user!}
                                onLogout={() => setUser(null)}
                            />
                        }
                    />

                    <Route
                        path="/inventory"
                        element={<InventoryPage />}
                    />
                </Route>

                <Route
                    path="*"
                    element={
                        <Navigate
                            to={user ? '/dashboard' : '/login'}
                            replace
                        />
                    }
                />
            </Routes>
        </BrowserRouter>
    );
}
