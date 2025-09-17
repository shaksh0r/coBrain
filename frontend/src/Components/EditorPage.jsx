// EditorPage.jsx
import React, { useState, useRef, useEffect } from 'react';
import CodeEditor from './CodeEditor.jsx';
import Sessions from './Sessions.jsx';
import Toolbar from './Toolbar.jsx';
import Terminal from './Terminal.jsx';
import LeftToolbar from './LeftToolbar';
import FileExplorerPanel from './FileExplorerPanel.jsx';
import IDEContext from '../Context/IDEContext.jsx';
import '../stylesheets/EditorPage.css';

const EditorPage = () => {
    const clientIdRef = useRef(null);
    const [userName, setUserName] = useState('');
    const stompClientRef = useRef(null);
    const [sessionID, setSessionID] = useState('');
    const [fileNameToFileId, setFileNameToFileId] = useState(new Map());
    const [activeFileId, setActiveFileId] = useState(null);
    const [language, setLanguage] = useState(null);

    const [explorerFiles, setExplorerFiles] = useState([]);
    const [showFileExplorer, setShowFileExplorer] = useState(false);

    // Move openFile function from Toolbar.jsx
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
            }
        }
        return fileID;
    };

    useEffect(() => {
        if (clientIdRef.current === null) {
            clientIdRef.current = localStorage.getItem("userId");
        }
        if (userName === '') {
            setUserName(localStorage.getItem("username"));
        }
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

    useEffect(() => {
        setFileNameToFileId(new Map());
        setActiveFileId(null);
    }, [sessionID]);

    const contextValue = {
        clientIdRef,
        stompClientRef,
        userName,
        setUserName,
        sessionID,
        setSessionID,
        fileNameToFileId,
        setFileNameToFileId,
        activeFileId,
        setActiveFileId,
        language,
        openFile,
        explorerFiles,
        setExplorerFiles,
        showFileExplorer,
        setShowFileExplorer,
    };

    const [activeKey, setActiveKey] = useState(null);

    const handleToolbarClick = (key) => {
        if (key === 'explorer') {
            setShowFileExplorer(!showFileExplorer);
        } else {
            setActiveKey(key);
        }
    };

    return (
        <div className="editorpage-container">
            <IDEContext.Provider value={contextValue}>
                <Toolbar />
                <div className="editorpage-main-area">
                    <LeftToolbar
                        activeKey={activeKey}
                        setActiveKey={handleToolbarClick}
                    />
                    <FileExplorerPanel />
                    <div className="main-content">
                        <div style={{ display: activeKey === 'sessions' ? 'none' : 'block', height: '100%' }}>
                            <CodeEditor />
                            <Terminal />
                        </div>
                        {activeKey === 'sessions' && <Sessions />}
                    </div>
                </div>
            </IDEContext.Provider>
        </div>
    );
};

export default EditorPage;