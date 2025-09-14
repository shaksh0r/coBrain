import './App.css';
import React, { useState, useEffect } from 'react';
import {
    BrowserRouter as Router,
    Routes,
    Route,
    Navigate
} from 'react-router-dom';
import EditorPage from './Components/EditorPage.jsx';
import Login from './Components/Login.jsx';
import Register from './Components/Register.jsx';
import AuthContext from './Context/AuthContext.jsx';
import { validateToken } from './API/authapi.js';
import './stylesheets/FormStyle.css';

function App() {
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [isLoading, setIsLoading] = useState(true);

    const checkIsAuthenticated = async () => {
        const token = localStorage.getItem("token");
        try {
            if (token) {
                const response = await validateToken(token);
                setIsAuthenticated(response.valid);
            } else {
                setIsAuthenticated(false);
            }
        } catch (error) {
            console.log("auth error:", error);
            setIsAuthenticated(false);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        checkIsAuthenticated();
    }, []);

    if (isLoading) {
        return (
            <div className="form-container">
                <p className="loading-text">Loading...</p>
            </div>
        );
    }

    const contextValue = {
        isAuthenticated,
        setIsAuthenticated
    };

    return (
        <AuthContext.Provider value={contextValue}>
            <Router>
                <Routes>
                    <Route path="/" element={<Navigate to="/home" replace />} />
                    <Route
                        path="/home"
                        element={
                            isAuthenticated ? (
                                <EditorPage />
                            ) : (
                                <Navigate to="/login" replace />
                            )
                        }
                    />
                    <Route
                        path="/login"
                        element={
                            isAuthenticated ? (
                                <Navigate to="/home" replace />
                            ) : (
                                <Login />
                            )
                        }
                    />
                    <Route
                        path="/register"
                        element={
                            isAuthenticated ? (
                                <Navigate to="/home" replace />
                            ) : (
                                <Register />
                            )
                        }
                    />
                </Routes>
            </Router>
        </AuthContext.Provider>
    );
}

export default App;