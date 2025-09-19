package com.service.auth.DTO;

import lombok.Data;

@Data
public class LoginRequest {
    private String username; // Can be username or email
    private String password;
}
