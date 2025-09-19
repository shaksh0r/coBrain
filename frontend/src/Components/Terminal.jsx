import React, { useRef, useEffect, useState } from 'react';
import { connectTerminal, disconnectTerminal, sendInput } from '../API/iowebsocket';
import { connectDebug, disconnectDebug, sendDebugCmd } from '../API/debugwebsocket';
import { connectProblems, disconnectProblems } from '../API/problemswebsocket';
import { useIDEContext } from '../Context/IDEContext';
import '../stylesheets/Terminal.css';

const Terminal = () => {
    const { language, clientIdRef } = useIDEContext();
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
        }, language, clientIdRef.current);

        terminalSocketRef.current = terminalSocket;

        const debugSocket = connectDebug((message) => {
            setDebugOutputLines((prev) => [...prev, message]);
        }, language, clientIdRef.current);

        debugSocketRef.current = debugSocket;

        const problemSocket = connectProblems((message) => {
            setProblemsOutputLines((prev) => [...prev, message]);
        }, language, clientIdRef.current);

        problemsSocketRef.current = problemSocket;

        return () => {
            disconnectTerminal(terminalSocket);
            disconnectDebug(debugSocket);
            disconnectProblems(problemSocket);
        };
    }, [language, clientIdRef]);

    useEffect(() => {
    }, [activeTab]);

    const handleKeyDown = (e) => {
        if (e.key === 'Enter') {
            if (activeTab === 'terminal' && terminalCommand.trim()) {
                setTerminalOutputLines((prev) => [...prev, `> ${terminalCommand}`]);
                sendInput(terminalSocketRef.current, terminalCommand + '\n');
                setTerminalCommand('');
            } else if (activeTab === 'debug' && debugCommand.trim()) {
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
                        <div className="terminal-output">
                            {terminalOutputLines.map((line, index) => (
                                <div key={index}>{line}</div>
                            ))}
                        </div>
                        <input
                            type="text"
                            value={terminalCommand}
                            onChange={(e) => setTerminalCommand(e.target.value)}
                            onKeyDown={handleKeyDown}
                            className="terminal-input"
                            placeholder=">"
                        />
                    </>
                );
            case 'debug':
                return (
                    <>
                        <div className="terminal-output">
                            {debugOutputLines.map((line, index) => (
                                <div key={index}>{line}</div>
                            ))}
                        </div>
                        <input
                            type="text"
                            value={debugCommand}
                            onChange={(e) => setDebugCommand(e.target.value)}
                            onKeyDown={handleKeyDown}
                            className="terminal-input"
                            placeholder=">"
                        />
                    </>
                );
            case 'problems':
                return (
                    <>
                        <div className="terminal-output">
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
        <div className="terminal-container">
            <div className="terminal-tabs">
                {['problems', 'debug', 'terminal'].map((tab) => (
                    <button
                        key={tab}
                        onClick={() => setActiveTab(tab)}
                        className={activeTab === tab ? 'terminal-tab-button active' : 'terminal-tab-button'}
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