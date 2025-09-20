import React, { useState, useRef, useEffect } from 'react';
import CodeEditor from './CodeEditor.jsx';
import Sessions from './Sessions.jsx';
import Toolbar from './Toolbar.jsx';
import Terminal from './Terminal.jsx';
import LeftToolbar from './LeftToolbar';
import FileExplorerPanel from './FileExplorerPanel.jsx';
import IDEContext from '../Context/IDEContext.jsx';
import { getFileIcon } from './util/util.js';
import '../stylesheets/EditorPage.css';

const EditorPage = () => {
    const clientIdRef = useRef(null);
    const [userName, setUserName] = useState('');
    const stompClientRef = useRef(null);
    const [sessionID, setSessionID] = useState('');
    const [fileNameToFileId, setFileNameToFileId] = useState(new Map());
    const [activeFileId, setActiveFileId] = useState(null);
    const [language, setLanguage] = useState("debugCpp");

    const [explorerFiles, setExplorerFiles] = useState([]);
    const [showFileExplorer, setShowFileExplorer] = useState(false);
    const [selectedFiles, setSelectedFiles] = useState(new Set());

    const [sessions, setSessions] = useState([]);

    const [breakpoints, setBreakpoints] = useState(new Set());

    const terminalSocketRef = useRef(null);
    const debugSocketRef = useRef(null);
    const problemsSocketRef = useRef(null);

    useEffect(() => {
        if (clientIdRef.current === null) {
            clientIdRef.current = localStorage.getItem("userId");
        }
        if (userName === '') {
            setUserName(localStorage.getItem("username"));
        }
        if (!activeFileId) {
            //setLanguage(null);
            return;
        }
        const fileName = Array.from(fileNameToFileId.entries()).find(
            ([, id]) => id === activeFileId
        )?.[0];
        if (!fileName) {
            //setLanguage(null);
            return;
        }
        const extension = fileName.split('.').pop()?.toLowerCase();
        //setLanguage(languageMap[extension] || 'plaintext');
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
        explorerFiles,
        setExplorerFiles,
        showFileExplorer,
        setShowFileExplorer,
        selectedFiles,
        setSelectedFiles,
        breakpoints,
        setBreakpoints,
        getFileIcon,
        sessions,
        setSessions,
        terminalSocketRef,
        debugSocketRef,
        problemsSocketRef,
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