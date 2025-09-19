// CHANGE TO WHATEVER PORT NUMBER YOU LIKE; THIS IS FOR BACKEND COPY AND GETCONTAINER
const PORT_NUMBER = 8082;

export async function registerUser(username, firstName, lastName, email, password) {
    try {
        const response = await fetch(`http://localhost:${PORT_NUMBER}/api/auth/signup`, { 
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, firstName, lastName, email, password })
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
            method: 'POST',  // match backend
            headers: {
                "Authorization": `Bearer ${token}`,  // correct header key
                "Content-Type": "application/json"   // optional but good practice
            }
        });
        console.log("Response:", response);
        const jsonData = await response.json();
        return jsonData;
    } catch (error) {
        console.log(error.message);
        throw error;
    }
}
