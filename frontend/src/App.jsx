import './App.css';
import { useState, useRef } from 'react';
import { v4 as uuidv4 } from 'uuid';
import CodeEditor from './Components/CodeEditor.jsx';
import Terminal from './Components/Terminal.jsx';

function App() {
    const clientIdRef = useRef(uuidv4());
	const [sessionID, setSessionID] = useState("1");
	const [language, setLanguage] = useState("java");

   return (
		<div style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
			<CodeEditor clientIdRef={clientIdRef} sessionID={sessionID} language={language} />
			<Terminal sessionID={sessionID} language={language} />
		</div>
	);
}

export default App;