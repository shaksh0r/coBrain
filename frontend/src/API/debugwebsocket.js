// CHANGE TO WHATEVER PORT NUMBER YOU LIKE; THIS IS FOR BACKEND COPY AND GETCONTAINER
const PORT_NUMBER = 8081;


export function sendDebugCmd(socket, input) {
	if (socket && socket.readyState === WebSocket.OPEN) {
		socket.send(input);
	}
}

export function connectDebug(onMessage, language, userId) {
	const wsUrl = `ws://localhost:${PORT_NUMBER}/${language}?userId=${userId}`;
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
