export async function getContainer(sessionID, language) {
    try {
        const response = await fetch(`http://localhost:8080/getContainer/${sessionID}/${language}`);
        const jsonData = await response.json();
        console.log(jsonData);
    } catch (error) {
        console.log(error.message);
    }
};

export async function copyCode(sessionID, language, dirContent){
    try {
        const response = await fetch(`http://localhost:8080/copy`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                "sessionID" : sessionID,
                "directoryContent" : dirContent,
                "language" : language
            })
        });
        const jsonData = await response.json();
        console.log(jsonData);
    } catch (error) {
        console.log(error.message);
    }
}