// CHANGE TO WHATEVER PORT NUMBER YOU LIKE; THIS IS FOR BACKEND COPY AND GETCONTAINER
import JSZip from 'jszip';
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
        const zip = await JSZip.loadAsync(zipContent);
        for (const fileName of Object.keys(zip.files)) {
            const file = zip.files[fileName];
            if (!file.dir) {
                const content = await file.async('string');
                console.log('File: ${fileName}');
                console.log('Content:', content);
            } else {
                console.log('Directory: ${fileName}');
            }
        }
        const formData = new FormData();
        formData.append("userID", sessionID); // Match backend's expected parameter name
        formData.append("language", language);
        formData.append("directoryContent", zipContent); // zipContent should be a File or Blob

        const response = await fetch(`http://localhost:${PORT_NUMBER}/copy`, {
            method: 'POST',
            body: formData // No need to set Content-Type; browser sets it automatically for FormData
        });

        const data = await response.json();

        
        if (!response.ok) {
            throw new Error(`HTTP error! Status: ${response.status}`);
        }
        return data.directory;

        // Optionally handle response
        // const jsonData = await response.json();
        // console.log(jsonData);
    } catch (error) {
        console.error("Error in copyCode:", error.message);
    }
}

export async function compile(clientIdRef, language, sourcePath,directory) {
    try {
        console.log("Directory:",directory);
        const response = await fetch(`http://localhost:${PORT_NUMBER}/compile`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                userID: clientIdRef.current,
                language,
                sourcePath,
                directory
            })
        });
        const jsonData = await response.json();
        console.log(jsonData);
    } catch (error) {
        console.error(error.message);
    }
}

export async function run(clientIdRef, language, className = ''){
    try {
        const response = await fetch(`http://localhost:${PORT_NUMBER}/run`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                userID: clientIdRef.current,
                language,
                className
            })
        });
        const jsonData = await response.json();
        console.log(jsonData);
    } catch (error) {
        console.error(error.message);
    }
}

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