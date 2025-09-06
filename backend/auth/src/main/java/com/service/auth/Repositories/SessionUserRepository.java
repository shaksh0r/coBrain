package com.service.auth.Repositories;

import com.service.auth.Entities.SessionUser;
import com.service.auth.Entities.SessionUserId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SessionUserRepository extends JpaRepository<SessionUser, SessionUserId> {
    List<SessionUser> findBySessionSessionId(UUID sessionId);
    List<SessionUser> findByUserUserId(UUID userId);
}