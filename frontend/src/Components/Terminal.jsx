import React, { useRef, useEffect, useState } from 'react';
import { connectTerminal, disconnectTerminal, sendInput } from '../API/iowebsocket';
import { connectDebug, disconnectDebug, sendDebugCmd } from '../API/debugwebsocket';
import { connectProblems, disconnectProblems } from '../API/problemswebsocket';
import { useIDEContext } from '../Context/IDEContext';

const Terminal = () => {
    const { sessionID, language } = useIDEContext();
    const [activeTab, setActiveTab] = useState('terminal');

    const terminalSocketRef = useRef(null);
    const [terminalOutputLines, setTerminalOutputLines] = useState([]);
    const [terminalCommand, setTerminalCommand] = useState('');

    const debugSocketRef = useRef(null);
    const [debugOutputLines, setDebugOutputLines] = useState([]);
    const [debugCommand, setDebugCommand] = useState('');

    const problemsSocketRef = useRef(null);
    const [problemsOutputLines, setProblemsOutputLines] = useState([]);

    useEffect(() => {
        const terminalSocket = connectTerminal((message) => {
            setTerminalOutputLines((prev) => [...prev, message]);
        }, language, sessionID);

        terminalSocketRef.current = terminalSocket;

        const debugSocket = connectDebug((message) => {
            setDebugOutputLines((prev) => [...prev, message]);
        }, language, sessionID);

        debugSocketRef.current = debugSocket;

        const problemSocket = connectProblems((message) => {
            setProblemsOutputLines((prev) => [...prev, message]);
        }, language, sessionID);

        problemsSocketRef.current = problemSocket;

        return () => {
            disconnectTerminal(terminalSocket);
            disconnectDebug(debugSocket);
            disconnectProblems(problemSocket);
        };
    }, [language, sessionID]);

    useEffect(() => {

    }, [activeTab]);

    const handleKeyDown = (e) => {
        if (e.key === 'Enter') {
            if (activeTab === 'terminal' && terminalCommand.trim()) {
                setTerminalOutputLines((prev) => [...prev, `> ${terminalCommand}`]);
                sendInput(terminalSocketRef.current, terminalCommand + '\n');
                setTerminalCommand('');
            }
            else if (activeTab === 'debug' && debugCommand.trim()) {
                setDebugOutputLines((prev) => [...prev, `> ${debugCommand}`]);
                sendDebugCmd(debugSocketRef.current, debugCommand + '\n');
                setDebugCommand('');
            }
        }
    };

    const renderTabContent = () => {
        switch (activeTab) {
            case 'terminal':
                return (
                    <>
                        <div
                            style={{
                                flex: 1,
                                overflowY: 'auto',
                                padding: '10px',
                                whiteSpace: 'pre-wrap',
                                wordBreak: 'break-all',
                            }}
                        >
                            {terminalOutputLines.map((line, index) => (
                                <div key={index}>{line}</div>
                            ))}
                        </div>
                        <input
                            type="text"
                            value={terminalCommand}
                            onChange={(e) => setTerminalCommand(e.target.value)}
                            onKeyDown={handleKeyDown}
                            style={{
                                backgroundColor: 'rgba(20, 20, 20, 1)',
                                color: 'rgba(204, 204, 204, 1)',
                                border: 'none',
                                borderTop: '1px solid rgba(43, 43, 43, 1)',
                                padding: '10px',
                                outline: 'none',
                            }}
                            placeholder=">"
                        />
                    </>
                );
            case 'debug':
                return (
                    <>
                        <div
                            style={{
                                flex: 1,
                                overflowY: 'auto',
                                padding: '10px',
                                whiteSpace: 'pre-wrap',
                                wordBreak: 'break-all',
                            }}
                        >
                            {debugOutputLines.map((line, index) => (
                                <div key={index}>{line}</div>
                            ))}
                        </div>
                        <input
                            type="text"
                            value={debugCommand}
                            onChange={(e) => setDebugCommand(e.target.value)}
                            onKeyDown={handleKeyDown}
                            style={{
                                backgroundColor: 'rgba(20, 20, 20, 1)',
                                color: 'rgba(204, 204, 204, 1)',
                                border: 'none',
                                borderTop: '1px solid rgba(43, 43, 43, 1)',
                                padding: '10px',
                                outline: 'none',
                            }}
                            placeholder=">"
                        />
                    </>
                );
            case 'problems':
                return (
                    <>
                        <div
                            style={{
                                flex: 1,
                                overflowY: 'auto',
                                padding: '10px',
                                whiteSpace: 'pre-wrap',
                                wordBreak: 'break-all',
                            }}
                        >
                            {problemsOutputLines.map((line, index) => (
                                <div key={index}>{line}</div>
                            ))}
                        </div>
                    </>
                );
            default:
                return null;
        }
    };

    return (
        <div
            style={{
                display: 'flex',
                flexDirection: 'column',
                height: '25%',
                borderTop: '1px solid rgba(43, 43, 43, 1)',
                backgroundColor: 'rgba(24, 24, 24, 1)',
                color: 'rgba(204, 204, 204, 1)',
                fontFamily: 'monospace',
                fontSize: '14px'
            }}
        >
            <div
                style={{
                    height: '30px',
                    backgroundColor: 'rgba(24, 24, 24, 1)',
                    display: 'flex',
                    alignItems: 'center',
                    padding: '0 16px',
                    borderBottom: '1px solid rgba(43, 43, 43, 1)',
                }}
            >
                {['problems', 'debug', 'terminal',].map((tab) => (
                    <button
                        key={tab}
                        onClick={() => setActiveTab(tab)}
                        style={{
                            background: activeTab === tab ? '#555' : '#333',
                            color: 'white',
                            border: 'none',
                            padding: '4px 12px',
                            marginRight: '8px',
                            cursor: 'pointer',
                            fontFamily: 'monospace',
                            fontSize: '14px',
                        }}
                    >
                        {tab.toUpperCase()}
                    </button>
                ))}
            </div>
            {renderTabContent()}
        </div>
    );
};

export default Terminal;