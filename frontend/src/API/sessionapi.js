// CHANGE TO WHATEVER PORT NUMBER YOU LIKE; THIS IS FOR BACKEND COPY AND GETCONTAINER
const PORT_NUMBER = 8082;

export async function createSession(token, sessionName, description = '', expirationHours = 24) {
    try {
        const response = await fetch(`http://localhost:${PORT_NUMBER}/api/sessions/create`, { 
            method: 'POST',
            headers: {
                "Authorization": `Bearer ${token}`,  // correct header key
                "Content-Type": "application/json"   // optional but good practice
            },
            body: JSON.stringify({ sessionName, description, expirationHours })
        });
        const jsonData = await response.json();
        return jsonData;
    } catch (error) {
        console.log(error.message);
        throw error;
    }
};

export async function joinSession(token, sessionId) {
    try {
        const response = await fetch(`http://localhost:${PORT_NUMBER}/api/sessions/${sessionId}/join`, { 
            method: 'POST',
            headers: {
                "Authorization": `Bearer ${token}`,  // correct header key
                "Content-Type": "application/json"   // optional but good practice
            }
        });
        const jsonData = await response.json();
        return jsonData;
    } catch (error) {
        console.log(error.message);
        throw error;
    }
};

export async function leaveSession(token, sessionId) {
    try {
        const response = await fetch(`http://localhost:${PORT_NUMBER}/api/sessions/${sessionId}/leave`, { 
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ token })
        });
        const jsonData = await response.json();
        return jsonData;
    } catch (error) {
        console.log(error.message);
        throw error;
    }
};

export async function getUserSessions(token) {
    try {
        console.log("getUserSessions called");
        const response = await fetch(`http://localhost:${PORT_NUMBER}/api/sessions/user`, { 
            method: 'GET',
            headers: {
                "Authorization": `Bearer ${token}`,  // correct header key
                "Content-Type": "application/json"   // optional but good practice
            }
        });
        const jsonData = await response.json();
        return jsonData;
    } catch (error) {
        console.log(error.message);
        throw error;
    }
};

export async function getSessionDetails(sessionId) {
    try {
        const response = await fetch(`http://localhost:${PORT_NUMBER}/api/sessions/${sessionId}`, { 
            method: 'GET',
            headers: { 'Content-Type': 'application/json' }
        });
        const jsonData = await response.json();
        return jsonData;
    } catch (error) {
        console.log(error.message);
        throw error;
    }
};

export async function getSessionUsers(sessionId) {
    try {
        const response = await fetch(`http://localhost:${PORT_NUMBER}/api/sessions/${sessionId}/users`, { 
            method: 'GET',
            headers: { 'Content-Type': 'application/json' }
        });
        const jsonData = await response.json();
        return jsonData;
    } catch (error) {
        console.log(error.message);
        throw error;
    }
};