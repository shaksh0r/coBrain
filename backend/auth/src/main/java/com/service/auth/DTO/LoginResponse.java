package com.service.auth.DTO;

import java.util.UUID;

public class LoginResponse {
    private String jwt;
    private UUID userId;

    public LoginResponse(String jwt, UUID userId) {
        this.jwt = jwt;
        this.userId = userId;
    }

    // Getters and Setters
    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}
