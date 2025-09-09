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
    private final Map<String, String> sessionToFileName = new HashMap<>();
    private final Map<String, String> fileNameToFileID = new HashMap<>();
    private final Map<String, Document> fileIDToDocument = new HashMap<>();
    private final Map<String, ObjectId> fileIDToTextId = new HashMap<>();

    public synchronized String getOrCreateFile(String sessionID, String fileName) {
        String key = sessionID + ":" + fileName;
        String fileID = sessionToFileName.get(key);

        if (fileID != null && fileIDToDocument.containsKey(fileID)) {
            return fileID;
        }

        fileID = UUID.randomUUID().toString();
        Document newDoc = new Document();
        ObjectId newTextId;
        try (Transaction tx = newDoc.startTransaction()) {
            newTextId = tx.set(ObjectId.ROOT, "text", ObjectType.TEXT);
            tx.commit();
        }

        sessionToFileName.put(key, fileID);
        fileNameToFileID.put(fileName, fileID);
        fileIDToDocument.put(fileID, newDoc);
        fileIDToTextId.put(fileID, newTextId);

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

    public synchronized String getDocument(String fileID) {
        Document doc = fileIDToDocument.get(fileID);
        ObjectId textId = fileIDToTextId.get(fileID);
        if (doc == null || textId == null) {
            throw new IllegalArgumentException("Invalid fileID: " + fileID);
        }
        Optional<String> text = doc.text(textId);
        return text.orElse("");
    }

    public synchronized ObjectNode getDirectoryContent(String sessionID){
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode dirContent = objectMapper.createObjectNode();
        for (Map.Entry<String, String> entry : sessionToFileName.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(sessionID + ":")) {
                String fileName = key.substring(sessionID.length() + 1);
                String fileID = entry.getValue();
                String content = getDocument(fileID);
                ObjectNode fileNode = objectMapper.createObjectNode();
                fileNode.put("fileName", fileName);
                fileNode.put("content", content);
                dirContent.set(fileName, fileNode);
            }
        }
        return dirContent;
    }
}