package com.sras.backend.repository;

import com.sras.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUserIdAndIsActiveTrue(Integer userId);

    Optional<User> findByUsernameAndIsActiveTrue(String username);

    Optional<User> findByUserIdAndUsernameAndIsActiveTrue(Integer userId, String username);

    boolean existsByUsername(String username);
}

