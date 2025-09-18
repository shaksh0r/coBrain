import { createContext, useContext } from 'react';

const IDEContext = createContext({
    clientIdRef: { current: null },
    stompClientRef: { current: null },
    userName: '',
    setUserName: () => {},
    sessionID: '',
    setSessionID: () => {},
    fileNameToFileId: new Map(),
    setFileNameToFileId: () => {},
    activeFileId: null,
    setActiveFileId: () => {},
    language: 'plaintext',
    explorerFiles: [],
    setExplorerFiles: () => {},
    showFileExplorer: false,
    setShowFileExplorer: () => {},
    selectedFiles: new Set(),
    setSelectedFiles: () => {},
    getFileIcon: () => null,
});

export const useIDEContext = () => useContext(IDEContext);

export default IDEContext;