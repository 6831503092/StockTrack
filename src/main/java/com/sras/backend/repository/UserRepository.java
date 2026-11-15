package com.sras.backend.repository;

import com.sras.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUserIdAndIsActiveTrue(Integer userId);

    Optional<User> findByFullNameIgnoreCaseAndIsActiveTrue(String fullName);

    boolean existsByFullNameIgnoreCase(String fullName);
}
