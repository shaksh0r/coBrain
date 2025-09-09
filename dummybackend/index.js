const JSZip = require('jszip');
const express = require('express');
const cors = require('cors');
const WebSocket = require('ws'); // Import ws library
const app = express();

app.use(cors());
app.use(express.json());

// REST API endpoints
app.get("/getContainer/:sessionID/:language", (req, res) => {
    const { sessionID, language } = req.params;
    console.log(`Received request for sessionID: ${sessionID}, language: ${language}`);
    res.json({ message: `Container for sessionID: ${sessionID}, language: ${language} created.` });
});

app.post("/copy", async (req, res) => {
    const { sessionID, directoryContent, language } = req.body;
    console.log(`Copy request for sessionID: ${sessionID}, language: ${language}`);

    // Decode base64 to buffer
    const zipBuffer = Buffer.from(directoryContent, 'base64');
    const zip = await JSZip.loadAsync(zipBuffer);

    // Extract files
    for (const fileName of Object.keys(zip.files)) {
        const file = zip.files[fileName];
        const content = await file.async('string');
        console.log('Extracted file:', fileName);
        console.log('Content:', content);
    }

    res.json({ message: `Code copied for sessionID: ${sessionID}, language: ${language}.` });
});

// Start Express server and attach WebSocket
const server = app.listen(3010, () => {
    console.log('Dummy backend server is running on port 3010');
});

// Initialize WebSocket server
const wss = new WebSocket.Server({ server });

wss.on('connection', (ws, req) => {
    console.log('WebSocket client connected');

    // Extract sessionID and language from query parameters
    const urlParams = new URLSearchParams(req.url.split('?')[1]);
    const language = urlParams.get('language');
    const sessionID = urlParams.get('sessionID');
    console.log(`Client connected with sessionID: ${sessionID}, language: ${language}`);

    // Handle incoming messages
    ws.on('message', (message) => {
        console.log(`Received message: ${message}`);
        const command = message.toString().trim();

        // Simulate a simple response (echo with a prefix)
        const response = `Output from ${language} session ${sessionID}: ${command} executed.`;
        ws.send(response);
    });

    // Handle client disconnection
    ws.on('close', () => {
        console.log('WebSocket client disconnected');
    });

    // Handle errors
    ws.on('error', (error) => {
        console.error('WebSocket error:', error);
    });
});