import { createContext, useContext } from 'react';

const IDEContext = createContext({
    sessionID: '',
    fileNameToFileId: new Map(),
    setFileNameToFileId: () => {},
    activeFileId: null,
    setActiveFileId: () => {},
    openFile: () => {},
    stompClientRef: { current: null },
    clientIdRef: { current: null },
    language: 'plaintext',
});

export const useIDEContext = () => useContext(IDEContext);

export default IDEContext;