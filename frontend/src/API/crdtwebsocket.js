import SockJS from 'sockjs-client/dist/sockjs';
import { Client } from '@stomp/stompjs';

export function sendCode(stompClient, code) {
    if (stompClient && stompClient.connected) {
        stompClient.publish({
            destination: '/app/code',
            body: code,
        });
    }
}

export function connectWebSocket(onMessage, onInitialState) {
    const stompClient = new Client({
        webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
        onConnect: () => {
            console.log("Connected\n");
            stompClient.subscribe('/code', (message) => {
                if (message.body && onMessage) {
                    onMessage(message.body);
                    console.log("Received code (WebSocket):\n" + message.body);
                }
            });
            stompClient.subscribe('/code/state', (message) => {
                if (message.body && onInitialState) {
                    onInitialState(message.body);
                    console.log("Received initial state (WebSocket):\n" + message.body);
                }
            });
            // Request the current document state
            stompClient.publish({ destination: '/app/code/state', body: '' });
        },
    });
    stompClient.activate();
    return stompClient;
}

export function disconnectWebSocket(stompClient) {
    if (stompClient) {
        stompClient.deactivate();
    }
}