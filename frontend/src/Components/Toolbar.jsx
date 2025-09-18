// Toolbar.jsx
import React, { useState, useRef, useEffect } from 'react';
import { useAuthContext } from '../Context/AuthContext.jsx';
import { useIDEContext } from '../Context/IDEContext.jsx';
import { getFilesForSession, createFile, loadFile } from '../API/crdtwebsocket.js';
import * as sessionApi from '../API/sessionapi.js';
import Modal from './Modal.jsx';
import '../stylesheets/Toolbar.css';

const Toolbar = () => {
    const { setIsAuthenticated } = useAuthContext();
    const { userName, setUserName, sessionID, setSessionID, setActiveFileId, clientIdRef,
            fileNameToFileId, setFileNameToFileId, explorerFiles, setExplorerFiles } = useIDEContext();
    const [showDropdown, setShowDropdown] = useState(false);
    const [showFileDropdown, setShowFileDropdown] = useState(false);
    const [showSessionsDropdown, setShowSessionsDropdown] = useState(false);
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [showJoinModal, setShowJoinModal] = useState(false);
    const [showOpenFileModal, setShowOpenFileModal] = useState(false);
    const [error, setError] = useState(null);

    const fileDropdownRef = useRef(null);
    const sessionsDropdownRef = useRef(null);
    const profileDropdownRef = useRef(null);

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (
                fileDropdownRef.current && !fileDropdownRef.current.contains(event.target) &&
                sessionsDropdownRef.current && !sessionsDropdownRef.current.contains(event.target) &&
                profileDropdownRef.current && !profileDropdownRef.current.contains(event.target)
            ) {
                setShowFileDropdown(false);
                setShowSessionsDropdown(false);
                setShowDropdown(false);
            } else {
                if (fileDropdownRef.current && !fileDropdownRef.current.contains(event.target)) {
                    setShowFileDropdown(false);
                }
                if (sessionsDropdownRef.current && !sessionsDropdownRef.current.contains(event.target)) {
                    setShowSessionsDropdown(false);
                }
                if (profileDropdownRef.current && !profileDropdownRef.current.contains(event.target)) {
                    setShowDropdown(false);
                }
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, []);

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
            console.log('createSession response:', response);
            setSessionID(response.sessionId);
            setShowCreateModal(false);
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
            console.log('joinSession response:', response);
            setSessionID(data.sessionID);
            setShowJoinModal(false);
            setError(null);
            const response_fe = await getFilesForSession(data.sessionID);
            setExplorerFiles(response_fe || []);
            console.log(response_fe);
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
            console.log('leaveSession response:', response);
            setSessionID('');
            setError(null);
        } catch (error) {
            setError(error.message || "Failed to leave session");
            console.error('leaveSession error:', error.message);
        }
    };

    const openFile = async (fileName) => {
        let fileID = null;
        if (fileNameToFileId.has(fileName)) {
            fileID = fileNameToFileId.get(fileName);
        } 
        else if (explorerFiles.some(file => file.fileName === fileName)) {
            fileID = explorerFiles.find(file => file.fileName === fileName).fileID;
            setFileNameToFileId((prev) => new Map(prev).set(fileName, fileID));
        }
        else {
            try {
                fileID = await createFile(sessionID, fileName, clientIdRef);
                setFileNameToFileId((prev) => new Map(prev).set(fileName, fileID));
            } catch (error) {
                console.error('Error creating file:', error);
            }
            if (Array.from(fileNameToFileId.values()).length === 0) {
                setActiveFileId(fileID);
            }
        }
        return fileID;
    };


    // File input ref for loading local files
    const fileInputRef = useRef(null);

    const handleLoadFile = () => {
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
            fileInputRef.current.click();
        }
        setShowFileDropdown(false);
    };

    const handleFileInputChange = async (e) => {
        const file = e.target.files && e.target.files[0];
        if (!file) {
            return;
        }
        const reader = new FileReader();
        reader.onload = async (event) => {
            const content = event.target.result;
            try {
                if (fileNameToFileId.has(file.name)){
                    setActiveFileId(fileNameToFileId.get(file.name));
                    setShowFileDropdown(false);
                    return;
                }
                if (explorerFiles.some(f => f.fileName === file.name)) {
                    const fileID = explorerFiles.find(f => f.fileName === file.name).fileID;
                    setFileNameToFileId((prev) => new Map(prev).set(file.name, fileID));
                    setActiveFileId(fileID);
                    setShowFileDropdown(false);
                    return;
                }

                const fileID = await loadFile(sessionID, file.name, clientIdRef, content);
                setFileNameToFileId((prev) => new Map(prev).set(file.name, fileID));
                setActiveFileId(fileID);
            } catch (error) {
                setError('Failed to load file into editor');
            }
        };
        reader.readAsText(file);
    };

    const handleOpenFile = () => {
        setError(null);
        setShowFileDropdown(false);
        setShowOpenFileModal(true);
    };

    const performOpenFile = async (data) => {
        if (!data.fileName) {
            setError("File name is required");
            return;
        }
        try {
            const fileID = await openFile(data.fileName);
            setActiveFileId(fileID);
            setShowOpenFileModal(false);
            setError(null);
        } catch (error) {
            setError("Failed to open file");
            console.error('openFile error:', error.message);
        }
    };

    return (
        <div className="top-bar">
            <div className="toolbar">
                <div className="dropdown-container" ref={fileDropdownRef}>
                    <button className="toolbar-button" onClick={toggleFileDropdown}>
                        File
                    </button>
                    <input
                        type="file"
                        ref={fileInputRef}
                        style={{ display: 'none' }}
                        onChange={handleFileInputChange}
                        accept=".java,.js,.jsx,.ts,.tsx,.py,.pyw,.cpp,.cxx,.cc,.c,.h,.hpp,
                        .cs,.go,.rb,.php,.swift,.kt,.kts,.rs,.scala,.sh,.bash,.zsh,.html,
                        .css,.scss,.less,.json,.yaml,.yml,.xml,.md,.markdown,.txt,.dockerfile,
                        .makefile,.ini,.conf,.toml,.bat,.ps1,.sql,.plist,.vue,.svelte,.astro,
                        .dart,.groovy,.perl,.pl,.r,.tex,.latex,.coffee,.asm,.sol,.log"
                    />
                    {showFileDropdown && (
                        <div className="dropdown-menu toolbar-dropdown">
                            <button className="dropdown-item" onClick={handleOpenFile} disabled={!sessionID}>New File</button>
                            <button className="dropdown-item" onClick={handleLoadFile} disabled={!sessionID}>Open File...</button>
                        </div>
                    )}
                </div>
                <div className="dropdown-container" ref={sessionsDropdownRef}>
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
            <div className="profile-section" ref={profileDropdownRef}>
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
                title="New File"
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