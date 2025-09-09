package com.service.auth.Services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.auth.DTO.CreateSessionRequest;
import com.service.auth.DTO.SessionResponse;
import com.service.auth.DTO.UserDTO;
import com.service.auth.Entities.Session;
import com.service.auth.Entities.User;
import com.service.auth.Repositories.SessionRepository;
import com.service.auth.Repositories.UserRepository;
import com.service.auth.Utilities.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SessionService {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public SessionResponse createSession(CreateSessionRequest request, String token) {
        String userId = jwtUtil.extractUserId(token);
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    
        Session session = new Session();
        session.setSessionName(request.getSessionName());
        session.setDescription(request.getDescription());
        session.setCreatedAt(LocalDateTime.now());
        if (request.getExpirationHours() != null) {
            session.setExpiresAt(LocalDateTime.now().plusHours(request.getExpirationHours()));
        }
        session.setActive(true);
    
        // Save session WITHOUT adding users first
        Session savedSession = sessionRepository.save(session);
    
        // Manually create the relationship in database only
        // Don't modify the entity objects to avoid circular references
        sessionRepository.addUser(savedSession.getSessionId(), creator.getUserId());
    
        // Build response manually
        List<UserDTO> users = new ArrayList<>();
        users.add(UserDTO.builder()
                .userId(creator.getUserId())
                .username(creator.getUsername())
                .email(creator.getEmail())
                .firstName(creator.getFirstName())
                .lastName(creator.getLastName())
                .build());

        SessionResponse response = SessionResponse.builder()
                .sessionId(savedSession.getSessionId())
                .sessionName(savedSession.getSessionName())
                .description(savedSession.getDescription())
                .createdAt(savedSession.getCreatedAt())
                .expiresAt(savedSession.getExpiresAt())
                .active(savedSession.isActive())
                .users(users)
                .build();

        // Manual JSON conversion to test
        try {
            String json = objectMapper.writeValueAsString(response);
            System.out.println("Manual JSON: " + json);
            return objectMapper.readValue(json, SessionResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("JSON conversion failed", e);
        }
    }

    public void joinSession(String sessionId, String token) {
        String userId = jwtUtil.extractUserId(token);
    
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
    
        if (!session.isActive()) {
            throw new RuntimeException("Session is not active");
        }
    
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(LocalDateTime.now())) {
            session.setActive(false);
            sessionRepository.save(session);
            throw new RuntimeException("Session has expired");
        }
    
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    
        // Check if user is already in session using database query
        // Don't load the relationship into memory
        boolean userAlreadyInSession = sessionRepository.isUserInSession(sessionId, userId);
        if (userAlreadyInSession) {
            throw new RuntimeException("User already in session");
        }
    
        // Use the repository method instead of entity manipulation
        sessionRepository.addUser(sessionId, userId);
    }
    
    public void leaveSession(String sessionId, String token) {
        String userId = jwtUtil.extractUserId(token);
    
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
    
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    
        // Check if user is in session using database query
        boolean userInSession = sessionRepository.isUserInSession(sessionId, userId);
        if (!userInSession) {
            throw new RuntimeException("User not in session");
        }
    
        // Use the repository method instead of entity manipulation
        sessionRepository.removeUser(sessionId, userId);
    }

    public List<SessionResponse> getUserSessions(String token) {
        String userId = jwtUtil.extractUserId(token);
    
        // Use native query to avoid loading entity relationships
        List<Session> sessions = sessionRepository.findSessionsByUserId(userId);
    
        return sessions.stream()
                .map(session -> SessionResponse.builder()
                        .sessionId(session.getSessionId())
                        .sessionName(session.getSessionName())
                        .description(session.getDescription())
                        .createdAt(session.getCreatedAt())
                        .expiresAt(session.getExpiresAt())
                        .active(session.isActive())
                        .users(null) // Don't load users to avoid circular reference
                        .build())
                .collect(Collectors.toList());
    }

    public SessionResponse getSession(String sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
    
        // Get users using native query to avoid loading entity relationships
        List<UserDTO> users = sessionRepository.findUsersBySessionId(sessionId);
    
        return SessionResponse.builder()
                .sessionId(session.getSessionId())
                .sessionName(session.getSessionName())
                .description(session.getDescription())
                .createdAt(session.getCreatedAt())
                .expiresAt(session.getExpiresAt())
                .active(session.isActive())
                .users(users)
                .build();
    }

    public List<UserDTO> getSessionUsers(String sessionId) {
        // Don't load the session entity with relationships
        if (!sessionRepository.existsById(sessionId)) {
            throw new RuntimeException("Session not found");
        }
    
        // Use native query to get users without loading entity relationships
        return sessionRepository.findUsersBySessionId(sessionId);
    }
}