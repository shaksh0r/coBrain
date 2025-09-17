package com.therap.coBrain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:3002", "http://localhost:3003",
                        "http://localhost:3004", "http://localhost:3005", "http://localhost:3006",
                        "http://localhost:3007", "http://localhost:3008", "http://localhost:3009"})
public class CodeController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private CRDTDocumentManager crdtDocumentManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Existing WebSocket handlers remain unchanged
    @MessageMapping("/code")
    @SendTo("/code")
    public String handleCode(@Payload String message) throws Exception {
        JsonNode node = objectMapper.readTree(message);
        String fileID = node.get("fileID").asText();
        String op = node.get("op").asText();
        int index = node.get("index").asInt();
        String value = node.has("value") ? node.get("value").asText() : null;

        if ("insert".equals(op) && value != null) {
            crdtDocumentManager.insertText(fileID, index, value);
        } else if ("delete".equals(op)) {
            int length = node.has("length") ? node.get("length").asInt() : 1;
            crdtDocumentManager.deleteText(fileID, index, length);
        } else if ("replace".equals(op) && value != null) {
            int length = node.has("length") ? node.get("length").asInt() : 1;
            crdtDocumentManager.replaceText(fileID, index, length, value);
        }

        System.out.println("Broadcasting message: " + message);
        return message;
    }

    @PostMapping("/api/state")
    public String getCurrentState(@RequestBody ObjectNode request) throws Exception {
        try {
            String fileID = request.get("fileID").asText();
            String sessionID = request.get("sessionID").asText();
            String content = crdtDocumentManager.getDocument(sessionID, fileID);
            ObjectNode response = objectMapper.createObjectNode();
            response.put("fileID", fileID);
            response.put("content", content);
            return objectMapper.writeValueAsString(response);
        } catch (IllegalArgumentException e) {
            ObjectNode errorResponse = objectMapper.createObjectNode();
            errorResponse.put("error", e.getMessage());
            return objectMapper.writeValueAsString(errorResponse);
        }
    }

    @PostMapping("/api/files")
    public ObjectNode createFile(@RequestBody ObjectNode request) throws Exception {
        String userID = request.get("userID").asText();
        String sessionID = request.get("sessionID").asText();
        String fileName = request.get("fileName").asText();

        FileResult fileResult = crdtDocumentManager.getOrCreateFile(sessionID, fileName);
        ObjectNode response = objectMapper.createObjectNode();
        response.put("fileID", fileResult.fileID);
        response.put("fileName", fileName);
        System.out.println("REST API response: fileID=" + fileResult.fileID + ", fileName=" + fileName);

        if (fileResult.isNewFile) {
            ObjectNode updateMsg = objectMapper.createObjectNode();
            updateMsg.put("action", "add");
            updateMsg.put("fileName", fileName);
            updateMsg.put("fileID", fileResult.fileID);
            messagingTemplate.convertAndSend("/topic/session/" + sessionID + "/files", updateMsg);
        }

        return response;
    }

    @PostMapping("/api/deleteFile")
    public ObjectNode deleteFile(@RequestBody ObjectNode request) throws Exception {
        String sessionID = request.get("sessionID").asText();
        String fileName = request.get("fileName").asText();

        String fileID = crdtDocumentManager.deleteFile(sessionID, fileName);

        ObjectNode updateMsg = objectMapper.createObjectNode();
        updateMsg.put("action", "delete");
        updateMsg.put("fileName", fileName);
        updateMsg.put("fileID", fileID);
        messagingTemplate.convertAndSend("/topic/session/" + sessionID + "/files", updateMsg);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", "success");
        return response;
    }

    @PostMapping("/api/directory")
    public ObjectNode getDirectoryContent(@RequestBody ObjectNode request) throws Exception {
        String sessionID = request.get("sessionID").asText();
        System.out.println("Received directory content request for sessionID: " + sessionID);
        return crdtDocumentManager.getDirectoryContent(sessionID);
    }

    @GetMapping("/api/getAllFiles")
    public ObjectNode getAllFiles() {
        return crdtDocumentManager.getAllFiles();
    }

    @GetMapping("/api/getFiles/{sessionID}")
    public ObjectNode getAllFilesForSession(@PathVariable String sessionID) {
        return crdtDocumentManager.getAllFilesForSession(sessionID);
    }
}