import React from 'react'
import { useRef, useState } from 'react';
import Editor from '@monaco-editor/react';
import SendBack from './SendBack';

const CodeEditor = () => {
    const editorRef = useRef(null);
    const [value, setValue] = useState("");
    const onMount = (editor) => {
        editorRef.current = editor;
        editor.focus();
    }
    const str = "import React from 'react'\nprint('Hello, World')\n";
    return (
        <div style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
            <div style={{
                height: '5%',
                backgroundColor: 'rgba(24, 24, 24, 1)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'flex-end',
                paddingRight: '16px' // optional, for spacing from the edge
                }}>
                <SendBack editorRef={editorRef} language={'python'} />
            </div>
            <div style={{ height: '95%'}}>
                <Editor
                    height="100%"
                    theme="vs-dark"
                    defaultLanguage="python"
                    defaultValue={str}
                    onMount={onMount}
                    value={value}
                    onChange={(newValue) => setValue(newValue)}
                />
            </div>
        </div>
    )
}

export default CodeEditor

