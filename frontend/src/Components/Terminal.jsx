import React, { useRef, useEffect, useState } from 'react';
import { connectTerminal, disconnectTerminal, sendInput } from '../API/iowebsocket';
import { useIDEContext } from '../Context/IDEContext';

const Terminal = () => {

    const { sessionID, language } = useIDEContext();
    const socketRef = useRef(null);
    const outputRef = useRef(null);
    const [outputLines, setOutputLines] = useState([]);
    const [command, setCommand] = useState('');

    useEffect(() => {
        const socket = connectTerminal((message) => {
            setOutputLines((prev) => [...prev, message]);
        }, language, sessionID);

        socketRef.current = socket;

        return () => {
            disconnectTerminal(socket);
        };
    }, [language, sessionID]);

    useEffect(() => {
        if (outputRef.current) {
            const isNearBottom = outputRef.current.scrollHeight - outputRef.current.scrollTop - outputRef.current.clientHeight < 20;
            if (isNearBottom) {
                outputRef.current.scrollTop = outputRef.current.scrollHeight;
            }
        }
    }, [outputLines]);

    const handleKeyDown = (e) => {
        if (e.key === 'Enter') {
            if (command.trim()) {
                setOutputLines((prev) => [...prev, `> ${command}`]);
                sendInput(socketRef.current, command + '\n');
                setCommand('');
            }
        }
    };

    return (
        <div style={{
            display: 'flex',
            flexDirection: 'column',
            height: '20vh',
            borderTop: '1px solid rgba(43, 43, 43, 1)',
            backgroundColor: 'rgba(24, 24, 24, 1)',
            color: 'rgba(204, 204, 204, 1)',
            fontFamily: 'monospace',
            fontSize: '14px'
        }}>
            <div
                ref={outputRef}
                style={{
                    flex: 1,
                    overflowY: 'auto',
                    padding: '10px',
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-all'
                }}
            >
                {outputLines.map((line, index) => (
                    <div key={index}>{line}</div>
                ))}
            </div>
            <input
                type="text"
                value={command}
                onChange={(e) => setCommand(e.target.value)}
                onKeyDown={handleKeyDown}
                style={{
                    backgroundColor: 'rgba(20, 20, 20, 1)',
                    color: 'rgba(204, 204, 204, 1)',
                    border: 'none',
                    borderTop: '1px solid rgba(43, 43, 43, 1)',
                    padding: '10px',
                    outline: 'none'
                }}
                placeholder=">"
            />
        </div>
    );
};

export default Terminal;