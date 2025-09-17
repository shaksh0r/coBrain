import React, { useEffect, useState } from 'react'
import { getUserSessions, joinSession } from '../API/sessionapi'
import { getFilesForSession } from '../API/crdtwebsocket.js';
import { useIDEContext } from '../Context/IDEContext.jsx';
import '../stylesheets/SessionsTable.css';

const Sessions = () => {
    const [sessions, setSessions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const { setSessionID, setExplorerFiles } = useIDEContext();
    const [joinError, setJoinError] = useState(null);

    const getSessions = async () => {
        try {
            setLoading(true);
            const response = await getUserSessions();
            setSessions(response.sessions);
            setError(null);
        } catch (error) {
            setError('Error fetching sessions');
            setSessions([]);
        } finally {
            setLoading(false);
        }
    }

    const handleJoinSession = async (sessionId) => {
        setJoinError(null);
        try {
            const token = localStorage.getItem("token");
            if (!token) throw new Error("Authentication token is missing");
            const response = await joinSession(token, sessionId);
            setSessionID(sessionId);
            const response_fe = await getFilesForSession(sessionId);
            setExplorerFiles(response_fe || []);
            console.log(response_fe);
            console.log('joinSession response:', response);
        } catch (error) {
            setJoinError(error.message || "Failed to join session");
        }
    }

    useEffect(() => {
        getSessions();
    }, []);

    // Copy session ID to clipboard
    const handleCopySessionId = async (sessionId) => {
        try {
            await navigator.clipboard.writeText(sessionId);
        } catch (error) {
            console.error("Failed to copy session ID:", error);
        }
    };

    return (
        <div className="sessions-table-container">
            <h2 className="sessions-table-title">Your Sessions</h2>
            {loading ? (
                <div className="sessions-table-loading">Loading...</div>
            ) : error ? (
                <div className="sessions-table-error">{error}</div>
            ) : (
                <>
                <table className="sessions-table">
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Description</th>
                            <th>Created At</th>
                            <th>Expires At</th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>
                        {sessions && sessions.length > 0 ? (
                            sessions.map(session => (
                                <tr key={session.sessionId}>
                                    <td>{session.sessionName}</td>
                                    <td>{session.description}</td>
                                    <td>{new Date(session.createdAt).toLocaleString()}</td>
                                    <td>{new Date(session.expiresAt).toLocaleString()}</td>
                                    <td style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                                        <button
                                            className="sessions-table-join-btn"
                                            onClick={() => handleJoinSession(session.sessionId)}
                                        >
                                            Join
                                        </button>
                                        <button
                                            className="sessions-table-copy-btn"
                                            title="Copy Session ID"
                                            onClick={() => handleCopySessionId(session.sessionId)}
                                        >
                                            🔗
                                        </button>
                                    </td>
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan={5} style={{ textAlign: 'center', color: '#888' }}>No sessions found.</td>
                            </tr>
                        )}
                    </tbody>
                </table>
                {joinError && <div className="sessions-table-error">{joinError}</div>}
                </>
            )}
        </div>
    )
}

export default Sessions
