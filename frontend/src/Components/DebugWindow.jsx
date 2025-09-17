import React, { useState, useRef } from 'react';
import '../stylesheets/DebugWindow.css';

const DebugWindow = ({ isOpen, onClose }) => {
    // Sample watch data; replace with actual debugging state or props
    const [watches] = useState([
        { variable: 'counter', value: '42', type: 'int' },
        { variable: 'message', value: '"Hello World"', type: 'string' },
        { variable: 'isActive', value: 'true', type: 'bool' },
        { variable: 'array', value: '[1, 2, 3]', type: 'array' },
    ]);

    // State for modal position
    const [position, setPosition] = useState({ x: 0, y: 0 });
    const [isDragging, setIsDragging] = useState(false);
    const dragRef = useRef({ startX: 0, startY: 0 });

    if (!isOpen) return null;

    const handleMouseDown = (e) => {
        // Prevent default to avoid text selection during drag
        e.preventDefault();
        setIsDragging(true);
        dragRef.current = {
            startX: e.clientX - position.x,
            startY: e.clientY - position.y,
        };
    };

    const handleMouseMove = (e) => {
        if (isDragging) {
            setPosition({
                x: e.clientX - dragRef.current.startX,
                y: e.clientY - dragRef.current.startY,
            });
        }
    };

    const handleMouseUp = () => {
        setIsDragging(false);
    };

    return (
        <div
            className="debug-modal"
            onMouseMove={handleMouseMove}
            onMouseUp={handleMouseUp}
        >
            <div
                className="debug-modal-content"
                style={{
                    transform: `translate(${position.x}px, ${position.y}px)`,
                }}
            >
                <div
                    className="debug-modal-header"
                    onMouseDown={handleMouseDown}
                >
                    <h2 className="debug-modal-title">Watches</h2>
                    <button className="debug-modal-close" onClick={onClose}>
                        &times;
                    </button>
                </div>
                <div className="debug-modal-body">
                    <table className="debug-table">
                        <thead>
                            <tr>
                                <th>Variable</th>
                                <th>Value</th>
                                <th>Type</th>
                            </tr>
                        </thead>
                        <tbody>
                            {watches.length > 0 ? (
                                watches.map((watch, index) => (
                                    <tr key={index}>
                                        <td>{watch.variable}</td>
                                        <td>{watch.value}</td>
                                        <td>{watch.type}</td>
                                    </tr>
                                ))
                            ) : (
                                <tr>
                                    <td colSpan="3" className="debug-table-empty">
                                        No watches set. Add variables to watch during debugging.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
                <div className="debug-modal-footer">
                    <button className="debug-modal-button" onClick={onClose}>
                        Close
                    </button>
                </div>
            </div>
        </div>
    );
};

export default DebugWindow;