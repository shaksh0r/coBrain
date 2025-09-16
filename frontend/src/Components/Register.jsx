import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import '../stylesheets/FormStyle.css';
import { useAuthContext } from '../Context/AuthContext.jsx';
import { useIDEContext } from '../Context/IDEContext.jsx';
import { registerUser } from '../API/authapi';

const Register = () => {
    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [firstname, setFirstname] = useState('');
    const [lastname, setLastname] = useState('');
    const [error, setError] = useState('');

    const { setIsAuthenticated } = useAuthContext();
    const { setUserName } = useIDEContext();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        if (!username || !email || !password || !firstname || !lastname) {
            setError('All fields are required');
            return;
        }

        if (!email.includes('@')) {
            setError('Invalid email format');
            return;
        }

        if (password.length < 6) {
            setError('Password must be at least 6 characters');
            return;
        }

        try {
            const response = await registerUser(username, firstname, lastname, email, password);
            console.log('Registration response:', response);
            localStorage.setItem("token", response.token);
            localStorage.setItem("username", response.username);
            localStorage.setItem("userId", response.userId);
            setUserName(response.username);
            setIsAuthenticated(true);
        } catch (err) {
            setError('Registration failed. Please try again.');
            return;
        }
    };

    return (
        <div className="form-container">
            <form onSubmit={handleSubmit} className="form">
                <h2 className="form-title">Register</h2>
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
                    <label className="form-label">First Name</label>
                    <input
                        type="text"
                        value={firstname}
                        onChange={(e) => setFirstname(e.target.value)}
                        className="form-input"
                        placeholder="Enter first name"
                    />
                </div>
                <div className="form-group">
                    <label className="form-label">Last Name</label>
                    <input
                        type="text"
                        value={lastname}
                        onChange={(e) => setLastname(e.target.value)}
                        className="form-input"
                        placeholder="Enter last name"
                    />
                </div>
                <div className="form-group">
                    <label className="form-label">Email</label>
                    <input
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        className="form-input"
                        placeholder="Enter email"
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
                <button type="submit" className="form-button">Register</button>
                <p className="form-nav">
                    Already have an account? <Link to="/login" className="form-nav-link">Login</Link>
                </p>
            </form>
        </div>
    );
};

export default Register;