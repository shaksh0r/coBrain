import React from 'react';
import '../stylesheets/FileExplorerPanel.css';
import { SiJavascript, SiReact, SiPython, SiTypescript, SiHtml5, SiCss3, SiJson, SiMarkdown } from 'react-icons/si';
import { FaFileAlt, FaJava } from 'react-icons/fa';
import { useIDEContext } from '../Context/IDEContext.jsx';

const getFileIcon = (fileName) => {
    if (!fileName || typeof fileName !== 'string') return <FaFileAlt />;
    const ext = fileName.split('.').pop().toLowerCase();
    switch (ext) {
        case 'js':
            return <SiJavascript color="#f7e018" />;
        case 'jsx':
            return <SiReact color="#61dafb" />;
        case 'ts':
        case 'tsx':
            return <SiTypescript color="#3178c6" />;
        case 'java':
            return <FaJava color="#e76f00" />;
        case 'py':
            return <SiPython color="#3776ab" />;
        case 'html':
            return <SiHtml5 color="#e34c26" />;
        case 'css':
            return <SiCss3 color="#1572b6" />;
        case 'json':
            return <SiJson color="#cbcb41" />;
        case 'md':
            return <SiMarkdown color="#4a4a4a" />;
        default:
            return <FaFileAlt />;
    }
};

const FileExplorerPanel = () => {
    const { openFile, setActiveFileId, explorerFiles, showFileExplorer } = useIDEContext();

    if (!showFileExplorer) return null;

    const handleFileClick = async (fileName, fileId) => {
        try {
            const fileID = await openFile(fileName);
            setActiveFileId(fileID);
        } catch (error) {
            console.error('Error opening file:', error);
        }
    };

    return (
        <div className="file-explorer-panel">
            <div className="file-explorer-header">File Explorer</div>
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
                                    style={{ cursor: 'pointer' }}
                                >
                                    <span className="file-explorer-file-icon">{getFileIcon(fileName)}</span>
                                    <span className="file-explorer-file-name">{fileName}</span>
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