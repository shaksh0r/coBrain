package com.service.auth.DTO;

import lombok.Data;

@Data
public class CreateSessionRequest {
    private String sessionName;
    private String description;
    private Integer expirationHours; // Optional, session expiration in hours
}
