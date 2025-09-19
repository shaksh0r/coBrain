package com.service.auth.DTO;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SessionResponse {
    private String sessionId;
    private String sessionName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean active;
    private List<UserDTO> users;
}

// Add this to the existing DTO classes file