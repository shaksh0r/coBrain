import React, { useEffect, useState } from 'react'
import { getUserSessions, joinSession } from '../API/sessionapi'
import { useIDEContext } from '../Context/IDEContext.jsx';
import '../stylesheets/SessionsTable.css';

const Sessions = () => {
    const [sessions, setSessions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const { setSessionID } = useIDEContext();
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
            console.log('joinSession response:', response);
        } catch (error) {
            setJoinError(error.message || "Failed to join session");
        }
    }

    useEffect(() => {
        getSessions();
    }, []);

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
                            <th>Session ID</th>
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
                                    <td>{session.sessionId}</td>
                                    <td>{session.sessionName}</td>
                                    <td>{session.description}</td>
                                    <td>{new Date(session.createdAt).toLocaleString()}</td>
                                    <td>{new Date(session.expiresAt).toLocaleString()}</td>
                                    <td>
                                        <button
                                            className="sessions-table-join-btn"
                                            onClick={() => handleJoinSession(session.sessionId)}
                                        >
                                            Join
                                        </button>
                                    </td>
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan={6} style={{ textAlign: 'center', color: '#888' }}>No sessions found.</td>
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
