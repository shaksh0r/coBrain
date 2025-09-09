package com.service.auth.Repositories;

import com.service.auth.DTO.UserDTO;
import com.service.auth.Entities.Session;
import com.service.auth.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, String> {
    List<Session> findByUsersContaining(User user);

    List<Session> findByActiveTrue();
    @Modifying
    @Query(value = "INSERT INTO user_sessions (session_id, user_id) VALUES (:sessionId, :userId)", nativeQuery = true)
    void addUser(@Param("sessionId") String sessionId, @Param("userId") String userId);
    
    // Also add remove method for completeness
    @Modifying
    @Query(value = "DELETE FROM user_sessions WHERE session_id = :sessionId AND user_id = :userId", nativeQuery = true)
    void removeUser(@Param("sessionId") String sessionId, @Param("userId") String userId);
    
    // Add this method to check if user is in session
    @Query(value = "SELECT COUNT(*) > 0 FROM user_sessions WHERE session_id = :sessionId AND user_id = :userId", nativeQuery = true)
    boolean isUserInSession(@Param("sessionId") String sessionId, @Param("userId") String userId);

     // Add these new methods to avoid entity relationship loading
    @Query(value = "SELECT s.* FROM sessions s INNER JOIN user_sessions us ON s.session_id = us.session_id WHERE us.user_id = :userId", nativeQuery = true)
    List<Session> findSessionsByUserId(@Param("userId") String userId);
    
    @Query(value = """
        SELECT u.user_id as userId, u.username, u.email, u.first_name as firstName, u.last_name as lastName 
        FROM users u 
        INNER JOIN user_sessions us ON u.user_id = us.user_id 
        WHERE us.session_id = :sessionId
        """, nativeQuery = true)
    List<UserDTO> findUsersBySessionId(@Param("sessionId") String sessionId);
}
