import React from 'react';
import '../stylesheets/FileExplorerPanel.css';


const FileExplorerPanel = ({ visible, files }) => {
    if (!visible) return null;
    return (
        <div className="file-explorer-panel">
            <div className="file-explorer-header">File Explorer</div>
            <div className="file-explorer-content">
                {Array.isArray(files) && files.length > 0 ? (
                    <ul className="file-explorer-list">
                        {files.map((file, idx) => (
                            <li key={file.fileName || file.name || idx} className="file-explorer-item">
                                <span className="file-explorer-file-icon">📄</span>
                                <span className="file-explorer-file-name">{file.fileName || file.name || String(file)}</span>
                            </li>
                        ))}
                    </ul>
                ) : (
                    <div className="file-explorer-empty">No files to display.</div>
                )}
            </div>
        </div>
    );
};

export default FileExplorerPanel;
