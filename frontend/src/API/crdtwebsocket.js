import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

export function connectWebSocket(onOperation, onCodeState, onFileResponse, clientIdRef) {
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

        // Subscribe to /code for CRDT operations
        stompClient.subscribe('/code', (message) => {
            try {
                const payload = JSON.parse(message.body);
                const fileID = payload.fileID;
                // console.log('Received:', payload);
                if (typeof onOperation === 'function') {
                    onOperation(fileID, message.body);
                } else {
                    console.error('onOperation is not a function:', onOperation);
                }
            } catch (e) {
                console.error('Error parsing operation message:', e, 'Raw message:', message.body);
            }
        });
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

export function sendCode(stompClient, fileID, message) {
    if (stompClient && stompClient.active) {
        const payload = {
            ...message,
            fileID: fileID
        };
        //console.log("Sending:", payload);
        stompClient.publish({
            destination: '/app/code',
            body: JSON.stringify(payload),
        });
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
        if (!response.ok) throw new Error('Failed to retrieve document contents');
        const data = await response.json();
        return data.content || '';
    } catch (error) {
        console.error('Error retrieving document contents:', error);
    }
}