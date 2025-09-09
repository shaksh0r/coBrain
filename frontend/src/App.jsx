import './App.css';
import { useState, useRef, useEffect } from 'react';
import { v4 as uuidv4 } from 'uuid';
import CodeEditor from './Components/CodeEditor.jsx';
import Terminal from './Components/Terminal.jsx';
import IDEContext from './Context/IDEContext.jsx';
import { requestDocumentState } from './API/crdtwebsocket';

function App() {
    const clientIdRef = useRef(uuidv4());
    const [sessionID, setSessionID] = useState("1");
    const [fileNameToFileId, setFileNameToFileId] = useState(new Map());
    const [activeFileId, setActiveFileId] = useState(null);
    const [language, setLanguage] = useState(null);
    const stompClientRef = useRef(null);

    const openFile = async (fileName) => {

        let fileID = null;

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
                requestDocumentState(stompClientRef.current, sessionID, fileID);
            }
        }
    };

    useEffect(() => {
        if (!activeFileId) {
            setLanguage(null);
            return;
        }
        const fileName = Array.from(fileNameToFileId.entries()).find(
            ([, id]) => id === activeFileId
        )?.[0];
        if (!fileName) {
            setLanguage(null);
            return;
        }

        const extension = fileName.split('.').pop()?.toLowerCase();
        const languageMap = {
            java: 'java',
            js: 'javascript',
            jsx: 'javascript',
            ts: 'typescript',
            tsx: 'typescript',
            py: 'python',
            cpp: 'cpp',
            cxx: 'cpp',
            cc: 'cpp',
            cs: 'csharp',
            html: 'html',
            css: 'css',
            json: 'json',
            md: 'markdown',
            txt: 'plaintext',
        };
        setLanguage(languageMap[extension] || 'plaintext');
    }, [activeFileId, fileNameToFileId]);

    const contextValue = {
        sessionID,
        fileNameToFileId,
        setFileNameToFileId,
        activeFileId,
        setActiveFileId,
        openFile,
        stompClientRef,
        clientIdRef,
        language,
    };

    return (
        <IDEContext.Provider value={contextValue}>
            <div style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
                <CodeEditor />
                <Terminal />
            </div>
        </IDEContext.Provider>
    );
}

export default App;