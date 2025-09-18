import React from 'react';
import { useIDEContext } from '../../Context/IDEContext';
import { copyCode } from '../../API/container';
import { zipDirectoryContent } from '../util/util';

const CopyButton = () => {
    const { sessionID, language, explorerFiles, clientIdRef } = useIDEContext();

    const handleClick = async () => {
        const zipContent = await zipDirectoryContent(explorerFiles, sessionID, clientIdRef, 'base64');
        await copyCode(sessionID, language, zipContent);
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