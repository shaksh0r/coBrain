import React from 'react';
import { useIDEContext } from '../../Context/IDEContext';
import { copyCode, getDirContent } from '../../API/restapi';

const CopyButton = ({ getEditorContent }) => {
    const { sessionID, language } = useIDEContext();

    const handleClick = async () => {
        const dirContent = await getDirContent(sessionID);
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