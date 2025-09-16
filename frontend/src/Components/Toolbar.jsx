import React, { useState } from 'react';
import { useAuthContext } from '../Context/AuthContext.jsx';
import { useIDEContext } from '../Context/IDEContext.jsx';
import * as sessionApi from '../API/sessionapi.js';
import Modal from './Modal.jsx';
import '../stylesheets/Toolbar.css';

const Toolbar = () => {
    const { setIsAuthenticated } = useAuthContext();
    const { userName, setUserName, sessionID, setSessionID, fileNameToFileId, setFileNameToFileId, clientIdRef, setActiveFileId } = useIDEContext();
    const [showDropdown, setShowDropdown] = useState(false);
    const [showFileDropdown, setShowFileDropdown] = useState(false);
    const [showSessionsDropdown, setShowSessionsDropdown] = useState(false);
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [showJoinModal, setShowJoinModal] = useState(false);
    const [showOpenFileModal, setShowOpenFileModal] = useState(false);
    const [error, setError] = useState(null);

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
        setSessionID(null);
    };

    const handleCreateSession = () => {
        setError(null);
        setShowSessionsDropdown(false);
        setShowCreateModal(true);
    };

    const performCreateSession = async (data) => {
        if (!data.sessionName) {
            setError("Session name is required");
            return;
        }
        try {
            const token = localStorage.getItem("token");
            if (!token) {
                throw new Error("Authentication token is missing");
            }
            const expirationHours = data.expirationHours ? parseInt(data.expirationHours, 10) : 24;
            const response = await sessionApi.createSession(token, data.sessionName, data.description || '', expirationHours);
            console.log('createSession response:', response); // Log response
            setSessionID(response.sessionId);
            setShowCreateModal(false); // Close modal
            setError(null);
        } catch (error) {
            setError(error.message || "Failed to create session");
            console.error('createSession error:', error.message);
        }
    };

    const handleJoinSession = () => {
        setError(null);
        setShowSessionsDropdown(false);
        setShowJoinModal(true);
    };

    const performJoinSession = async (data) => {
        if (!data.sessionID) {
            setError("Session ID is required");
            return;
        }
        try {
            const token = localStorage.getItem("token");
            if (!token) {
                throw new Error("Authentication token is missing");
            }
            const response = await sessionApi.joinSession(token, data.sessionID);
            console.log('joinSession response:', response); // Log response
            setSessionID(data.sessionID);
            setShowJoinModal(false); // Close modal
            setError(null);
        } catch (error) {
            setError(error.message || "Failed to join session");
            console.error('joinSession error:', error.message);
        }
    };

    const handleLeaveSession = async () => {
        setShowSessionsDropdown(false);
        try {
            const token = localStorage.getItem("token");
            if (!token) {
                throw new Error("Authentication token is missing");
            }
            const response = await sessionApi.leaveSession(token, sessionID);
            console.log('leaveSession response:', response); // Log response
            setSessionID('');
            setError(null);
        } catch (error) {
            setError(error.message || "Failed to leave session");
            console.error('leaveSession error:', error.message);
        }
    };

    const handleOpenFile = () => {
        setError(null);
        setShowFileDropdown(false);
        setShowOpenFileModal(true);
    }

    const performOpenFile = async (data) => {
        if (!data.fileName) {
            setError("File name is required");
            return;
        }
        try {
            await openFile(data.fileName);
            setShowOpenFileModal(false);
            setError(null);
        } catch (error) {
            setError("Failed to open file");
            console.error('openFile error:', error.message);
        }
    }

    const openFile = async (fileName) => {

        let fileID = null;

        console.log('Current session:', sessionID);

        if (fileNameToFileId.has(fileName)) {
            fileID = fileNameToFileId.get(fileName);
        } else {
            try {
                const response = await fetch('http://localhost:8080/api/files', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        userID: clientIdRef.current,
                        sessionID: sessionID,
                        fileName: fileName,
                    }),
                });
                if (!response.ok) throw new Error('Failed to create file');
                const data = await response.json();
                fileID = data.fileID;
                setFileNameToFileId((prev) => new Map(prev).set(data.fileName, data.fileID));
            } catch (error) {
                console.error('Error creating file:', error);
            }

            if (Array.from(fileNameToFileId.values()).length === 0) {
                setActiveFileId(fileID);
            }
        }
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
                            <button className="dropdown-item" onClick={handleOpenFile} disabled={!sessionID}>New File</button>
                            <button className="dropdown-item" onClick={handleOpenFile} disabled={!sessionID}>Open File</button>
                        </div>
                    )}
                </div>
                <div className="dropdown-container">
                    <button className="toolbar-button" onClick={toggleSessionsDropdown}>
                        Sessions
                    </button>
                    {showSessionsDropdown && (
                        <div className="dropdown-menu toolbar-dropdown">
                            <button className="dropdown-item" onClick={handleCreateSession}>Create Session</button>
                            <button className="dropdown-item" onClick={handleJoinSession}>Join Session</button>
                            <button className="dropdown-item" onClick={handleLeaveSession} disabled={!sessionID}>Leave Session</button>
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
                        <button className="dropdown-item" onClick={logout}>Logout</button>
                    </div>
                )}
            </div>

            {/* Open File Modal */}
            <Modal
                show={showOpenFileModal}
                onClose={() => setShowOpenFileModal(false)}
                title="Open File"
                inputs={[{ name: 'fileName', placeholder: 'Enter file name', label: 'File Name', type: 'text' }]}
                onSubmit={performOpenFile}
                submitLabel="Open"
            />

            {/* Create Session Modal */}
            <Modal
                show={showCreateModal}
                onClose={() => setShowCreateModal(false)}
                title="Create Session"
                inputs={[
                    { name: 'sessionName', placeholder: 'Enter session name', label: 'Session Name', type: 'text' },
                    { name: 'description', placeholder: 'Enter session description (optional)', label: 'Description', type: 'text' },
                    { name: 'expirationHours', placeholder: 'Enter duration in hours (default 24)', label: 'Expiration (hours)', type: 'number', min: 1 }
                ]}
                onSubmit={performCreateSession}
                submitLabel="Create"
            >
                {error && <p className="modal-error">{error}</p>}
            </Modal>

            {/* Join Session Modal */}
            <Modal
                show={showJoinModal}
                onClose={() => setShowJoinModal(false)}
                title="Join Session"
                inputs={[{ name: 'sessionID', placeholder: 'Enter Session ID', label: 'Session ID', type: 'text' }]}
                onSubmit={performJoinSession}
                submitLabel="Join"
            >
                {error && <p className="modal-error">{error}</p>}
            </Modal>
        </div>
    );
};

export default Toolbar;