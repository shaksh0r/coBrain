package com.service.auth.Services;

import com.service.auth.Entities.Session;
import com.service.auth.Entities.SessionUser;
import com.service.auth.Entities.SessionUserId;
import com.service.auth.Entities.User;
import com.service.auth.Repositories.SessionRepository;
import com.service.auth.Repositories.SessionUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final SessionUserRepository sessionUserRepository;
    private final UserService userService;
    private final RestTemplate restTemplate;

    public SessionService(SessionRepository sessionRepository, SessionUserRepository sessionUserRepository, UserService userService) {
        this.sessionRepository = sessionRepository;
        this.sessionUserRepository = sessionUserRepository;
        this.userService = userService;
        this.restTemplate = new RestTemplate();
    }

    @Transactional
    public Session createSession(UUID userId, String name) {
        User user = userService.getUserById(userId);

        Session session = new Session();
        session.setName(name);
        session.setOwner(user);
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusHours(24));
        session.setStatus(Session.SessionStatus.ACTIVE);
        session = sessionRepository.save(session);

        SessionUser sessionUser = new SessionUser();
        sessionUser.setSession(session);
        sessionUser.setUser(user);
        sessionUser.setRole(SessionUser.Role.OWNER);
        sessionUserRepository.save(sessionUser);

        // Notify main service
        notifyMainService(session.getSessionId(), userId, "owner");
        return session;
    }

    @Transactional
    public void joinSession(UUID sessionId, UUID userId) {
        Session session = sessionRepository.findById(sessionId)
                .filter(s -> s.getStatus() == Session.SessionStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Session not found or inactive"));
        User user = userService.getUserById(userId);

        SessionUserId sessionUserId = new SessionUserId();
        sessionUserId.setUserId(userId);
        sessionUserId.setSessionId(sessionId);

        if (sessionUserRepository.findById(sessionUserId).isPresent()) {
            throw new IllegalArgumentException("User already in session");
        }

        SessionUser sessionUser = new SessionUser();
        sessionUser.setSession(session);
        sessionUser.setUser(user);
        sessionUser.setRole(SessionUser.Role.PARTICIPANT);
        sessionUserRepository.save(sessionUser);

        // Notify main service
        notifyMainService(sessionId, userId, "participant");
    }

    public List<Session> getUserSessions(UUID userId) {
        return sessionUserRepository.findByUserUserId(userId).stream()
                .map(SessionUser::getSession)
                .filter(s -> s.getStatus() == Session.SessionStatus.ACTIVE)
                .toList();
    }

    public List<User> getSessionUsers(UUID sessionId) {
        return sessionUserRepository.findBySessionSessionId(sessionId).stream()
                .map(SessionUser::getUser)
                .toList();
    }

    private void notifyMainService(UUID sessionId, UUID userId, String role) {
        // Send HTTP POST to main service (localhost:8080)
        String url = "http://localhost:8080/api/sessions/" + sessionId + "/sync";
        restTemplate.postForObject(url, new SyncRequest(sessionId, userId, role), Void.class);
    }

    // DTO for notifying main service
    private static class SyncRequest {
        public UUID sessionId;
        public UUID userId;
        public String role;

        public SyncRequest(UUID sessionId, UUID userId, String role) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.role = role;
        }
    }
}