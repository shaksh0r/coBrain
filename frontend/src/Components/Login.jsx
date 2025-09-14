import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import '../stylesheets/FormStyle.css';
import { useAuthContext } from '../Context/AuthContext.jsx';
import { useIDEContext } from '../Context/IDEContext.jsx';
import { loginUser } from '../API/authapi';

const Login = () => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');

    const { setIsAuthenticated } = useAuthContext();
    const { setUserName } = useIDEContext();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        if (!username || !password) {
            setError('Both fields are required');
            return;
        }

        try {
            const response = await loginUser(username, password);
            console.log('Login response:', response);
            localStorage.setItem("token", response.token);
            localStorage.setItem("username", response.username);
            localStorage.setItem("userId", response.userId);
            setUserName(response.username);
            setIsAuthenticated(true);
        } catch (err) {
            setError('Login failed. Please try again.');
        }
    };

    return (
        <div className="form-container">
            <form onSubmit={handleSubmit} className="form">
                <h2 className="form-title">Login</h2>
                <div className="form-group">
                    <label className="form-label">Username</label>
                    <input
                        type="text"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        className="form-input"
                        placeholder="Enter username"
                    />
                </div>
                <div className="form-group">
                    <label className="form-label">Password</label>
                    <input
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        className="form-input"
                        placeholder="Enter password"
                    />
                </div>
                {error && <p className="form-error">{error}</p>}
                <button type="submit" className="form-button">Login</button>
                <p className="form-nav">
                    Don't have an account? <Link to="/register" className="form-nav-link">Register</Link>
                </p>
            </form>
        </div>
    );
};

export default Login;