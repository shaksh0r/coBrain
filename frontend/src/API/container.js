// CHANGE TO WHATEVER PORT NUMBER YOU LIKE; THIS IS FOR BACKEND COPY AND GETCONTAINER
const PORT_NUMBER = 8081;

export async function getContainer(sessionID, language) {
    try {
        const response = await fetch(`http://localhost:${PORT_NUMBER}/getContainer/${sessionID}/${language}`);
        const jsonData = await response.json();
        console.log(jsonData);
    } catch (error) {
        console.error(error.message);
    }
};

export async function copyCode(sessionID, language, zipContent) {
    try {
        const response = await fetch(`http://localhost:${PORT_NUMBER}/copy`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                "sessionID" : sessionID,
                "directoryContent" : zipContent,
                "language" : language
            })
        });
        
        const jsonData = await response.json();
        console.log(jsonData);
    } catch (error) {
        console.error(error.message);
    }
};

export async function getDirContent(sessionID) {
    try {
        const response = await fetch(`http://localhost:8080/api/directory`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                "sessionID": sessionID
            })
        });
        const jsonData = await response.json();
        return jsonData;
    } catch (error) {
        console.log(error.message);
    }
};