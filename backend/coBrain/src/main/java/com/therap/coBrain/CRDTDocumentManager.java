package com.therap.coBrain;

import org.automerge.Document;
import org.automerge.ObjectId;
import org.automerge.ObjectType;
import org.automerge.Transaction;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class CRDTDocumentManager {
    private final Document doc;
    private final ObjectId textId;

    public CRDTDocumentManager() {
        this.doc = new Document();
        ObjectId tempTextId;
        try (Transaction tx = doc.startTransaction()) {
            tempTextId = tx.set(ObjectId.ROOT, "text", ObjectType.TEXT);
            tx.commit();
        }
        this.textId = tempTextId;
    }


    public synchronized void insertText(int index, String value) {
        try (Transaction tx = doc.startTransaction()) {
            tx.spliceText(this.textId, index, 0, value);
            tx.commit();
        }
    }

    // Replace the entire document content
    public synchronized void setDocument(String value) {
        try (Transaction tx = doc.startTransaction()) {
            Optional<String> current = doc.text(this.textId);
            int len = current.map(String::length).orElse(0);
            if (len > 0) {
                tx.spliceText(this.textId, 0, len, "");
            }
            tx.spliceText(this.textId, 0, 0, value);
            tx.commit();
        }
    }

    public synchronized void deleteText(int index, int length) {
        try (Transaction tx = doc.startTransaction()) {
            tx.spliceText(this.textId, index, length, "");
            tx.commit();
        }
    }

    public synchronized String getDocument() {
        Optional<String> text = doc.text(this.textId);
        return text.orElse("");
    }
}
