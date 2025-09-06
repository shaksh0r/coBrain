package com.service.auth.DTO;

import java.util.UUID;

public class SessionResponse {
    private UUID sessionId;
    private String name;

    public SessionResponse(UUID sessionId, String name) {
        this.sessionId = sessionId;
        this.name = name;
    }

    // Getters and Setters
    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
