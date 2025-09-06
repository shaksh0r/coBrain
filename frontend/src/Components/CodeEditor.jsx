import React, { useRef, useEffect } from 'react';
import Editor from '@monaco-editor/react';
import * as monaco from 'monaco-editor';
import { connectWebSocket, disconnectWebSocket, sendCode } from '../API/crdtwebsocket';
import CopyButton from './buttons/CopyButton';
import ContainerButton from './buttons/ContainerButton';

const CodeEditor = ({ clientIdRef, sessionID, language }) => {

    const editorRef = useRef(null);
    const stompClientRef = useRef(null);
    const isProgrammaticChange = useRef(false);

    useEffect(() => {
        console.log("Connecting to WebSocket...");
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
                                text: initialState || '',
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

    const handleRemoteOperation = (msg) => {
        try {
            console.log("Received operation:", msg);
            const op = JSON.parse(msg);

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

    const handleEditorChange = (newValue, event) => {
        if (isProgrammaticChange.current) {
            return;
        }

        const model = editorRef.current?.getModel();
        if (!model || !event?.changes) return;

        event.changes.forEach(change => {
            const start = model.getOffsetAt(change.range.getStartPosition());
            const op = { clientId: clientIdRef.current };

            if (change.text.length > 0 && change.rangeLength === 0) {
                op.op = "insert";
                op.index = start;
                op.value = change.text;
            } else if (change.text.length === 0 && change.rangeLength > 0) {
                op.op = "delete";
                op.index = start;
                op.length = change.rangeLength;
            } else {
                op.op = "replace";
                op.index = start;
                op.length = change.rangeLength;
                op.value = change.text;
            }

            console.log("Sending op:", op);
            sendCode(stompClientRef.current, JSON.stringify(op));
        });
    };

    const autoType = async (text, delay = 100) => {
        const model = editorRef.current?.getModel();
        const editor = editorRef.current;
        if (!model || !editor) return;
        for (let i = 0; i < text.length; i++) {
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
            const endPosition = model.getPositionAt(model.getValue().length);
            editor.setPosition(endPosition);
            await new Promise(res => setTimeout(res, delay));
        }
    };

    const getEditorContent = () => {
        return editorRef.current?.getModel()?.getValue() || "";
    };

    return (
        <div style={{ display: 'flex', flexDirection: 'column', height: '80vh' }}>
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
                <ContainerButton sessionID={sessionID} language={language} />
                <CopyButton sessionID={sessionID} language={language} getEditorContent={getEditorContent} />
            </div>
            <div style={{ height: '95%' }}>
                <style>
                    {`
                    .monaco-editor,
                    .monaco-editor-background {
                        background-color: #1e1e1e !important;
                    }
                    `}
                </style>
                <Editor
                    height="100%"
                    theme="vs-dark"
                    defaultLanguage={language}
                    onMount={onMount}
                    onChange={handleEditorChange}
                />
            </div>
        </div>
    );
};

export default CodeEditor;