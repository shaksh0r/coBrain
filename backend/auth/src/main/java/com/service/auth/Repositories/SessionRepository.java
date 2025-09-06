package com.service.auth.Repositories;

import com.service.auth.Entities.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    List<Session> findByOwnerUserId(UUID ownerUserId);
    List<Session> findByStatus(Session.SessionStatus status);
}