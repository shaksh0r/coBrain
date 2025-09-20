// CHANGE TO WHATEVER PORT NUMBER YOU LIKE; THIS IS FOR BACKEND COPY AND GETCONTAINER
const PORT_NUMBER = 8081;


export function sendProblemCmd(socket, input) {
	if (socket && socket.readyState === WebSocket.OPEN) {
		socket.send(input);
	}
}

export function connectProblems(onMessage, language, userId) {

	if (!userId) {
		setTimeout(() => {
			connectProblems(onMessage, language, userId);
		}, 1000);
		return;
	}

	const wsUrl = `ws://localhost:${PORT_NUMBER}/${language}?userId=${userId}`;
	console.log("Connecting to", wsUrl);
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
