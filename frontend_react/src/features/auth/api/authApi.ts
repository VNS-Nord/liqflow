import type { AuthenticatedUser } from '../types.ts';

const API_BASE = '/api/v1/auth';

export interface CsrfToken {
    headerName: string;
    parameterName: string;
    token: string;
}

export async function getCsrfToken(): Promise<CsrfToken> {
    const response = await fetch(`${API_BASE}/csrf`, {
        method: 'GET',
        credentials: 'include',
    });

    if (!response.ok) {
        throw new Error(`Failed to fetch CSRF token: ${response.status}`);
    }

    return response.json() as Promise<CsrfToken>;
}

export async function login(username: string, password:string): Promise<void> {
    const csrf = await getCsrfToken();

    const response = await fetch(`${API_BASE}/login`, {
        method: 'POST',
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json',
            [csrf.headerName]: csrf.token,
        },
        body: JSON.stringify({username, password}),
    });

    if (!response.ok) {
        throw new Error(`Login failed: ${response.status}`)
    }
}

export async function getCurrentUser(): Promise<AuthenticatedUser> {
    const response = await fetch(`${API_BASE}/me`, {
        method: 'GET',
        credentials: 'include',
    });

    if (!response.ok) {
        throw new Error(`Failed to get user: ${response.status}`);
    }

    return response.json() as Promise<AuthenticatedUser>;
}

export async function logout(): Promise<void> {
    const csrf = await getCsrfToken();

    const response = await fetch(`${API_BASE}/logout`, {
        method: 'POST',
        credentials: 'include',
        headers: {
            [csrf.headerName]: csrf.token,
        },
    });

    if (!response.ok) {
        throw new Error(`Logout failed: ${response.status}`);
    }
}