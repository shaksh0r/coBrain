import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

export function connectWebSocket(onOperation, onCodeState, onFileResponse, clientIdRef, sessionID) {
    if (!clientIdRef || typeof clientIdRef.current === 'undefined') {
        console.error('clientIdRef is undefined or invalid, generating temporary ID');
        clientIdRef = { current: `temp_${Date.now()}_${Math.random().toString(36).substr(2, 9)}` };
    }

    const socket = new SockJS('http://localhost:8080/ws');
    const stompClient = new Client({
        webSocketFactory: () => socket,
        //debug: (str) => console.log('STOMP Debug:', str),
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
    });

    stompClient.onConnect = (frame) => {
        console.log('Connected: ' + frame);

        stompClient.subscribe('/code', (message) => {
            try {
                const payload = JSON.parse(message.body);
                const fileID = payload.fileID;
                const sessionID = payload.sessionID;
                // console.log('Received:', payload);
                if (typeof onOperation === 'function') {
                    onOperation(fileID, sessionID, message.body);
                } else {
                    console.error('onOperation is not a function:', onOperation);
                }
            } catch (e) {
                console.error('Error parsing operation message:', e, 'Raw message:', message.body);
            }
        });

        if (sessionID && typeof onFileResponse === 'function') {
            stompClient.subscribe(`/topic/session/${sessionID}/files`, (message) => {
                try {
                    const payload = JSON.parse(message.body);
                    onFileResponse(payload);
                } catch (e) {
                    console.error('Error parsing file event message:', e, 'Raw message:', message.body);
                }
            });
        }
    };

    stompClient.onStompError = (frame) => {
        console.error('Broker reported error: ' + frame.headers['message']);
        console.error('Additional details: ' + frame.body);
    };

    stompClient.onWebSocketError = (error) => {
        console.error('WebSocket error:', error);
    };

    stompClient.configure({
        connectHeaders: {
            login: clientIdRef.current,
            passcode: 'password',
        },
    });

    stompClient.activate();
    console.log('STOMP client activated');
    return stompClient;
}

export function disconnectWebSocket(stompClient) {
    if (stompClient) {
        stompClient.deactivate();
        console.log('WebSocket disconnected');
    }
}

export function sendCode(stompClient, fileID, sessionID, message) {
    if (stompClient && stompClient.active) {
        const payload = {
            ...message,
            fileID: fileID,
            sessionID: sessionID
        };
        //console.log("Sending:", payload);
        stompClient.publish({
            destination: '/app/code',
            body: JSON.stringify(payload),
        });
    }
}

export async function getAllFiles() {
    try {
        const response = await fetch('http://localhost:8080/api/getAllFiles', {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' },
        });
        const data = await response.json();
        return data.files || [];
    } catch (error) {
        console.error('Error retrieving all files:', error);
    }
}

export async function getFilesForSession(sessionID) {
    try {
        const response = await fetch(`http://localhost:8080/api/getFiles/${sessionID}`, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' },
        });
        const data = await response.json();
        return data.files || [];
    } catch (error) {
        console.error('Error retrieving files for session:', error);
    }
}

export async function requestDocumentState(sessionID, fileID, clientIdRef) {
    try {
        const response = await fetch('http://localhost:8080/api/state', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                userID: clientIdRef.current,
                sessionID: sessionID,
                fileID: fileID,
            }),
        });
        const data = await response.json();
        return data.content || '';
    } catch (error) {
        console.error('Error retrieving document contents:', error);
    }
}

export async function createFile(sessionID, fileName, clientIdRef){
    try {
        const response = await fetch('http://localhost:8080/api/createFile', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                userID: clientIdRef.current,
                sessionID,
                fileName,
            }),
        });
        const data = await response.json();
        return data.fileID;
    } catch (error) {
        console.error('Error creating file:', error);
    }
}

export async function loadFile(sessionID, fileName, clientIdRef, content){
    try {
        const response = await fetch(`http://localhost:8080/api/loadFile`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                userID: clientIdRef.current,
                sessionID,
                fileName,
                content
            }),
        });
        const data = await response.json();
        return data.fileID || '';
    } catch (error) {
        console.error('Error loading file:', error);
    }
}

export async function deleteFile(sessionID, fileNames) {
    try {
        const response = await fetch(`http://localhost:8080/api/deleteFile`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                sessionID,
                fileNames
            }),
        });
        const data = await response.json();
        return data.success || false;
    } catch (error) {
        console.error('Error deleting file:', error);
    }
}