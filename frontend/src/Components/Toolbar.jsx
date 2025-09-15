import React, { useState } from 'react';
import { useAuthContext } from '../Context/AuthContext.jsx';
import { useIDEContext } from '../Context/IDEContext.jsx';
import '../stylesheets/Toolbar.css';

const Toolbar = () => {
    const { setIsAuthenticated } = useAuthContext();
    const { userName, setUserName } = useIDEContext();
    const [showDropdown, setShowDropdown] = useState(false);
    const [showFileDropdown, setShowFileDropdown] = useState(false);
    const [showSessionsDropdown, setShowSessionsDropdown] = useState(false);

    const toggleDropdown = () => {
        setShowDropdown((prev) => !prev);
        setShowFileDropdown(false);
        setShowSessionsDropdown(false);
    };

    const toggleFileDropdown = () => {
        setShowFileDropdown((prev) => !prev);
        setShowDropdown(false);
        setShowSessionsDropdown(false);
    };

    const toggleSessionsDropdown = () => {
        setShowSessionsDropdown((prev) => !prev);
        setShowDropdown(false);
        setShowFileDropdown(false);
    };

    const logout = (e) => {
        e.preventDefault();
        localStorage.removeItem("token");
        localStorage.removeItem("username");
        localStorage.removeItem("userId");
        setIsAuthenticated(false);
        setUserName('');
    };

    return (
        <div className="top-bar">
            <div className="toolbar">
                <div className="dropdown-container">
                    <button className="toolbar-button" onClick={toggleFileDropdown}>
                        File
                    </button>
                    {showFileDropdown && (
                        <div className="dropdown-menu toolbar-dropdown">
                            <button className="dropdown-item">New File</button>
                            <button className="dropdown-item">Open File</button>
                        </div>
                    )}
                </div>
                <div className="dropdown-container">
                    <button className="toolbar-button" onClick={toggleSessionsDropdown}>
                        Sessions
                    </button>
                    {showSessionsDropdown && (
                        <div className="dropdown-menu toolbar-dropdown">
                            <button className="dropdown-item">Create Session</button>
                            <button className="dropdown-item">Join Session</button>
                            <button className="dropdown-item">Session Details</button>
                            <button className="dropdown-item">Session Members</button>
                            <button className="dropdown-item">Leave Session</button>
                        </div>
                    )}
                </div>
            </div>
            <div className="profile-section">
                <button className="profile-button" onClick={toggleDropdown}>
                    <span className="profile-icon">{userName.charAt(0).toUpperCase()}</span>
                    {userName}
                </button>
                {showDropdown && (
                    <div className="dropdown-menu profile-dropdown">
                        <button className="dropdown-item">Settings</button>
                        <button className="dropdown-item">Feedback</button>
                        <button className="dropdown-item" onClick={logout}>Logout</button>
                    </div>
                )}
            </div>
        </div>
    );
};

export default Toolbar;