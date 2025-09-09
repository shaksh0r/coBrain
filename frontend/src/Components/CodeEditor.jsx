import React, { useRef, useEffect, useState } from 'react';
import Editor from '@monaco-editor/react';
import * as monaco from 'monaco-editor';
import { connectWebSocket, disconnectWebSocket, sendCode, requestDocumentState } from '../API/crdtwebsocket';
import CopyButton from './buttons/CopyButton';
import ContainerButton from './buttons/ContainerButton';
import { useIDEContext } from '../Context/IDEContext';
import '../stylesheets/CodeEditor.css';

const CodeEditor = () => {
    const { sessionID, fileNameToFileId, setFileNameToFileId, activeFileId, setActiveFileId, stompClientRef, clientIdRef, language } = useIDEContext();
    const editorRef = useRef(null);
    const isProgrammaticChange = useRef(false);
    const decorationsRef = useRef([]);
    const [breakpoints, setBreakpoints] = useState(new Set());

    useEffect(() => {
        const client = connectWebSocket(
            (fileID, message) => {
                if (Array.from(fileNameToFileId.values()).includes(fileID)) {
                    if (fileID === activeFileId) {
                        handleRemoteOperation(fileID, message);
                    } else {
                        console.log('Operation ignored - fileID does not match activeFileId:', fileID, activeFileId);
                    }
                } else {
                    console.warn('Operation received for unknown fileID:', fileID);
                }
            }
        );
        stompClientRef.current = client;
        return () => {
            console.log("Disconnecting WebSocket...");
            disconnectWebSocket(client);
        };
    }, [sessionID, setFileNameToFileId, setActiveFileId, fileNameToFileId]);

    const handleTabSwitch = async (fileID) => {
        clearBreakpoints();
        const content = await requestDocumentState(sessionID, fileID, clientIdRef);
        loadDocument(content);
        setActiveFileId(fileID);
    };

    const handleCloseTab = (fileName) => {

        if ( Array.from(fileNameToFileId.entries()).length === 1){
            window.location.reload();
            return;
        }

        const fileID = fileNameToFileId.get(fileName);
        setFileNameToFileId((prev) => {
            const newMap = new Map(prev);
            newMap.delete(fileName);
            return newMap;
        });
        if (fileID === activeFileId) {
            const remainingFiles = Array.from(fileNameToFileId.entries());
            const newActive = remainingFiles.find(([name, id]) => id !== fileID);
            if (newActive) {
                handleTabSwitch(newActive[1]);
            } else {
                setActiveFileId(null);
            }
        }
    };

    const onMount = async (editor) => {
        editorRef.current = editor;
        editor.focus();
        console.log("Editor mounted");

        const content = await requestDocumentState(sessionID, activeFileId, clientIdRef);
        loadDocument(content);

        editor.updateOptions({
            glyphMargin: true,
            lineNumbersMinChars: 5,
        });

        editor.onMouseDown((e) => {
            if (e.target.type === monaco.editor.MouseTargetType.GUTTER_GLYPH_MARGIN) {
                const lineNumber = e.target.position.lineNumber;
                toggleBreakpoint(lineNumber);
            }
        });

        const model = editor.getModel();
        if (model) {
            model.onDidChangeContent(() => {
                updateBreakpointDecorations();
            });
        }
    };

    const toggleBreakpoint = (lineNumber) => {
        setBreakpoints((prev) => {
            const newBreakpoints = new Set(prev);
            if (newBreakpoints.has(lineNumber)) {
                newBreakpoints.delete(lineNumber);
            } else {
                newBreakpoints.add(lineNumber);
            }
            updateBreakpointDecorations(newBreakpoints);
            return newBreakpoints;
        });
    };

    const clearBreakpoints = () => {
        setBreakpoints(new Set());
        updateBreakpointDecorations(new Set());
    };

    const updateBreakpointDecorations = (bps = breakpoints) => {
        const model = editorRef.current?.getModel();
        if (!model) return;

        decorationsRef.current = editorRef.current.deltaDecorations(decorationsRef.current, []);

        const newDecorations = Array.from(bps).map((lineNumber) => ({
            range: new monaco.Range(lineNumber, 1, lineNumber, 1),
            options: {
                isWholeLine: true,
                glyphMarginClassName: 'breakpoint',
                linesDecorationsClassName: 'breakpoint-line',
                glyphMarginHoverMessage: { value: 'Breakpoint' }, // Fixed: Use plain object for hover message
            },
        }));

        decorationsRef.current = editorRef.current.deltaDecorations([], newDecorations);
    };

    const handleRemoteOperation = (fileID, msg) => {
        try {
            const op = JSON.parse(msg);

            if (op.clientId && op.clientId === clientIdRef.current) {
                return;
            }

            if (fileID !== activeFileId || !editorRef.current) {
                return;
            }

            const model = editorRef.current.getModel();
            if (!model) return;

            isProgrammaticChange.current = true;

            if (op.op === "insert") {
                const pos = model.getPositionAt(op.index);
                model.applyEdits([
                    {
                        range: new monaco.Range(pos.lineNumber, pos.column, pos.lineNumber, pos.column),
                        text: op.value,
                        forceMoveMarkers: true,
                    },
                ]);
            } else if (op.op === "delete") {
                const start = model.getPositionAt(op.index);
                const end = model.getPositionAt(op.index + op.length);
                model.applyEdits([
                    {
                        range: new monaco.Range(start.lineNumber, start.column, end.lineNumber, end.column),
                        text: "",
                        forceMoveMarkers: true,
                    },
                ]);
            } else if (op.op === "replace") {
                const start = model.getPositionAt(op.index);
                const end = model.getPositionAt(op.index + op.length);
                model.applyEdits([
                    {
                        range: new monaco.Range(start.lineNumber, start.column, end.lineNumber, end.column),
                        text: op.value,
                        forceMoveMarkers: true,
                    },
                ]);
            }

            console.log("applying op", op);

            isProgrammaticChange.current = false;

            updateBreakpointDecorations();
        } catch (e) {
            console.error("Bad operation:", msg, e);
        }
    };

    const handleEditorChange = (newValue, event) => {
        if (isProgrammaticChange.current || !activeFileId) {
            return;
        }

        const model = editorRef.current?.getModel();
        if (!model || !event?.changes) return;

        event.changes.forEach((change) => {
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

            sendCode(stompClientRef.current, activeFileId, op);
        });

        updateBreakpointDecorations();
    };

    const loadDocument = (content) => {
        const model = editorRef.current.getModel();
        if (model) {
            isProgrammaticChange.current = true;
            model.applyEdits([
                {
                    range: model.getFullModelRange(),
                    text: content || '',
                    forceMoveMarkers: true,
                },
            ]);
            isProgrammaticChange.current = false;
        }
    };

    useEffect(() => {
        console.log('Current breakpoints:', Array.from(breakpoints));
    }, [breakpoints]);

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
                    forceMoveMarkers: true,
                },
            ]);
            const endPosition = model.getPositionAt(model.getValue().length);
            editor.setPosition(endPosition);
            await new Promise((res) => setTimeout(res, delay));
        }
    };

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
    };

    const getEditorContent = () => {
        return editorRef.current?.getModel()?.getValue() || "";
    };

    return (
        <div style={{ display: 'flex', flexDirection: 'column', height: '80vh' }}>
            <div
                style={{
                    height: '5%',
                    backgroundColor: 'rgba(24, 24, 24, 1)',
                    display: 'flex',
                    alignItems: 'center',
                    padding: '0 16px',
                }}
            >
                <div style={{ flexGrow: 1 }}>
                    {Array.from(fileNameToFileId.entries()).map(([fileName, fileID]) => (
                        <span key={fileID} style={{ marginRight: '8px' }}>
                            <button
                                onClick={() => handleTabSwitch(fileID)}
                                style={{
                                    background: fileID === activeFileId ? '#555' : '#333',
                                    color: 'white',
                                    border: 'none',
                                    padding: '4px 8px',
                                }}
                            >
                                {fileName}
                            </button>
                            <button
                                onClick={() => handleCloseTab(fileName)}
                                style={{
                                    background: '#333',
                                    color: 'white',
                                    border: 'none',
                                    padding: '4px 8px',
                                }}
                            >
                                x
                            </button>
                        </span>
                    ))}
                    <button
                        onClick={() => {
                            const fileName = prompt("Enter the name of the file to open:");
                            if (fileName) {
                                openFile(fileName);
                            }
                        }}

                        style={{ color: 'white', backgroundColor: 'rgba(66, 66, 66, 1)' }}
                    >
                        +
                    </button>
                </div>
                <button
                    onClick={() =>
                        autoType(
                            'Hello, this is auto-typing!\nMy name is Gawwy and I love coding in java because it is so much fun!!!\n',
                            100
                        )
                    }
                    style={{
                        background: '#333',
                        color: 'white',
                        border: 'none',
                        padding: '4px 8px',
                        marginRight: '8px',
                    }}
                >
                    AutoType Demo
                </button>
                <ContainerButton />
                <CopyButton
                    getEditorContent={getEditorContent}
                />
            </div>
            <div style={{ height: '95%' }}>
                {activeFileId ? (
                    <Editor
                        height="100%"
                        theme="vs-dark"
                        language={language}
                        onMount={onMount}
                        onChange={handleEditorChange}
                    />
                ) : (
                    <div
                    style={{
                        top: 0,
                        left: 0,
                        height: '100%',
                        backgroundColor: 'rgba(30, 30, 30, 1)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: 'rgba(104, 104, 104, 0.5)',
                        fontSize: '1.2rem',
                        zIndex: 1,
                    }}
                >
                    No file selected. Open a file to start editing.
                </div>
                )}
            </div>
        </div>
    );
};

export default CodeEditor;