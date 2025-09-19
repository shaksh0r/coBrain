// CHANGE TO WHATEVER PORT NUMBER YOU LIKE; THIS IS FOR BACKEND COPY AND GETCONTAINER
const PORT_NUMBER = 8080;

export async function registerUser(username, firstname, lastname, email, password) {
    try {
        const response = await fetch(`http://localhost:${PORT_NUMBER}/api/auth/signup`, { 
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, firstname, lastname, email, password })
        });
        const jsonData = await response.json();
        return jsonData;
    } catch (error) {
        console.log(error.message);
        throw error;
    }
};

export async function loginUser(username, password) {
    try {
        const response = await fetch(`http://localhost:${PORT_NUMBER}/api/auth/login`, { 
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        const jsonData = await response.json();
        return jsonData;
    } catch (error) {
        console.log(error.message);
        throw error;
    }
};

export async function validateToken(token) {
    try {
        const response = await fetch(`http://localhost:${PORT_NUMBER}/api/auth/validate`, {
            method: 'GET',
            headers: { token }
        });
        const jsonData = await response.json();
        return jsonData;
    } catch (error) {
        console.log(error.message);
        throw error;
    }
};