package com.pashumandi.repository;

import com.pashumandi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.phone = :identifier OR (u.email IS NOT NULL AND u.email = :identifier)")
    Optional<User> findByIdentifier(@Param("identifier") String identifier);

    Optional<User> findByPhoneOrEmail(String phone, String email);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);
}
