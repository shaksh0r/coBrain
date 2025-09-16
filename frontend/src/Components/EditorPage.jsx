import React, { useState, useRef, useEffect } from 'react';
import CodeEditor from './CodeEditor.jsx';
import Sessions from './Sessions.jsx';
import Toolbar from './Toolbar.jsx';
import Terminal from './Terminal.jsx';
import LeftToolbar from './LeftToolbar';
import FileExplorerPanel from './FileExplorerPanel.jsx';
import { getFilesForSession } from '../API/crdtwebsocket.js';
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
    };


    const [activeKey, setActiveKey] = useState(null);
    const [showFileExplorer, setShowFileExplorer] = useState(false);
    const [explorerFiles, setExplorerFiles] = useState([]);

    // Custom handler for LeftToolbar to toggle File Explorer
    const handleToolbarClick = (key) => {
        if (key === 'explorer') {
            setShowFileExplorer((prev) => {
                const next = !prev;
                if (next) {
                    // Only fetch files when toggling ON
                    (async () => {
                        if (sessionID) {
                            try {
                                const files = await getFilesForSession(sessionID);
                                console.log('Fetched files for session:', files);
                                setExplorerFiles(Array.isArray(files) ? files : []);
                            } catch (err) {
                                setExplorerFiles([]);
                                console.error('Failed to fetch files for session', err);
                            }
                        } else {
                            setExplorerFiles([]);
                        }
                    })();
                }
                return next;
            });
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
                    <FileExplorerPanel visible={showFileExplorer} files={explorerFiles} />
                    <div className="main-content">
                        {activeKey === 'sessions' ? (
                            <Sessions />
                        ) : (
                            <>
                                <CodeEditor />
                                <Terminal />
                            </>
                        )}
                    </div>
                </div>
            </IDEContext.Provider>
        </div>
    );
};

export default EditorPage;