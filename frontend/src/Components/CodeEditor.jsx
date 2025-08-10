import React, { useRef, useState, useEffect } from 'react';
import Editor from '@monaco-editor/react';
import { connectWebSocket, disconnectWebSocket, sendCode } from '../API/websocket';

const CodeEditor = () => {
    const editorRef = useRef(null);
    const [value, setValue] = useState("");
    const str = "#Welcome to coBrain\n\n";

    // WebSocket setup using modular functions
    // Store stompClient reference
    const stompClientRef = useRef(null);
    useEffect(() => {
        // Connect and set up both real-time and initial state handlers
        const client = connectWebSocket(
            (msg) => setValue(msg),
            (initialState) => setValue(initialState)
        );
        stompClientRef.current = client;
        return () => disconnectWebSocket();
    }, []);

    const onMount = (editor) => {
        editorRef.current = editor;
        editor.focus();
    };

    // Handler for editor change
    const handleEditorChange = (newValue, event) => {
        // For now, send the whole new value as an 'insert' op at index 0 (replace all)
        // In a real app, you would diff and send only the change
        setValue(newValue);
        const op = {
            op: "insert",
            index: 0,
            value: newValue
        };
        sendCode(stompClientRef.current, JSON.stringify(op));
    };

    return (
        <div style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
            <div style={{
                height: '5%',
                backgroundColor: 'rgba(24, 24, 24, 1)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'flex-end',
                paddingRight: '16px' // optional, for spacing from the edge
                }}>
                <button>
                    Placeholder Buttons
                </button>
            </div>
            <div style={{ height: '95%'}}>
                <Editor
                    height="100%"
                    theme="vs-dark"
                    defaultLanguage="python"
                    defaultValue={str}
                    onMount={onMount}
                    value={value}
                    onChange={handleEditorChange}
                />
            </div>
        </div>
    )
}

export default CodeEditor

