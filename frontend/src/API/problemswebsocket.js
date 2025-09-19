// CHANGE TO WHATEVER PORT NUMBER YOU LIKE; THIS IS FOR BACKEND COPY AND GETCONTAINER
const PORT_NUMBER = 8081;

export function connectProblems(onMessage, language, userId) {
	const wsUrl = `ws://localhost:${PORT_NUMBER}/${language}?userId=${userId}`;
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
