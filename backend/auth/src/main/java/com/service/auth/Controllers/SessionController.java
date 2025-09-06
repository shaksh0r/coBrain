package com.service.auth.Controllers;

import com.service.auth.DTO.CreateSessionRequest;
import com.service.auth.DTO.SessionResponse;
import com.service.auth.DTO.UserResponse;
import com.service.auth.Entities.Session;
import com.service.auth.Services.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(@RequestBody CreateSessionRequest request, @AuthenticationPrincipal UUID userId) {
        try {
            Session session = sessionService.createSession(userId, request.getName());
            System.out.println("Name: " + request.getName());
            System.out.println("Session ID: " + userId);
            return ResponseEntity.ok(new SessionResponse(session.getSessionId(), session.getName()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/sessions/{sessionId}/join")
    public ResponseEntity<?> joinSession(@PathVariable UUID sessionId, @AuthenticationPrincipal UUID userId) {
        try {
            sessionService.joinSession(sessionId, userId);
            return ResponseEntity.ok("Joined session successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/users/{userId}/sessions")
    public ResponseEntity<?> getUserSessions(@PathVariable UUID userId, @AuthenticationPrincipal UUID authenticatedUserId) {
        if (!userId.equals(authenticatedUserId)) {
            return ResponseEntity.status(403).body("Unauthorized");
        }
        List<SessionResponse> sessions = sessionService.getUserSessions(userId).stream()
                .map(s -> new SessionResponse(s.getSessionId(), s.getName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/sessions/{sessionId}/users")
    public ResponseEntity<?> getSessionUsers(@PathVariable UUID sessionId, @AuthenticationPrincipal UUID userId) {
        try {
            // Verify user is in session
            sessionService.getSessionUsers(sessionId).stream()
                    .filter(u -> u.getUserId().equals(userId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("User not in session"));
            List<UserResponse> users = sessionService.getSessionUsers(sessionId).stream()
                    .map(u -> new UserResponse(u.getUserId(), u.getUsername()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(users);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}