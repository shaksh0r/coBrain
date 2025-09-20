import React, { useState, useRef } from 'react';
import { useIDEContext } from '../Context/IDEContext';
import '../stylesheets/DebugWindow.css';

const DebugWindow = ({ isOpen, onClose }) => {
    const { watches } = useIDEContext();

    const [position, setPosition] = useState({ x: 0, y: 0 });
    const [isDragging, setIsDragging] = useState(false);
    const dragRef = useRef({ startX: 0, startY: 0 });

    if (!isOpen) return null;

    const handleMouseDown = (e) => {
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
                            </tr>
                        </thead>
                        <tbody>
                            {watches.length > 0 ? (
                                watches.map((watch, index) => (
                                    <tr key={index}>
                                        <td>{watch.variable}</td>
                                        <td>{watch.value}</td>
                                    </tr>
                                ))
                            ) : (
                                <tr>
                                    <td colSpan="2" className="debug-table-empty">
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