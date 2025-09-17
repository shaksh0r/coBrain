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
    openFile: async (fileName) => {},
    explorerFiles: [],
    setExplorerFiles: () => {},
    showFileExplorer: false,
    setShowFileExplorer: () => {},
});

export const useIDEContext = () => useContext(IDEContext);

export default IDEContext;