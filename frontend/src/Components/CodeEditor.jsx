import React, { useRef, useEffect } from 'react';
import Editor from '@monaco-editor/react';
import * as monaco from 'monaco-editor';
import { connectWebSocket, disconnectWebSocket, sendCode } from '../API/websocket';
import { v4 as uuidv4 } from 'uuid';   // <-- install with `npm install uuid`

const CodeEditor = () => {

    const editorRef = useRef(null);
    const stompClientRef = useRef(null);
    const isProgrammaticChange = useRef(false);

    // unique ID for this client session
    const clientIdRef = useRef(uuidv4());

    useEffect(() => {
        console.log("Connecting to WebSocket...");
        // Connect and set up both real-time and initial state handlers
        const client = connectWebSocket(
            handleRemoteOperation,
            (initialState) => {
                console.log("Received initial state:", initialState);
                if (editorRef.current) {
                    const model = editorRef.current.getModel();
                    if (model) {
                        isProgrammaticChange.current = true;
                        model.applyEdits([
                            {
                                range: model.getFullModelRange(),
                                text: initialState || '', // Default to empty string if null/undefined
                                forceMoveMarkers: true
                            }
                        ]);
                        isProgrammaticChange.current = false;
                    }
                }
            },
            (error) => {
                console.error("WebSocket connection error:", error);
            }
        );
        stompClientRef.current = client;

        return () => {
            console.log("Disconnecting WebSocket...");
            disconnectWebSocket(client);
        };
    }, []);

    const onMount = (editor) => {
        editorRef.current = editor;
        editor.focus();
        console.log("Editor mounted");
    };

    // Apply operations from backend (ignore my own)
    const handleRemoteOperation = (msg) => {
        try {
            console.log("Received operation:", msg);
            const op = JSON.parse(msg);

            // skip self-echo
            if (op.clientId && op.clientId === clientIdRef.current) {
                return;
            }

            const model = editorRef.current?.getModel();
            if (!model) return;

            isProgrammaticChange.current = true;

            if (op.op === "insert") {
                const pos = model.getPositionAt(op.index);
                model.applyEdits([{
                    range: new monaco.Range(
                        pos.lineNumber, pos.column,
                        pos.lineNumber, pos.column
                    ),
                    text: op.value,
                    forceMoveMarkers: true
                }]);
            } else if (op.op === "delete") {
                const start = model.getPositionAt(op.index);
                const end = model.getPositionAt(op.index + op.length);
                model.applyEdits([{
                    range: new monaco.Range(
                        start.lineNumber, start.column,
                        end.lineNumber, end.column
                    ),
                    text: "",
                    forceMoveMarkers: true
                }]);
            } else if (op.op === "replace") {
                const start = model.getPositionAt(op.index);
                const end = model.getPositionAt(op.index + op.length);
                model.applyEdits([{
                    range: new monaco.Range(
                        start.lineNumber, start.column,
                        end.lineNumber, end.column
                    ),
                    text: op.value,
                    forceMoveMarkers: true
                }]);
            }

            isProgrammaticChange.current = false;
        } catch (e) {
            console.error("Bad operation:", msg, e);
        }
    };

    // On local edit → send ops (tagged with clientId)
    const handleEditorChange = (newValue, event) => {
        if (isProgrammaticChange.current) {
            return;
        }

        const model = editorRef.current?.getModel();
        if (!model || !event?.changes) return;

        event.changes.forEach(change => {
            const start = model.getOffsetAt(change.range.getStartPosition());
            const op = { clientId: clientIdRef.current };  // tag with my clientId

            if (change.text.length > 0 && change.rangeLength === 0) {
                // Insert
                op.op = "insert";
                op.index = start;
                op.value = change.text;
            } else if (change.text.length === 0 && change.rangeLength > 0) {
                // Delete
                op.op = "delete";
                op.index = start;
                op.length = change.rangeLength;
            } else {
                // Replace
                op.op = "replace";
                op.index = start;
                op.length = change.rangeLength;
                op.value = change.text;
            }

            console.log("Sending op:", op);
            sendCode(stompClientRef.current, JSON.stringify(op));
        });
    };

    // Auto-type function: types a string into the editor, mimicking human typing
    const autoType = async (text, delay = 100) => {
        const model = editorRef.current?.getModel();
        const editor = editorRef.current;
        if (!model || !editor) return;
        for (let i = 0; i < text.length; i++) {
            // Use applyEdits to insert one character at the end
            model.applyEdits([
                {
                    range: new monaco.Range(
                        model.getLineCount(),
                        model.getLineMaxColumn(model.getLineCount()),
                        model.getLineCount(),
                        model.getLineMaxColumn(model.getLineCount())
                    ),
                    text: text[i],
                    forceMoveMarkers: true
                }
            ]);
            // Move cursor to the end after each insert
            const endPosition = model.getPositionAt(model.getValue().length);
            editor.setPosition(endPosition);
            await new Promise(res => setTimeout(res, delay));
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
                paddingRight: '16px'
            }}>
                <button onClick={() => autoType('Hello, this is auto-typing!\nMy name is Gawwy and I love coding in java because it is so much fun!!!\n', 100)}>
                    AutoType Demo
                </button>
            </div>
            <div style={{ height: '95%' }}>
                <Editor
                    height="100%"
                    theme="vs-dark"
                    defaultLanguage="python"
                    onMount={onMount}
                    onChange={handleEditorChange}
                />
            </div>
        </div>
    );
};

export default CodeEditor;