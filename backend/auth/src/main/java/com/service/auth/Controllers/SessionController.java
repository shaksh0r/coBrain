package com.service.auth.Controllers;

import com.service.auth.DTO.CreateSessionRequest;
import com.service.auth.DTO.ErrorResponse;
import com.service.auth.DTO.MessageResponse;
import com.service.auth.DTO.SessionResponse;
import com.service.auth.Entities.Session;
import com.service.auth.Services.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/create")
    public ResponseEntity<?> createSession(@RequestBody CreateSessionRequest request,
                                           @RequestHeader("Authorization") String token) {
        try {
            SessionResponse session = sessionService.createSession(request, token.replace("Bearer ", ""));
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/{sessionId}/join")
    public ResponseEntity<?> joinSession(@PathVariable String sessionId,
                                         @RequestHeader("Authorization") String token) {
        try {
            sessionService.joinSession(sessionId, token.replace("Bearer ", ""));
            return ResponseEntity.ok(new MessageResponse("Successfully joined session"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/{sessionId}/leave")
    public ResponseEntity<?> leaveSession(@PathVariable String sessionId,
                                          @RequestHeader("Authorization") String token) {
        try {
            sessionService.leaveSession(sessionId, token.replace("Bearer ", ""));
            return ResponseEntity.ok(new MessageResponse("Successfully left session"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserSessions(@RequestHeader("Authorization") String token) {
        try {
            List<SessionResponse> sessions = sessionService.getUserSessions(token.replace("Bearer ", ""));
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable String sessionId) {
        try {
            SessionResponse session = sessionService.getSession(sessionId);
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{sessionId}/users")
    public ResponseEntity<?> getSessionUsers(@PathVariable String sessionId) {
        try {
            return ResponseEntity.ok(sessionService.getSessionUsers(sessionId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
}