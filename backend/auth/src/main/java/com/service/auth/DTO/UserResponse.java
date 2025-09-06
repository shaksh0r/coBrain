package com.service.auth.DTO;

import java.util.UUID;

public class UserResponse {
    private UUID userId;
    private String username;

    public UserResponse(UUID userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    // Getters and Setters
    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
