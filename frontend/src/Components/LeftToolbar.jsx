import React from 'react';
import '../stylesheets/LeftToolbar.css';

const LeftToolbar = ({ activeKey, setActiveKey }) => {
    return (
        <div className="left-toolbar">
            <div className="left-toolbar-main">
                <button
                    className={`left-toolbar-button${activeKey === 'editor' ? ' active' : ''}`}
                    title="Code Editor"
                    onClick={() => {
                        setActiveKey('editor');
                    }}
                >
                    <span className="left-toolbar-icon">📄</span>
                </button>
                <button
                    className={`left-toolbar-button${activeKey === 'explorer' ? ' active' : ''}`}
                    title="File Explorer"
                    onClick={() => {
                        setActiveKey('explorer');
                    }}
                >
                    <span className="left-toolbar-icon">📂</span>
                </button>
                <button
                    className={`left-toolbar-button${activeKey === 'sessions' ? ' active' : ''}`}
                    title="Sessions"
                    onClick={() => {
                        setActiveKey('sessions');
                    }}
                >
                    <span className="left-toolbar-icon">👥</span>
                </button>
            </div>
            <div className="left-toolbar-bottom">
                <button
                    className={`left-toolbar-button${activeKey === 'help' ? ' active' : ''}`}
                    title="Help"
                    onClick={() => {
                        window.open('https://github.com/shaksh0r/coBrain/blob/main/README.md', '_blank', 'noopener,noreferrer');
                    }}
                >
                    <span className="left-toolbar-icon">❓</span>
                </button>
                <button
                    className={`left-toolbar-button left-toolbar-settings${activeKey === 'settings' ? ' active' : ''}`}
                    title="Settings"
                    onClick={() => {
                        console.log("Clicked settings");
                    }}
                >
                    <span className="left-toolbar-icon">⚙️</span>
                </button>
            </div>
        </div>
    );
};

export default LeftToolbar;
