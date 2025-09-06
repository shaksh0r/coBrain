package com.service.auth.Entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "session_users")
public class SessionUser {
    @EmbeddedId
    private SessionUserId id;

    @ManyToOne
    @MapsId("sessionId")
    @JoinColumn(name = "session_id")
    private Session session;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Column(name = "role", length = 20)
    @Enumerated(EnumType.STRING)
    private Role role;

    public enum Role {
        OWNER, PARTICIPANT, VIEWER
    }

    public SessionUser() {
        this.id = new SessionUserId();
    }

    // Getters and Setters
    public SessionUserId getId() {
        return id;
    }

    public void setId(SessionUserId id) {
        this.id = id;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
        this.id.setSessionId(session.getSessionId());
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        this.id.setUserId(user.getUserId());
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}