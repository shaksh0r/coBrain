const JSZip = require('jszip');
const express = require('express');
const cors = require('cors');
const WebSocket = require('ws');
const app = express();

const PORT_NUMBER = 8082;
let global_session_id = 3;

app.use(cors());
app.use(express.json());

// REST API endpoints

// AUTH

app.post("/api/auth/signup", (req, res) => {
    const { username, firstname, lastname, email, password } = req.body;
    console.log(`Signup request for username: ${username}, email: ${email}`);

    if (!username || !firstname || !lastname || !email || !password) {
        return res.status(400).json({ error: 'All fields are required' });
    }

    res.status(201).json({
        token: "dummy-token",
        userId: "2105079",
        username,
        email,
        message: "User registered successfully"
    });
});

app.post("/api/auth/login", (req, res) => {
    const { username, password } = req.body;
    console.log(`Login request for username: ${username}`);

    const userId = username === 'Gawwy' ? '2105094' : '2105079';

    res.status(200).json({
        token: "dummy-token",
        userId,
        username,
        email: "dummy@example.com",
        message: "Login successful"
    });
});

app.get("/api/auth/validate", (req, res) => {
    return res.json({ valid: true });
});

// SESSIONS

app.post("/api/sessions/create", (req, res) => {
    const { sessionName, description, expirationHours } = req.body;
    console.log(`Creating session: ${sessionName}`);

    res.status(201).json({
        sessionId: (global_session_id++).toString(),
        sessionName,
        description,
        createdAt: new Date().toISOString(),
        expiresAt: new Date(Date.now() + expirationHours * 3600000).toISOString(),
        active: true,
        message: `Session created successfully ${sessionName}:${global_session_id - 1}`,
        users: [
            {
                userId: "2105094",
                username: "Gawwy",
                email: "gawwy@example.com"
            }
        ]
    });
});

app.post("/api/sessions/:sessionId/join", (req, res) => {
    const sessionId = req.params.sessionId;
    console.log(`User joining session: ${sessionId}`);

    res.status(201).json({
        message: `Successfully joined session ${sessionId}`
    });
});

app.post("/api/sessions/:sessionId/leave", (req, res) => {
    const sessionId = req.params.sessionId;
    console.log(`User leaving session: ${sessionId}`);

    res.status(201).json({
        message: "Successfully left session"
    });
});

app.post("/api/sessions/user/:userId", (req, res) => {
    res.status(200).json({
        sessions: [
            {
                sessionId: "1",
                sessionName: "Demo Session 1",
                description: "This is a demo session",
                createdAt: new Date().toISOString(),
                expiresAt: new Date(Date.now() + 2 * 3600000).toISOString(),
                active: true
            },
            {
                sessionId: "2",
                sessionName: "Demo Session 2",
                description: "This is a demo session",
                createdAt: new Date().toISOString(),
                expiresAt: new Date(Date.now() + 2 * 3600000).toISOString(),
                active: true
            }
        ]
    });
});

app.get("/api/sessions/:sessionId", (req, res) => {
    const sessionId = req.params.sessionId;
    console.log(`Fetching details for session: ${sessionId}`);
    res.status(200).json({
        sessionId,
        sessionName: `Demo Session ${sessionId}`,
        description: "This is a demo session",
        createdAt: new Date().toISOString(),
        expiresAt: new Date(Date.now() + 2 * 3600000).toISOString(),
        active: true
    });
});

app.get("/api/sessions/:sessionId/users", (req, res) => {
    const sessionId = req.params.sessionId;
    console.log(`Fetching users for session: ${sessionId}`);
    res.status(200).json({
        users: [
            {
                userId: "2105094",
                username: "Gawwy",
                email: "gawwy@example.com"
            },
            {
                userId: "2105079",
                username: "Therap",
                email: "therap@example.com"
            }
        ]
    });
});

// CONTAINER
app.get("/getContainer/:sessionID/:language", (req, res) => {
    const { sessionID, language } = req.params;
    console.log(`Received request for sessionID: ${sessionID}, language: ${language}`);
    res.json({ message: `Container for sessionID: ${sessionID}, language: ${language} created.` });
});

app.post("/copy", async (req, res) => {
    const { sessionID, directoryContent, language } = req.body;
    console.log(`Copy request for sessionID: ${sessionID}, language: ${language}`);

    const zipBuffer = Buffer.from(directoryContent, 'base64');
    const zip = await JSZip.loadAsync(zipBuffer);

    for (const fileName of Object.keys(zip.files)) {
        const file = zip.files[fileName];
        const content = await file.async('string');
        console.log('Extracted file:', fileName);
        console.log('Content:', content);
    }

    res.json({ message: `Code copied for sessionID: ${sessionID}, language: ${language}.` });
});

const server = app.listen(PORT_NUMBER, () => {
    console.log(`Dummy backend server is running on port ${PORT_NUMBER}`);
});

const wss = new WebSocket.Server({ server });

wss.on('connection', (ws, req) => {
    console.log('WebSocket client connected');

    const urlParams = new URLSearchParams(req.url.split('?')[1]);
    const language = urlParams.get('language');
    const sessionID = urlParams.get('sessionID');
    console.log(`Client connected with sessionID: ${sessionID}, language: ${language}`);

    ws.on('message', (message) => {
        console.log(`Received message: ${message}`);
        const command = message.toString().trim();

        const response = `Output from ${language} session ${sessionID}: ${command} executed.`;
        ws.send(response);
    });

    ws.on('close', () => {
        console.log('WebSocket client disconnected');
    });

    ws.on('error', (error) => {
        console.error('WebSocket error:', error);
    });
});