package com.therap.coBrain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.messaging.handler.annotation.Payload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class CodeController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private CRDTDocumentManager crdtDocumentManager;

    private final ObjectMapper objectMapper = new ObjectMapper();


    // WebSocket handler for CRDT operations
    @MessageMapping("/code")
    @SendTo("/code")
    public String handleCode(@Payload String message) throws Exception {
        // Expecting JSON: { "op": "insert"|"delete", "index": int, "value": string }
        JsonNode node = objectMapper.readTree(message);
        String op = node.get("op").asText();
        int index = node.get("index").asInt();
        String value = node.has("value") ? node.get("value").asText() : null;

        if ("insert".equals(op) && value != null) {
            // Replace the entire document content
            crdtDocumentManager.insertText(index, value);
        } else if ("delete".equals(op)) {
            int length = node.has("length") ? node.get("length").asInt() : 1;
            crdtDocumentManager.deleteText(index, length);
        }

        // Broadcast the operation to all clients
        return crdtDocumentManager.getDocument();
    }

    // Endpoint to get the current document state (for new clients)
    @MessageMapping("/code/state")
    @SendTo("/code/state")
    public String getCurrentState() {
        return crdtDocumentManager.getDocument();
    }
}
