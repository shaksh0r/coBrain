package com.service.auth.DTO;

import lombok.Data;

@Data
public class SignupRequest {
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
}

