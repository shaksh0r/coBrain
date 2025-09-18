import React from 'react';
import '../stylesheets/FileExplorerPanel.css';
import { useIDEContext } from '../Context/IDEContext.jsx';
import { deleteFile } from '../API/crdtwebsocket.js';

const FileExplorerPanel = () => {
    const { setActiveFileId, explorerFiles, showFileExplorer, sessionID, getFileIcon, selectedFiles,
            setSelectedFiles, setFileNameToFileId, fileNameToFileId, activeFileId, setExplorerFiles } = useIDEContext();

    if (!showFileExplorer) return null;

    const handleFileClick = async (fileName, fileID) => {
        try {
            setFileNameToFileId(prev => new Map(prev).set(fileName, fileID));
            setActiveFileId(fileID);
        } catch (error) {
            console.error('Error opening file:', error);
        }
    };

    const handleCheckboxChange = (fileName) => {
        setSelectedFiles(prev => {
            const newSet = new Set(prev);
            if (newSet.has(fileName)) {
                newSet.delete(fileName);
            } else {
                newSet.add(fileName);
            }
            return newSet;
        });
    };

    const handleDeleteSelected = async () => {
        if (selectedFiles.size === 0) return;
        try {
            await deleteFile(sessionID, Array.from(selectedFiles));
            if (Array.from(selectedFiles).some(fileName => fileNameToFileId.get(fileName) === activeFileId)) {
                setActiveFileId(null);
            }
            setFileNameToFileId(prev => {
                const newMap = new Map(prev);
                selectedFiles.forEach(fileName => newMap.delete(fileName));
                return newMap;
            });
            setExplorerFiles(prev => prev.filter(f => !selectedFiles.has(f.fileName)));
            setSelectedFiles(new Set());
        } catch (error) {
            console.error('Error deleting files:', error);
        }
    }

    return (
        <div className="file-explorer-panel">
            <div className="file-explorer-header">
                File Explorer
                <button onClick={handleDeleteSelected} disabled={selectedFiles.size === 0} title="Delete Selected Files">
                    <span className="file-explorer-delete-icon">🗑️</span>
                </button>
            </div>
            <div className="file-explorer-content">
                {Array.isArray(explorerFiles) && explorerFiles.length > 0 ? (
                    <ul className="file-explorer-list">
                        {explorerFiles.map((file, idx) => {
                            const fileName = file.fileName;
                            const fileID = file.fileID;
                            return (
                                <li
                                    key={fileName || idx}
                                    className="file-explorer-item"
                                    onClick={() => handleFileClick(fileName, fileID)}
                                >
                                    <span className="file-explorer-file-icon">{getFileIcon(fileName)}</span>
                                    <span className="file-explorer-file-name">{fileName}</span>
                                    <input
                                        type="checkbox"
                                        checked={selectedFiles.has(fileName)}
                                        onChange={() => handleCheckboxChange(fileName)}
                                        style={{ marginRight: 8 }}
                                        onClick={e => e.stopPropagation()}
                                    />
                                </li>
                            );
                        })}
                    </ul>
                ) : (
                    <div className="file-explorer-empty">No files to display.</div>
                )}
            </div>
        </div>
    );
};

export default FileExplorerPanel;