import React from 'react';
import { useIDEContext } from '../../Context/IDEContext';
import { copyCode } from '../../API/restapi';

const CopyButton = ({ getEditorContent }) => {
    const { sessionID, language, activeFileId, fileNameToFileId } = useIDEContext();

    const handleClick = async () => {
        if (!activeFileId) {
            console.log("No active file to copy");
            return;
        }
        const fileName = Array.from(fileNameToFileId.entries()).find(
            ([, id]) => id === activeFileId
        )?.[0];
        if (!fileName) {
            console.log("Active file not found");
            return;
        }
        const content = getEditorContent();
        const dirContent = [{ fileName, content }];
        await copyCode(sessionID, language, dirContent);
    };

    return (
        <button
            onClick={handleClick}
            style={{
                background: '#333',
                color: 'white',
                border: 'none',
                padding: '4px 8px',
            }}
        >
            Copy Code
        </button>
    );
};

export default CopyButton;