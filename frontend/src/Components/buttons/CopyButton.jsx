import React from 'react';
import { copyCode } from '../../API/restapi';


const CopyButton = ({ sessionID, language, getEditorContent }) => {
    return (
        <button onClick={() => copyCode(sessionID, language, getEditorContent()).then(response => {
            console.log(response);
        })}>
            Copy Code
        </button>
    );
};

export default CopyButton;
