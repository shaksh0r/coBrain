package com.service.auth.Entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
public class SessionUserId implements Serializable {

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "user_id")
    private UUID userId;

    // Getters and Setters
    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionUserId that = (SessionUserId) o;
        return sessionId.equals(that.sessionId) && userId.equals(that.userId);
    }

    @Override
    public int hashCode() {
        return sessionId.hashCode() + userId.hashCode();
    }
}