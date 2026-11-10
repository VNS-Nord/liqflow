import {useState} from 'react';
import './LoginPage.css';

import { getCsrfToken } from '../api/authApi.ts';
import { login, getCurrentUser } from '../api/authApi.ts';
import type { AuthenticatedUser } from '../types.ts';

interface LoginPageProps {
    onLogin: (user: AuthenticatedUser) => void;
}

export default function LoginPage({onLogin}: LoginPageProps) {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');

    async function handleSubmit(event: React.SubmitEvent<HTMLFormElement>) {
        event.preventDefault();

        try {
            await login(username, password);
            const user = await getCurrentUser();
            onLogin(user);
            console.log('Login successful!');
        } catch (error) {
          console.error('Login failed:', error);
        }
    }

    return (
        <div className="login-page">
            <form onSubmit={handleSubmit}>
                <h1>LiqFlow</h1>
                <h2>Sign in</h2>

                <label htmlFor="username">Username</label>
                <input
                    id="username"
                    type="text"
                    value={username}
                    onChange={(event) => setUsername(event?.target.value)}
                    required/>

                <label htmlFor="password">Password</label>
                <input
                    id="password"
                    type="password"
                    value={password}
                    onChange={(event) => setPassword(event?.target.value)}
                    required/>

                <button type="submit">Sign in</button>
            </form>
        </div>
    );
}