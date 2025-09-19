package com.test.database.demo.Repository;

import com.test.database.demo.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository for the User entity.
 * By extending JpaRepository, we get a bunch of CRUD (Create, Read, Update, Delete)
 * methods for free, like save(), findById(), findAll(), deleteById().
 * Spring automatically implements this interface at runtime.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // You can add custom query methods here if needed, e.g., findByName(String name)
}