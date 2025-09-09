// CHANGE TO WHATEVER PORT NUMBER YOU LIKE; THIS IS FOR BACKEND COPY AND GETCONTAINER
const PORT_NUMBER = 3010;


export function sendInput(socket, input) {
	if (socket && socket.readyState === WebSocket.OPEN) {
		socket.send(input);
	}
}

export function connectTerminal(onMessage, language, sessionID) {
	const wsUrl = `ws://localhost:${PORT_NUMBER}/${language}?sessionID=${sessionID}`;
	const socket = new WebSocket(wsUrl);

	socket.onopen = () => {
		console.log("IO WebSocket Connected\n");
	};

	socket.onmessage = (event) => {
		if (onMessage) {
			onMessage(event.data);
			console.log("Received IO message (WebSocket):\n" + event.data);
		}
	};

	socket.onerror = (error) => {
		console.error("WebSocket error:", error);
	};

	return socket;
}

export function disconnectTerminal(socket) {
	if (socket) {
		socket.close();
	}
}
