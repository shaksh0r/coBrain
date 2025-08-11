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
        const oldValue = value;
        setValue(newValue);

        // If values are equal, do nothing
        if (newValue === oldValue) return;

        // Find first point of difference
        let start = 0;
        while (start < newValue.length && start < oldValue.length && newValue[start] === oldValue[start]) {
            start++;
        }

        // Find last point of difference
        let endNew = newValue.length - 1;
        let endOld = oldValue.length - 1;
        while (endNew >= start && endOld >= start && newValue[endNew] === oldValue[endOld]) {
            endNew--;
            endOld--;
        }

        if (newValue.length > oldValue.length) {
            // Insertion
            const inserted = newValue.slice(start, endNew + 1);
            const op = {
                op: "insert",
                index: start,
                value: inserted
            };
            sendCode(stompClientRef.current, JSON.stringify(op));
        } else if (newValue.length < oldValue.length) {
            // Deletion
            const op = {
                op: "delete",
                index: start,
                length: endOld - start + 1
            };
            sendCode(stompClientRef.current, JSON.stringify(op));
        } else {
            // Replacement (same length, but different content)
            const replaced = newValue.slice(start, endNew + 1);
            const op = {
                op: "replace",
                index: start,
                length: endOld - start + 1,
                value: replaced
            };
            sendCode(stompClientRef.current, JSON.stringify(op));
        }
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

