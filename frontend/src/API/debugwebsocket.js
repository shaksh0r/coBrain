// CHANGE TO WHATEVER PORT NUMBER YOU LIKE; THIS IS FOR BACKEND COPY AND GETCONTAINER
const PORT_NUMBER = 3010;


export function sendDebugCmd(socket, input) {
	if (socket && socket.readyState === WebSocket.OPEN) {
		socket.send(input);
	}
}

export function connectDebug(onMessage, language, sessionID) {
	const wsUrl = `ws://localhost:${PORT_NUMBER}/${language}?sessionID=${sessionID}`;
	const socket = new WebSocket(wsUrl);

	socket.onopen = () => {
		console.log("Debugger Connected\n");
	};

	socket.onmessage = (event) => {
		if (onMessage) {
			onMessage(event.data);
			console.log("debug:\n" + event.data);
		}
	};

	socket.onerror = (error) => {
		console.error("debugger error:", error);
	};

	return socket;
}

export function disconnectDebug(socket) {
	if (socket) {
		socket.close();
	}
}
