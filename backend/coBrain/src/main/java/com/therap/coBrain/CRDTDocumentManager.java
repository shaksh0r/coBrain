package com.therap.coBrain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.automerge.Document;
import org.automerge.ObjectId;
import org.automerge.ObjectType;
import org.automerge.Transaction;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class CRDTDocumentManager {
    private final Map<String, Map<String, String>> sessionToFiles = new HashMap<>(); // sessionID -> (fileName -> fileID)
    private final Map<String, Document> fileIDToDocument = new HashMap<>();
    private final Map<String, ObjectId> fileIDToTextId = new HashMap<>();

    public synchronized String createFile(String sessionID, String fileName) throws IllegalArgumentException {
        Map<String, String> fileMap = sessionToFiles.computeIfAbsent(sessionID, k -> new HashMap<>());

        String fileID = fileMap.get(fileName);
        if (fileID != null && fileIDToDocument.containsKey(fileID)) {
            throw new IllegalArgumentException("File already exists in session: " + sessionID + ", file: " + fileName);
        }

        fileID = UUID.randomUUID().toString();
        Document newDoc = new Document();
        ObjectId newTextId;
        try (Transaction tx = newDoc.startTransaction()) {
            newTextId = tx.set(ObjectId.ROOT, "text", ObjectType.TEXT);
            tx.commit();
        }

        fileMap.put(fileName, fileID);
        fileIDToDocument.put(fileID, newDoc);
        fileIDToTextId.put(fileID, newTextId);

        return fileID;
    }

    public synchronized String deleteFile(String sessionID, String fileName) {
        Map<String, String> fileMap = sessionToFiles.get(sessionID);
        if (fileMap == null || !fileMap.containsKey(fileName)) {
            throw new IllegalArgumentException("File not found in session: " + sessionID + ", file: " + fileName);
        }
        String fileID = fileMap.remove(fileName);
        fileIDToDocument.remove(fileID);
        fileIDToTextId.remove(fileID);
        return fileID;
    }

    public synchronized void insertText(String fileID, int index, String value) {
        Document doc = fileIDToDocument.get(fileID);
        ObjectId textId = fileIDToTextId.get(fileID);
        if (doc == null || textId == null) {
            throw new IllegalArgumentException("Invalid fileID: " + fileID);
        }
        try (Transaction tx = doc.startTransaction()) {
            tx.spliceText(textId, index, 0, value);
            tx.commit();
        }
    }

    public synchronized void deleteText(String fileID, int index, int length) {
        Document doc = fileIDToDocument.get(fileID);
        ObjectId textId = fileIDToTextId.get(fileID);
        if (doc == null || textId == null) {
            throw new IllegalArgumentException("Invalid fileID: " + fileID);
        }
        try (Transaction tx = doc.startTransaction()) {
            tx.spliceText(textId, index, length, "");
            tx.commit();
        }
    }

    public synchronized void replaceText(String fileID, int index, int length, String value) {
        Document doc = fileIDToDocument.get(fileID);
        ObjectId textId = fileIDToTextId.get(fileID);
        if (doc == null || textId == null) {
            throw new IllegalArgumentException("Invalid fileID: " + fileID);
        }
        try (Transaction tx = doc.startTransaction()) {
            tx.spliceText(textId, index, length, value);
            tx.commit();
        }
    }

    public synchronized void setDocument(String fileID, String value) {
        Document doc = fileIDToDocument.get(fileID);
        ObjectId textId = fileIDToTextId.get(fileID);
        if (doc == null || textId == null) {
            throw new IllegalArgumentException("Invalid fileID: " + fileID);
        }
        try (Transaction tx = doc.startTransaction()) {
            Optional<String> current = doc.text(textId);
            int len = current.map(String::length).orElse(0);
            if (len > 0) {
                tx.spliceText(textId, 0, len, "");
            }
            tx.spliceText(textId, 0, 0, value);
            tx.commit();
        }
    }

    public synchronized String getDocument(String sessionID, String fileID) {
        Map<String, String> fileMap = sessionToFiles.get(sessionID);
        if (fileMap == null) {
            throw new IllegalArgumentException("No files found for sessionID: " + sessionID);
        }

        String fileName = null;
        for (Map.Entry<String, String> entry : fileMap.entrySet()) {
            if (entry.getValue().equals(fileID)) {
                fileName = entry.getKey();
                break;
            }
        }
        if (fileName == null) {
            throw new IllegalArgumentException("File not found for sessionID: " + sessionID + " and fileID: " + fileID);
        }

        Document doc = fileIDToDocument.get(fileID);
        ObjectId textId = fileIDToTextId.get(fileID);
        if (doc == null || textId == null) {
            throw new IllegalArgumentException("Invalid fileID: " + fileID);
        }
        Optional<String> text = doc.text(textId);
        return text.orElse("");
    }

    public synchronized ObjectNode getDirectoryContent(String sessionID) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode dirContent = objectMapper.createObjectNode();
        Map<String, String> fileMap = sessionToFiles.get(sessionID);
        if (fileMap != null) {
            for (Map.Entry<String, String> entry : fileMap.entrySet()) {
                String fileName = entry.getKey();
                String fileID = entry.getValue();
                String content = getDocument(sessionID, fileID);
                ObjectNode fileNode = objectMapper.createObjectNode();
                fileNode.put("fileName", fileName);
                fileNode.put("fileID", fileID); // Include fileID for frontend use
                fileNode.put("content", content);
                dirContent.set(fileName, fileNode);
            }
        }
        return dirContent;
    }

    public synchronized ObjectNode getAllFiles() {
        ObjectMapper objectMapper = new ObjectMapper();
        java.util.List<ObjectNode> fileList = new java.util.ArrayList<>();

        for (Map.Entry<String, Map<String, String>> sessionEntry : sessionToFiles.entrySet()) {
            String sessionID = sessionEntry.getKey();
            Map<String, String> fileMap = sessionEntry.getValue();
            for (Map.Entry<String, String> fileEntry : fileMap.entrySet()) {
                String fileName = fileEntry.getKey();
                String fileID = fileEntry.getValue();

                ObjectNode fileNode = objectMapper.createObjectNode();
                fileNode.put("sessionID", sessionID);
                fileNode.put("fileName", fileName);
                fileNode.put("fileID", fileID);

                fileList.add(fileNode);
            }
        }

        fileList.sort((a, b) -> {
            int cmp = a.get("sessionID").asText().compareTo(b.get("sessionID").asText());
            if (cmp != 0) return cmp;
            return a.get("fileName").asText().compareTo(b.get("fileName").asText());
        });

        ObjectNode result = objectMapper.createObjectNode();
        result.set("files", objectMapper.valueToTree(fileList));
        return result;
    }

    public synchronized ObjectNode getAllFilesForSession(String sessionID) {
        ObjectMapper objectMapper = new ObjectMapper();
        java.util.List<ObjectNode> fileList = new java.util.ArrayList<>();

        Map<String, String> fileMap = sessionToFiles.get(sessionID);
        if (fileMap != null) {
            for (Map.Entry<String, String> fileEntry : fileMap.entrySet()) {
                String fileName = fileEntry.getKey();
                String fileID = fileEntry.getValue();

                ObjectNode fileNode = objectMapper.createObjectNode();
                fileNode.put("fileName", fileName);
                fileNode.put("fileID", fileID);

                fileList.add(fileNode);
            }
        }

        fileList.sort((a, b) -> a.get("fileName").asText().compareTo(b.get("fileName").asText()));

        ObjectNode result = objectMapper.createObjectNode();
        result.set("files", objectMapper.valueToTree(fileList));
        return result;
    }

    public synchronized String loadFile(String sessionID, String fileName, String content){
        String fileID;
        try {
            fileID = createFile(sessionID, fileName);
            setDocument(fileID, content);
        } catch (Exception e) {
            System.out.println("Error occurred while loading file: " + e.getMessage());
            throw e;
        }
        return fileID;
    }
}