import React from 'react';
import CodeEditor from './CodeEditor.jsx';
import Toolbar from './Toolbar.jsx';
import Terminal from './Terminal.jsx';
import IDEContext from '../Context/IDEContext.jsx';
import { useState, useRef, useEffect } from 'react';
import '../stylesheets/EditorPage.css';

const EditorPage = () => {
    const clientIdRef = useRef(null);
    const [userName, setUserName] = useState('');

    const stompClientRef = useRef(null);
    const [sessionID, setSessionID] = useState("1");
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

    return (
        <div className="editorpage-container">
            <IDEContext.Provider value={contextValue}>
                <Toolbar />
                <div className="main-content">
                    <CodeEditor />
                    <Terminal />
                </div>
            </IDEContext.Provider>
        </div>
    );
};

export default EditorPage;