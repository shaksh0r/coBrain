import './App.css';
import { useState, useRef, useEffect } from 'react';
import { v4 as uuidv4 } from 'uuid';
import CodeEditor from './Components/CodeEditor.jsx';
import Terminal from './Components/Terminal.jsx';
import IDEContext from './Context/IDEContext.jsx';

function App() {
    const clientIdRef = useRef(uuidv4());
    const [sessionID, setSessionID] = useState("1");
    const [fileNameToFileId, setFileNameToFileId] = useState(new Map());
    const [activeFileId, setActiveFileId] = useState(null);
    const [language, setLanguage] = useState(null);
    const stompClientRef = useRef(null);

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
        stompClientRef,
        clientIdRef,
        language,
    };

    return (
        <IDEContext.Provider value={contextValue}>
            <div style={{ 
                display: 'flex', 
                flexDirection: 'column', 
                height: '100vh',
                margin: 0,
                padding: 0,
                backgroundColor: 'rgba(24, 24, 24, 1)'
            }}>
                <CodeEditor />
                <Terminal />
            </div>
        </IDEContext.Provider>
    );
}

export default App;