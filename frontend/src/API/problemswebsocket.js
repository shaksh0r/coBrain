// CHANGE TO WHATEVER PORT NUMBER YOU LIKE; THIS IS FOR BACKEND COPY AND GETCONTAINER
const PORT_NUMBER = 3010;

export function connectProblems(onMessage, language, sessionID) {
	const wsUrl = `ws://localhost:${PORT_NUMBER}/${language}?sessionID=${sessionID}`;
	const socket = new WebSocket(wsUrl);

	socket.onopen = () => {
		console.log("Problems panel Connected\n");
	};

	socket.onmessage = (event) => {
		if (onMessage) {
			onMessage(event.data);
			console.log("problems:\n" + event.data);
		}
	};

	socket.onerror = (error) => {
		console.error("problems error:", error);
	};

	return socket;
}

export function disconnectProblems(socket) {
	if (socket) {
		socket.close();
	}
}
