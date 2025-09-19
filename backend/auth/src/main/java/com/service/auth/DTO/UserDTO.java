package com.service.auth.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDTO {
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
}
