import React, { useRef, useEffect, useState } from 'react';
import Editor from '@monaco-editor/react';
import { Range, editor as EditorNamespace } from 'monaco-editor/esm/vs/editor/editor.api';
import { connectWebSocket, disconnectWebSocket, sendCode, requestDocumentState, getAllFiles } from '../API/crdtwebsocket';
import CopyButton from './buttons/CopyButton';
import ContainerButton from './buttons/ContainerButton';
import DebugWindow from './DebugWindow';
import { useIDEContext } from '../Context/IDEContext';
import '../stylesheets/CodeEditor.css';

const CodeEditor = () => {
    const { sessionID, fileNameToFileId, setFileNameToFileId, activeFileId, setActiveFileId, getFileIcon,
          breakpoints, setBreakpoints, stompClientRef, clientIdRef, language, setExplorerFiles } = useIDEContext();
    const editorRef = useRef(null);
    const isProgrammaticChange = useRef(false);
    const decorationsRef = useRef([]);
    const ghostDecorationRef = useRef([]);
    const breakpointsRef = useRef(breakpoints);
    const [isDebugOpen, setIsDebugOpen] = useState(false);

    useEffect(() => {
        const client = connectWebSocket(
            (fileID, sessID, message) => {
                if (sessID !== sessionID) {
                    console.log('Operation ignored - sessionID does not match');
                    return;
                }
                if (Array.from(fileNameToFileId.values()).includes(fileID)) {
                    if (fileID === activeFileId) {
                        handleRemoteOperation(fileID, message);
                    } else {
                        console.log('Operation ignored - fileID does not match activeFileId:', fileID, "!==", activeFileId);
                    }
                } else {
                    console.warn('Operation received for unknown fileID:', fileID);
                }
            },
            null,
            (fileEvent) => {
                if (fileEvent.action === "add") {
                    setExplorerFiles(prev => [...prev, { fileName: fileEvent.fileName, fileID: fileEvent.fileID }]);
                } else if (fileEvent.action === "delete") {
                    if (Array.isArray(fileEvent.fileNames) && Array.isArray(fileEvent.fileIDs)) {
                        fileEvent.fileNames.forEach((fileName) => handleCloseTab(fileName));
                        setExplorerFiles(prev => prev.filter(f => !fileEvent.fileIDs.includes(f.fileID)));
                    }
                }
            },
            clientIdRef,
            sessionID
        );
        stompClientRef.current = client;
        return () => {
            console.log("Disconnecting WebSocket...");
            disconnectWebSocket(client);
        };
    }, [sessionID, setFileNameToFileId, setActiveFileId, fileNameToFileId, activeFileId]);

    const handleTabSwitch = async (fileID) => {
        if (!fileID || !sessionID || editorRef.current === null) {
            return;
        }
        clearBreakpoints();
        const content = await requestDocumentState(sessionID, fileID, clientIdRef);
        loadDocument(content);
        setActiveFileId(fileID);
    };

    useEffect(() => {
        handleTabSwitch(activeFileId);
    }, [activeFileId]);

    const handleCloseTab = (fileName) => {
        console.log("Closing tab for file:", fileName);
        const fileID = fileNameToFileId.get(fileName);
        if (!fileID) return;
        setFileNameToFileId((prev) => {
            const newMap = new Map(prev);
            newMap.delete(fileName);
            return newMap;
        });
        if (fileID === activeFileId) {
            setActiveFileId(null);
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
            glyphMarginWidth: 28,
            lineNumbersMinChars: 5,
        });

        editor.onMouseDown((e) => {
            if (e.target.type === EditorNamespace.MouseTargetType.GUTTER_GLYPH_MARGIN) {
                const lineNumber = e.target.position.lineNumber;
                toggleBreakpoint(lineNumber);
            }
        });

        editor.onMouseMove((e) => {
            if (e.target.type === EditorNamespace.MouseTargetType.GUTTER_GLYPH_MARGIN) {
                const lineNumber = e.target.position.lineNumber;
                if (!breakpointsRef.current.has(lineNumber)) {
                    ghostDecorationRef.current = editor.deltaDecorations(ghostDecorationRef.current, [{
                        range: new Range(lineNumber, 1, lineNumber, 1),
                        options: {
                            glyphMarginClassName: 'ghost-breakpoint',
                        },
                    }]);
                } else {
                    ghostDecorationRef.current = editor.deltaDecorations(ghostDecorationRef.current, []);
                }
            } else {
                ghostDecorationRef.current = editor.deltaDecorations(ghostDecorationRef.current, []);
            }
        });

        editor.onMouseLeave(() => {
            ghostDecorationRef.current = editor.deltaDecorations(ghostDecorationRef.current, []);
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
            range: new Range(lineNumber, 1, lineNumber, 1),
            options: {
                isWholeLine: true,
                glyphMarginClassName: 'breakpoint',
                linesDecorationsClassName: 'breakpoint-line',
                // glyphMarginHoverMessage: { value: 'Breakpoint' },
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
                        range: new Range(pos.lineNumber, pos.column, pos.lineNumber, pos.column),
                        text: op.value,
                        forceMoveMarkers: true,
                    },
                ]);
            } else if (op.op === "delete") {
                const start = model.getPositionAt(op.index);
                const end = model.getPositionAt(op.index + op.length);
                model.applyEdits([
                    {
                        range: new Range(start.lineNumber, start.column, end.lineNumber, end.column),
                        text: "",
                        forceMoveMarkers: true,
                    },
                ]);
            } else if (op.op === "replace") {
                const start = model.getPositionAt(op.index);
                const end = model.getPositionAt(op.index + op.length);
                model.applyEdits([
                    {
                        range: new Range(start.lineNumber, start.column, end.lineNumber, end.column),
                        text: op.value,
                        forceMoveMarkers: true,
                    },
                ]);
            }
            isProgrammaticChange.current = false;
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
            sendCode(stompClientRef.current, activeFileId, sessionID, op);
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
        breakpointsRef.current = breakpoints;
        // console.log('Current breakpoints:', Array.from(breakpoints));
    }, [breakpoints]);

    const autoType = async (text, delay = 100) => {
        const model = editorRef.current?.getModel();
        const editor = editorRef.current;
        if (!model || !editor) return;
        for (let i = 0; i < text.length; i++) {
            model.applyEdits([
                {
                    range: new Range(
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

    return (
        <div className="code-editor-container">
            <div className="code-editor-top-bar">
                <div className="code-editor-tabs">
                    {Array.from(fileNameToFileId.entries()).map(([fileName, fileID]) => (
                        <button
                            key={fileID}
                            onClick={() => handleTabSwitch(fileID)}
                            className={fileID === activeFileId ? 'code-editor-tab-button active' : 'code-editor-tab-button'}
                        >
                            <span className="tab-content">
                                <span className="tab-file-icon">{getFileIcon(fileName)}</span>
                                {fileName}
                            </span>
                            <span
                                className="code-editor-close-icon"
                                onClick={(e) => {
                                    e.stopPropagation();
                                    handleCloseTab(fileName);
                                }}
                            >
                                ⨉
                            </span>
                        </button>
                    ))}
                </div>
                <button
                    onClick={() => setIsDebugOpen(true)}
                    className="code-editor-action-button margin-right"
                >
                    Watches
                </button>
                <button
                    onClick={() =>
                        autoType(
                            'Hello, this is auto-typing!\nMy name is Gawwy and I love coding in java because it is so much fun!!!\n',
                            100
                        )
                    }
                    className="code-editor-action-button margin-right"
                >
                    AutoType Demo
                </button>
                <ContainerButton />
                <CopyButton />
                <button
                    onClick={async () => {
                        const files = await getAllFiles();
                        console.log(files);
                    }}
                    className="code-editor-action-button margin-left"
                >
                    Get All Files
                </button>
            </div>
            <div className="code-editor-area">
                {activeFileId ? (
                    <Editor
                        height="100%"
                        theme="vs-dark"
                        language={language}
                        onMount={onMount}
                        onChange={handleEditorChange}
                    />
                ) : (
                    <div className="code-editor-no-file">
                        No file selected. Open a file to start editing.
                    </div>
                )}
            </div>
            <DebugWindow isOpen={isDebugOpen} onClose={() => setIsDebugOpen(false)} />
        </div>
    );
};

export default CodeEditor;