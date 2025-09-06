package com.service.auth.Controllers;

import com.service.auth.DTO.LoginResponse;
import com.service.auth.DTO.SignupResponse;
import com.service.auth.Entities.User;
import com.service.auth.DTO.LoginRequest;
import com.service.auth.DTO.SignupRequest;
import com.service.auth.Services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        try {
            User user = userService.signup(request);
            return ResponseEntity.ok(new SignupResponse(user.getUserId(), user.getUsername()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            String jwt = userService.login(request);
            User user = userService.getUserByUsername(request.getUsername());
            return ResponseEntity.ok(new LoginResponse(jwt, user.getUserId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}